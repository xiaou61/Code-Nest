#!/usr/bin/env python3
"""Run a bounded synthetic Alertmanager -> SRE ingestion acceptance drill."""

from __future__ import annotations

import argparse
import ipaddress
import json
import os
import re
import stat
import sys
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any, Callable
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen


ALERT_NAME = "CodeNestSreE2E"
REQUIRED_EVIDENCE_TYPES = {
    "ALERT_SNAPSHOT",
    "DEPLOYMENT_SNAPSHOT",
    "RUNBOOK_SNAPSHOT",
}
DRILL_ID_PATTERN = re.compile(r"[A-Za-z0-9_-]{4,40}")


class DrillError(RuntimeError):
    """Controlled acceptance-drill failure."""


def iso8601(value: datetime) -> str:
    return value.astimezone(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def validate_base_url(value: str, label: str) -> str:
    parsed = urlparse(value)
    try:
        parsed.port
    except ValueError as exception:
        raise DrillError(f"invalid {label} URL") from exception
    if (
        parsed.scheme not in {"http", "https"}
        or not parsed.hostname
        or parsed.username
        or parsed.password
        or parsed.path not in {"", "/"}
        or parsed.params
        or parsed.query
        or parsed.fragment
    ):
        raise DrillError(f"invalid {label} URL")
    try:
        is_loopback = ipaddress.ip_address(parsed.hostname).is_loopback
    except ValueError:
        is_loopback = parsed.hostname.lower() == "localhost"
    if not is_loopback:
        raise DrillError(f"{label} URL must use a loopback host")
    return value.rstrip("/")


def read_token(path_value: str, label: str) -> str:
    path = Path(path_value).resolve()
    if not path.is_file():
        raise DrillError(f"{label} file is missing: {path}")
    if os.name != "nt" and stat.S_IMODE(path.stat().st_mode) & 0o077:
        raise DrillError(f"{label} file must not be accessible by group or other users: {path}")
    token = path.read_text(encoding="utf-8").strip()
    if not token or len(token) > 4096 or any(character.isspace() for character in token):
        raise DrillError(f"{label} file is empty or invalid: {path}")
    return token


def request_json(
    method: str,
    url: str,
    *,
    payload: object | None = None,
    bearer_token: str | None = None,
    timeout: float = 20.0,
) -> Any:
    body = None if payload is None else json.dumps(payload, separators=(",", ":")).encode()
    headers = {"Accept": "application/json", "User-Agent": "code-nest-sre-e2e/2.5.1"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if bearer_token:
        headers["Authorization"] = f"Bearer {bearer_token}"
    request = Request(url, data=body, headers=headers, method=method)
    try:
        with urlopen(request, timeout=timeout) as response:  # noqa: S310 - URLs are validated and operator-scoped.
            response_body = response.read(1_000_001)
    except HTTPError as exception:
        error_body = exception.read(2048).decode("utf-8", errors="replace")
        raise DrillError(f"HTTP {exception.code} from {url}: {error_body}") from exception
    except (URLError, TimeoutError, OSError) as exception:
        raise DrillError(f"request failed for {url}: {exception.__class__.__name__}") from exception
    if len(response_body) > 1_000_000:
        raise DrillError(f"response too large from {url}")
    if not response_body.strip():
        return None
    try:
        return json.loads(response_body)
    except json.JSONDecodeError as exception:
        raise DrillError(f"invalid JSON response from {url}") from exception


def wait_for(
    description: str,
    timeout_seconds: float,
    interval_seconds: float,
    operation: Callable[[], Any | None],
) -> Any:
    deadline = time.monotonic() + timeout_seconds
    last_error: DrillError | None = None
    while time.monotonic() <= deadline:
        try:
            result = operation()
            if result is not None:
                return result
        except DrillError as exception:
            last_error = exception
        time.sleep(interval_seconds)
    suffix = f": {last_error}" if last_error else ""
    raise DrillError(f"timed out waiting for {description}{suffix}")


def build_alert(drill_id: str, *, resolved: bool = False, starts_at: str | None = None) -> dict[str, Any]:
    now = datetime.now(timezone.utc)
    start = starts_at or iso8601(now - timedelta(seconds=5))
    end = iso8601(now - timedelta(seconds=1) if resolved else now + timedelta(hours=1))
    service = f"code-nest-sre-e2e-{drill_id.lower()}"
    return {
        "labels": {
            "alertname": ALERT_NAME,
            "severity": "warning",
            "service": service,
            "job": "code-nest",
            "instance": f"synthetic-{drill_id.lower()}",
            "environment": "production",
            "drill": "true",
            "drill_id": drill_id,
        },
        "annotations": {
            "summary": "Code Nest synthetic SRE delivery drill",
            "description": "Bounded firing/resolved acceptance event; no remediation is executed.",
        },
        "startsAt": start,
        "endsAt": end,
        "generatorURL": "https://github.com/xiaou61/Code-Nest/actions",
    }


def alertmanager_alert(alertmanager_url: str, drill_id: str) -> dict[str, Any] | None:
    alerts = request_json("GET", f"{alertmanager_url}/api/v2/alerts")
    if not isinstance(alerts, list):
        raise DrillError("Alertmanager returned an invalid alert list")
    for alert in alerts:
        if isinstance(alert, dict) and alert.get("labels", {}).get("drill_id") == drill_id:
            fingerprint = alert.get("fingerprint")
            if isinstance(fingerprint, str) and fingerprint:
                return alert
    return None


def api_result(payload: Any, label: str) -> Any:
    if not isinstance(payload, dict) or payload.get("code") != 200:
        raise DrillError(f"{label} returned a non-success result")
    return payload.get("data")


def find_incident(application_url: str, admin_token: str, service: str) -> dict[str, Any] | None:
    query = urlencode({"service": service, "pageNum": 1, "pageSize": 20})
    payload = request_json(
        "GET",
        f"{application_url}/api/admin/sre/incidents?{query}",
        bearer_token=admin_token,
    )
    data = api_result(payload, "incident list")
    records = data.get("records") if isinstance(data, dict) else None
    if not isinstance(records, list):
        raise DrillError("incident list has an invalid record collection")
    for incident in records:
        if (
            isinstance(incident, dict)
            and incident.get("service") == service
            and incident.get("alertName") == ALERT_NAME
        ):
            return incident
    return None


def get_incident(application_url: str, admin_token: str, incident_id: int) -> dict[str, Any]:
    payload = request_json(
        "GET",
        f"{application_url}/api/admin/sre/incidents/{incident_id}",
        bearer_token=admin_token,
    )
    incident = api_result(payload, "incident detail")
    if not isinstance(incident, dict):
        raise DrillError("incident detail is invalid")
    return incident


def get_evidence(application_url: str, admin_token: str, incident_id: int) -> list[dict[str, Any]]:
    payload = request_json(
        "GET",
        f"{application_url}/api/admin/sre/incidents/{incident_id}/evidence",
        bearer_token=admin_token,
    )
    evidence = api_result(payload, "incident evidence")
    if not isinstance(evidence, list) or any(not isinstance(item, dict) for item in evidence):
        raise DrillError("incident evidence is invalid")
    return evidence


def webhook_payload(alert: dict[str, Any], status_value: str) -> dict[str, Any]:
    labels = alert.get("labels")
    annotations = alert.get("annotations", {})
    if not isinstance(labels, dict) or not isinstance(annotations, dict):
        raise DrillError("Alertmanager alert labels or annotations are invalid")
    canonical_alert = {
        "status": status_value,
        "labels": labels,
        "annotations": annotations,
        "startsAt": alert.get("startsAt"),
        "endsAt": alert.get("endsAt"),
        "generatorURL": alert.get("generatorURL", ""),
        "fingerprint": alert.get("fingerprint"),
    }
    return {
        "receiver": "qq-email-and-sre-webhook",
        "status": status_value,
        "groupLabels": {"alertname": ALERT_NAME},
        "commonLabels": labels,
        "commonAnnotations": annotations,
        "externalURL": "http://127.0.0.1:19093",
        "alerts": [canonical_alert],
    }


def verify_duplicate_response(payload: Any) -> None:
    data = api_result(payload, "duplicate webhook replay")
    if not isinstance(data, dict) or data.get("duplicates") != 1:
        raise DrillError("duplicate webhook replay was not deduplicated")
    if any(data.get(key, 0) != 0 for key in ("createdEvents", "updatedEvents", "createdIncidents")):
        raise DrillError("duplicate webhook replay changed persistent alert state")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--confirm-notification", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--drill-id", default=datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S"))
    parser.add_argument("--alertmanager-url", default="http://127.0.0.1:19093")
    parser.add_argument("--application-url", default="http://127.0.0.1:9999")
    parser.add_argument("--admin-token-file", default="/etc/code-nest/sre-e2e-admin-token")
    parser.add_argument(
        "--webhook-token-file",
        default="/opt/code-nest/monitoring/secrets/sre_webhook_token",
    )
    parser.add_argument("--poll-timeout-seconds", type=float, default=180)
    parser.add_argument("--resolve-timeout-seconds", type=float, default=420)
    parser.add_argument("--poll-interval-seconds", type=float, default=2)
    return parser.parse_args()


def run(args: argparse.Namespace) -> None:
    if not DRILL_ID_PATTERN.fullmatch(args.drill_id):
        raise DrillError("drill ID must contain 4-40 letters, digits, underscores, or hyphens")
    if not 1 <= args.poll_timeout_seconds <= 900:
        raise DrillError("poll timeout must be between 1 and 900 seconds")
    if not 1 <= args.resolve_timeout_seconds <= 900:
        raise DrillError("resolve timeout must be between 1 and 900 seconds")
    if not 0.01 <= args.poll_interval_seconds <= 30:
        raise DrillError("poll interval must be between 0.01 and 30 seconds")

    firing = build_alert(args.drill_id)
    if args.dry_run:
        print(json.dumps([firing], ensure_ascii=True, indent=2))
        return
    if not args.confirm_notification:
        raise DrillError("--confirm-notification is required because this drill sends QQ notifications")

    alertmanager_url = validate_base_url(args.alertmanager_url, "Alertmanager")
    application_url = validate_base_url(args.application_url, "application")
    admin_token = read_token(args.admin_token_file, "admin token")
    webhook_token = read_token(args.webhook_token_file, "webhook token")
    firing_posted = False
    resolution_posted = False

    try:
        print(f"Posting fixed synthetic firing alert drill_id={args.drill_id}")
        request_json("POST", f"{alertmanager_url}/api/v2/alerts", payload=[firing])
        firing_posted = True
        delivered_alert = wait_for(
            "Alertmanager fingerprint",
            args.poll_timeout_seconds,
            args.poll_interval_seconds,
            lambda: alertmanager_alert(alertmanager_url, args.drill_id),
        )
        service = delivered_alert["labels"]["service"]
        incident = wait_for(
            "SRE incident ingestion",
            args.poll_timeout_seconds,
            args.poll_interval_seconds,
            lambda: find_incident(application_url, admin_token, service),
        )
        incident_id = incident.get("id")
        if not isinstance(incident_id, int) or incident_id <= 0:
            raise DrillError("ingested incident ID is invalid")
        print(f"Incident ingested incident_id={incident_id}")

        evidence = wait_for(
            "operational evidence",
            args.poll_timeout_seconds,
            args.poll_interval_seconds,
            lambda: (
                current
                if REQUIRED_EVIDENCE_TYPES.issubset(
                    {item.get("sourceType") for item in current}
                )
                else None
            )
            if (current := get_evidence(application_url, admin_token, incident_id))
            else None,
        )
        evidence_ids_before = {item.get("id") for item in evidence}
        print("Required alert, deployment, and runbook evidence captured")

        duplicate_body = webhook_payload(delivered_alert, "firing")
        webhook_url = f"{application_url}/api/internal/sre/alertmanager/v1/alerts"
        for replay_number in (1, 2):
            duplicate_response = request_json(
                "POST", webhook_url, payload=duplicate_body, bearer_token=webhook_token
            )
            verify_duplicate_response(duplicate_response)
            print(f"Duplicate replay deduplicated replay={replay_number}")
        evidence_ids_after = {
            item.get("id") for item in get_evidence(application_url, admin_token, incident_id)
        }
        if evidence_ids_after != evidence_ids_before:
            raise DrillError("duplicate webhook replay created additional evidence")

        resolved_alert = build_alert(
            args.drill_id,
            resolved=True,
            starts_at=delivered_alert.get("startsAt"),
        )
        print("Posting synthetic resolved alert")
        request_json("POST", f"{alertmanager_url}/api/v2/alerts", payload=[resolved_alert])
        resolution_posted = True
        wait_for(
            "resolved incident",
            args.resolve_timeout_seconds,
            args.poll_interval_seconds,
            lambda: (
                current
                if current.get("state") == "RESOLVED"
                else None
            )
            if (current := get_incident(application_url, admin_token, incident_id))
            else None,
        )
        wait_for(
            "Alertmanager active-alert cleanup",
            args.resolve_timeout_seconds,
            args.poll_interval_seconds,
            lambda: True if alertmanager_alert(alertmanager_url, args.drill_id) is None else None,
        )
        print(
            f"SRE Alertmanager E2E passed drill_id={args.drill_id} "
            f"incident_id={incident_id} state=RESOLVED"
        )
    finally:
        if firing_posted and not resolution_posted:
            try:
                cleanup = build_alert(args.drill_id, resolved=True, starts_at=firing["startsAt"])
                request_json("POST", f"{alertmanager_url}/api/v2/alerts", payload=[cleanup])
                print("Best-effort resolved cleanup posted", file=sys.stderr)
            except DrillError:
                print("WARNING: best-effort resolved cleanup failed", file=sys.stderr)


def main() -> int:
    try:
        run(parse_args())
        return 0
    except DrillError as exception:
        print(f"SRE Alertmanager E2E failed: {exception}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
