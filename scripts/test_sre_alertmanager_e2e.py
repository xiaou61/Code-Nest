#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import subprocess
import tempfile
import threading
import unittest
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse


SCRIPT = Path(__file__).with_name("sre-alertmanager-e2e.py")


class FakeSreHandler(BaseHTTPRequestHandler):
    alert: dict | None = None
    incident_state = "OPEN"
    duplicate_deliveries = 0

    def log_message(self, _format: str, *_args: object) -> None:
        return

    def send_json(self, status: int, payload: object) -> None:
        body = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def read_json(self) -> object:
        length = int(self.headers.get("Content-Length", "0"))
        return json.loads(self.rfile.read(length))

    def do_POST(self) -> None:  # noqa: N802
        path = urlparse(self.path).path
        payload = self.read_json()
        if path == "/api/v2/alerts":
            alert = payload[0]
            alert["fingerprint"] = "a" * 64
            self.__class__.alert = alert
            ends_at = alert.get("endsAt")
            if ends_at and datetime.fromisoformat(ends_at.replace("Z", "+00:00")) <= datetime.now(timezone.utc):
                self.__class__.incident_state = "RESOLVED"
            else:
                self.__class__.incident_state = "OPEN"
            self.send_response(200)
            self.end_headers()
            return
        if path == "/api/internal/sre/alertmanager/v1/alerts":
            self.__class__.duplicate_deliveries += 1
            self.send_json(202, {
                "code": 200,
                "data": {
                    "received": 1,
                    "createdEvents": 0,
                    "updatedEvents": 0,
                    "duplicates": 1,
                    "createdIncidents": 0,
                    "resolvedIncidents": 0,
                },
            })
            return
        self.send_error(404)

    def do_GET(self) -> None:  # noqa: N802
        parsed = urlparse(self.path)
        if parsed.path == "/api/v2/alerts":
            alerts = (
                []
                if self.__class__.alert is None or self.__class__.incident_state == "RESOLVED"
                else [self.__class__.alert]
            )
            self.send_json(200, alerts)
            return
        if parsed.path == "/api/admin/sre/incidents":
            labels = self.__class__.alert["labels"]
            self.send_json(200, {
                "code": 200,
                "data": {
                    "records": [{
                        "id": 1,
                        "service": labels["service"],
                        "alertName": labels["alertname"],
                        "state": self.__class__.incident_state,
                    }],
                },
            })
            return
        if parsed.path == "/api/admin/sre/incidents/1":
            self.send_json(200, {
                "code": 200,
                "data": {"id": 1, "state": self.__class__.incident_state},
            })
            return
        if parsed.path == "/api/admin/sre/incidents/1/evidence":
            self.send_json(200, {
                "code": 200,
                "data": [
                    {"id": 11, "sourceType": "ALERT_SNAPSHOT"},
                    {"id": 12, "sourceType": "DEPLOYMENT_SNAPSHOT"},
                    {"id": 13, "sourceType": "RUNBOOK_SNAPSHOT"},
                ],
            })
            return
        self.send_error(404)


class SreAlertmanagerE2ETest(unittest.TestCase):
    def setUp(self) -> None:
        FakeSreHandler.alert = None
        FakeSreHandler.incident_state = "OPEN"
        FakeSreHandler.duplicate_deliveries = 0

    def test_dry_run_is_fixed_and_contains_no_credentials(self) -> None:
        result = subprocess.run(
            ["python", str(SCRIPT), "--dry-run", "--drill-id", "20260728-test"],
            check=True,
            capture_output=True,
            text=True,
        )
        payload = json.loads(result.stdout)
        labels = payload[0]["labels"]
        self.assertEqual(labels["alertname"], "CodeNestSreE2E")
        self.assertEqual(labels["severity"], "warning")
        self.assertEqual(labels["drill"], "true")
        self.assertNotIn("token", result.stdout.lower())
        self.assertNotIn("password", result.stdout.lower())

    def test_rejects_non_loopback_application_url_before_reading_tokens(self) -> None:
        result = subprocess.run(
            [
                "python",
                str(SCRIPT),
                "--confirm-notification",
                "--application-url",
                "https://example.com",
            ],
            check=False,
            capture_output=True,
            text=True,
        )

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("application URL must use a loopback host", result.stderr)
        self.assertNotIn("file is missing", result.stderr)

    def test_full_drill_checks_duplicates_evidence_and_resolution(self) -> None:
        server = ThreadingHTTPServer(("127.0.0.1", 0), FakeSreHandler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        base_url = f"http://127.0.0.1:{server.server_port}"
        try:
            with tempfile.TemporaryDirectory() as temporary_directory:
                admin_token = Path(temporary_directory) / "admin-token"
                webhook_token = Path(temporary_directory) / "webhook-token"
                admin_token.write_text("admin-secret", encoding="utf-8")
                webhook_token.write_text("webhook-secret", encoding="utf-8")
                if os.name != "nt":
                    admin_token.chmod(0o600)
                    webhook_token.chmod(0o600)

                result = subprocess.run(
                    [
                        "python",
                        str(SCRIPT),
                        "--confirm-notification",
                        "--drill-id",
                        "20260728-test",
                        "--alertmanager-url",
                        base_url,
                        "--application-url",
                        base_url,
                        "--admin-token-file",
                        str(admin_token),
                        "--webhook-token-file",
                        str(webhook_token),
                        "--poll-timeout-seconds",
                        "2",
                        "--resolve-timeout-seconds",
                        "2",
                        "--poll-interval-seconds",
                        "0.01",
                    ],
                    check=True,
                    capture_output=True,
                    text=True,
                )
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)

        self.assertIn("SRE Alertmanager E2E passed", result.stdout)
        self.assertEqual(FakeSreHandler.duplicate_deliveries, 2)
        self.assertEqual(FakeSreHandler.incident_state, "RESOLVED")


if __name__ == "__main__":
    unittest.main()
