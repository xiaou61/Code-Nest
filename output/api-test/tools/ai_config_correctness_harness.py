#!/usr/bin/env python3
"""Service-level correctness harness for admin AI configuration APIs.

This harness is stricter than HTTP smoke testing:
- success-path operations must return business code 200 and expected fields.
- read-after-write state is asserted where the API has stateful behavior.
- expected auth/validation/business errors are recorded separately.
- AI API keys are never written to result files.
"""

from __future__ import annotations

import argparse
import datetime as dt
import glob
import json
import os
import re
import socket
import sys
import time
import traceback
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Any


SUCCESS_CODE = 200
RUN_ID = dt.datetime.now().strftime("%m%d%H%M%S")


@dataclass
class HttpResult:
    ok: bool
    status: int | None
    elapsed_ms: int
    text: str
    json_body: Any | None
    error: str | None = None


class HarnessFailure(AssertionError):
    pass


def now_iso() -> str:
    return dt.datetime.now(dt.timezone.utc).astimezone().isoformat(timespec="seconds")


def write_json(path: str, data: Any) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def truncate(value: Any, limit: int = 500) -> str:
    text = "" if value is None else str(value)
    text = re.sub(r"\s+", " ", text).strip()
    return text if len(text) <= limit else text[: limit - 3] + "..."


def parse_json(text: str) -> Any | None:
    try:
        return json.loads(text)
    except Exception:
        return None


def result_code(body: Any | None) -> int | None:
    if isinstance(body, dict) and isinstance(body.get("code"), int):
        return int(body["code"])
    return None


def result_message(body: Any | None, text: str) -> str:
    if isinstance(body, dict):
        return truncate(body.get("message"))
    return truncate(text)


def response_data(body: Any | None) -> Any:
    if isinstance(body, dict):
        return body.get("data")
    return None


def http_request(
    base_url: str,
    method: str,
    path: str,
    *,
    query: dict[str, Any] | None = None,
    body: Any = None,
    token: str | None = None,
    timeout: float = 15.0,
) -> HttpResult:
    url = base_url.rstrip("/") + path
    if query:
        url += "?" + urllib.parse.urlencode(query, doseq=True)
    headers = {"Accept": "application/json", "User-Agent": "CodeNestAiConfigCorrectness/1.0"}
    data: bytes | None = None
    if token:
        headers["Authorization"] = "Bearer " + token
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"

    start = time.time()
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read()
            text = raw.decode("utf-8", errors="replace")
            elapsed = int((time.time() - start) * 1000)
            return HttpResult(True, resp.status, elapsed, text, parse_json(text))
    except urllib.error.HTTPError as e:
        raw = e.read()
        text = raw.decode("utf-8", errors="replace")
        elapsed = int((time.time() - start) * 1000)
        return HttpResult(True, e.code, elapsed, text, parse_json(text))
    except Exception as e:
        elapsed = int((time.time() - start) * 1000)
        return HttpResult(False, None, elapsed, "", None, f"{type(e).__name__}: {truncate(e)}")


def latest_log_files() -> list[str]:
    candidates: list[str] = []
    for pattern in (
        "logs/code-nest*.log",
        "logs/*.log",
        "/opt/code-nest/logs/code-nest*.log",
        "/opt/code-nest/logs/*.log",
    ):
        candidates.extend(glob.glob(pattern))
    return sorted(set(candidates), key=lambda p: os.path.getmtime(p), reverse=True)[:4]


def captcha_code_from_logs(captcha_key: str) -> str | None:
    pattern = re.compile(re.escape(captcha_key) + r".*?code:\s*([A-Za-z0-9]{4,6})")
    for path in latest_log_files():
        try:
            with open(path, "rb") as f:
                f.seek(0, os.SEEK_END)
                size = f.tell()
                f.seek(max(0, size - 512_000))
                text = f.read().decode("utf-8", errors="ignore")
            match = pattern.search(text)
            if match:
                return match.group(1)
        except Exception:
            continue
    return None


def redis_get_raw(db: int, key: str) -> bytes | None:
    def encode(parts: list[str | bytes]) -> bytes:
        data = b"*" + str(len(parts)).encode() + b"\r\n"
        for part in parts:
            if isinstance(part, str):
                part = part.encode("utf-8")
            data += b"$" + str(len(part)).encode() + b"\r\n" + part + b"\r\n"
        return data

    def read_resp(sock: socket.socket) -> Any:
        line = b""
        while not line.endswith(b"\r\n"):
            chunk = sock.recv(1)
            if not chunk:
                return None
            line += chunk
        line = line[:-2]
        typ = line[:1]
        body = line[1:]
        if typ == b"+":
            return body
        if typ == b":":
            return int(body)
        if typ == b"$":
            length = int(body)
            if length < 0:
                return None
            data = b""
            while len(data) < length + 2:
                data += sock.recv(length + 2 - len(data))
            return data[:length]
        if typ == b"*":
            return [read_resp(sock) for _ in range(int(body))]
        if typ == b"-":
            raise RuntimeError(body.decode("utf-8", errors="replace"))
        return line

    sock = socket.socket()
    sock.settimeout(4)
    try:
        sock.connect(("127.0.0.1", 6379))
        sock.sendall(encode(["SELECT", str(db)]))
        read_resp(sock)
        sock.sendall(encode(["GET", key]))
        return read_resp(sock)
    finally:
        sock.close()


def captcha_code_from_redis(captcha_key: str) -> str | None:
    for db in (3, 0, 1, 2, 4, 5, 6, 7):
        try:
            raw = redis_get_raw(db, "user:captcha:" + captcha_key)
        except Exception:
            continue
        if not raw:
            continue
        candidates = []
        if len(raw) >= 4:
            candidates.append(raw[-4:])
        if len(raw) > 1 and raw[0] in {3, 4, 5}:
            candidates.append(raw[1:])
        for candidate in candidates:
            try:
                text = candidate.decode("ascii")
            except Exception:
                continue
            if re.fullmatch(r"[A-Za-z0-9]{4,6}", text):
                return text
    return None


def generate_captcha(base_url: str) -> tuple[str, str]:
    http = http_request(base_url, "GET", "/captcha/generate", timeout=8)
    assert_success_http(http, "GET /captcha/generate")
    data = response_data(http.json_body)
    key = data.get("captchaKey") if isinstance(data, dict) else None
    if not key:
        raise HarnessFailure("captchaKey missing from captcha response")
    code = captcha_code_from_logs(key) or captcha_code_from_redis(key)
    if not code:
        raise HarnessFailure("captcha code could not be acquired from logs or Redis")
    return key, code


def assert_success_http(http: HttpResult, step: str) -> None:
    if not http.ok:
        raise HarnessFailure(f"{step}: transport error: {http.error}")
    if http.status is None or http.status >= 500:
        raise HarnessFailure(f"{step}: HTTP failure status={http.status}, body={truncate(http.text)}")
    code = result_code(http.json_body)
    if code != SUCCESS_CODE:
        raise HarnessFailure(
            f"{step}: expected business code 200, got status={http.status}, "
            f"code={code}, message={result_message(http.json_body, http.text)}"
        )


def assert_business_error(http: HttpResult, step: str, message_part: str | None = None) -> None:
    if not http.ok:
        raise HarnessFailure(f"{step}: transport error during expected business error: {http.error}")
    if http.status is None or http.status >= 500:
        raise HarnessFailure(f"{step}: expected business error, got HTTP status={http.status}")
    code = result_code(http.json_body)
    message = result_message(http.json_body, http.text)
    if code == SUCCESS_CODE:
        raise HarnessFailure(f"{step}: expected business error but got success")
    if message_part and message_part not in message:
        raise HarnessFailure(f"{step}: expected message containing {message_part!r}, got {message!r}")


def require_dict(value: Any, step: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise HarnessFailure(f"{step}: expected response data object, got {type(value).__name__}")
    return value


def require_list(value: Any, step: str) -> list[Any]:
    if not isinstance(value, list):
        raise HarnessFailure(f"{step}: expected response data list, got {type(value).__name__}")
    return value


def contains_raw_secret(value: Any, secret: str | None) -> bool:
    if not secret:
        return False
    return secret in json.dumps(value, ensure_ascii=False)


def mask_secret(text: str, secret: str | None) -> str:
    if not secret:
        return text
    return text.replace(secret, "[REDACTED_AI_API_KEY]")


class AiConfigCorrectnessHarness:
    def __init__(self, base_url: str, out_dir: str, ai_base_url: str | None = None, ai_model: str | None = None, ai_api_key: str | None = None):
        self.base_url = base_url.rstrip("/")
        self.out_dir = out_dir
        self.ai_base_url = ai_base_url
        self.ai_model = ai_model
        self.ai_api_key = ai_api_key
        self.results: list[dict[str, Any]] = []
        self.context: dict[str, Any] = {
            "runId": RUN_ID,
            "baseUrl": self.base_url,
            "aiLiveConfigured": bool(ai_base_url and ai_model and ai_api_key),
            "aiLiveBaseUrl": ai_base_url,
            "aiLiveModel": ai_model,
        }
        self.admin_token: str | None = None

    def record(
        self,
        name: str,
        method: str,
        path: str,
        http: HttpResult,
        assertion: str,
        status: str,
        *,
        expected: str = "SUCCESS",
        details: dict[str, Any] | None = None,
    ) -> None:
        safe_details = details or {}
        if contains_raw_secret(safe_details, self.ai_api_key):
            raise HarnessFailure(f"{name}: attempted to record raw AI API key in result details")
        self.results.append(
            {
                "step": len(self.results) + 1,
                "name": name,
                "method": method,
                "path": path,
                "expected": expected,
                "status": status,
                "httpStatus": http.status,
                "businessCode": result_code(http.json_body),
                "message": mask_secret(result_message(http.json_body, http.text) if http.ok else truncate(http.error), self.ai_api_key),
                "elapsedMs": http.elapsed_ms,
                "assertion": assertion,
                "details": safe_details,
                "testedAt": now_iso(),
            }
        )

    def call_success(
        self,
        name: str,
        method: str,
        path: str,
        *,
        query: dict[str, Any] | None = None,
        body: Any = None,
        token: str | None = None,
        assertion: str = "business code 200",
        record_details: dict[str, Any] | None = None,
        timeout: float = 15.0,
    ) -> Any:
        http = http_request(self.base_url, method, path, query=query, body=body, token=token, timeout=timeout)
        try:
            assert_success_http(http, f"{method} {path}")
            data = response_data(http.json_body)
            if contains_raw_secret(data, self.ai_api_key):
                raise HarnessFailure(f"{method} {path}: response data leaked raw AI API key")
            self.record(name, method, self.path_with_query(path, query), http, assertion, "PASS", details=record_details)
            return data
        except Exception:
            self.record(name, method, self.path_with_query(path, query), http, assertion, "FAIL", details=record_details)
            raise

    def call_expected_error(
        self,
        name: str,
        method: str,
        path: str,
        *,
        message_part: str | None = None,
        query: dict[str, Any] | None = None,
        body: Any = None,
        token: str | None = None,
        assertion: str = "expected business error",
    ) -> None:
        http = http_request(self.base_url, method, path, query=query, body=body, token=token)
        try:
            if contains_raw_secret(http.json_body, self.ai_api_key) or contains_raw_secret(http.text, self.ai_api_key):
                raise HarnessFailure(f"{method} {path}: error response leaked raw AI API key")
            assert_business_error(http, f"{method} {path}", message_part)
            self.record(
                name,
                method,
                self.path_with_query(path, query),
                http,
                assertion,
                "PASS_EXPECTED_ERROR",
                expected="BUSINESS_ERROR",
                details={"expectedMessagePart": message_part},
            )
        except Exception:
            self.record(
                name,
                method,
                self.path_with_query(path, query),
                http,
                assertion,
                "FAIL",
                expected="BUSINESS_ERROR",
                details={"expectedMessagePart": message_part},
            )
            raise

    @staticmethod
    def path_with_query(path: str, query: dict[str, Any] | None) -> str:
        if not query:
            return path
        return path + "?" + urllib.parse.urlencode(query, doseq=True)

    def admin_login(self) -> None:
        login_body = {"username": "admin", "password": "123456", "rememberMe": False}
        login_data = self.call_success(
            "admin login",
            "POST",
            "/auth/login",
            body=login_body,
            assertion="seed admin can log in and obtain an access token",
        )
        login = require_dict(login_data, "admin login")
        token = login.get("accessToken")
        user_info = login.get("userInfo")
        if not isinstance(token, str) or not token:
            raise HarnessFailure("admin login: response missing accessToken")
        if not isinstance(user_info, dict) or user_info.get("username") != "admin":
            raise HarnessFailure("admin login: userInfo should identify admin user")
        self.admin_token = token

    def run(self) -> dict[str, Any]:
        failure: str | None = None
        try:
            self.admin_login()
            token = self.admin_token
            if not token:
                raise HarnessFailure("admin token missing after login")

            self.call_expected_error(
                "runtime config requires admin auth",
                "GET",
                "/admin/ai/config/runtime",
                message_part="Token",
                assertion="admin AI config runtime endpoint rejects anonymous access",
            )

            runtime = require_dict(
                self.call_success(
                    "get AI runtime config",
                    "GET",
                    "/admin/ai/config/runtime",
                    token=token,
                    assertion="runtime config returns provider/model/config flags without raw secrets",
                ),
                "get AI runtime config",
            )
            provider = runtime.get("provider")
            if provider != "openai-compatible":
                raise HarnessFailure(f"runtime config: expected provider openai-compatible, got {provider!r}")
            for bool_key in ("configured", "apiKeyConfigured", "pricingConfigured", "metricsPersistenceEnabled", "ragEnabled", "ragApiKeyConfigured"):
                if not isinstance(runtime.get(bool_key), bool):
                    raise HarnessFailure(f"runtime config: {bool_key} should be boolean")
            if runtime.get("apiKeyMasked") and "******" not in str(runtime.get("apiKeyMasked")):
                raise HarnessFailure("runtime config: apiKeyMasked should be masked")
            if runtime.get("ragApiKeyMasked") and "******" not in str(runtime.get("ragApiKeyMasked")):
                raise HarnessFailure("runtime config: ragApiKeyMasked should be masked")

            schema_catalog = require_dict(
                self.call_success(
                    "get AI schema catalog",
                    "GET",
                    "/admin/ai/config/schema-catalog",
                    token=token,
                    assertion="schema catalog exposes prompts, RAG queries, retrieval profiles, and structured schemas",
                ),
                "get AI schema catalog",
            )
            prompts = require_list(schema_catalog.get("prompts"), "schema catalog prompts")
            rag_queries = require_list(schema_catalog.get("ragQueries"), "schema catalog ragQueries")
            retrieval_profiles = require_list(schema_catalog.get("retrievalProfiles"), "schema catalog retrievalProfiles")
            structured_schemas = require_list(schema_catalog.get("structuredSchemas"), "schema catalog structuredSchemas")
            if not prompts:
                raise HarnessFailure("schema catalog: prompts should not be empty")
            if not structured_schemas:
                raise HarnessFailure("schema catalog: structuredSchemas should not be empty")
            self.context["schemaCatalogCounts"] = {
                "prompts": len(prompts),
                "ragQueries": len(rag_queries),
                "retrievalProfiles": len(retrieval_profiles),
                "structuredSchemas": len(structured_schemas),
            }

            regression_cases = require_dict(
                self.call_success(
                    "get AI regression cases",
                    "GET",
                    "/admin/ai/config/regression/cases",
                    token=token,
                    assertion="regression case catalog reports consistent counts",
                ),
                "get AI regression cases",
            )
            cases = require_list(regression_cases.get("cases"), "regression cases")
            if regression_cases.get("totalCount") != len(cases):
                raise HarnessFailure("regression cases: totalCount should match cases length")
            if not cases:
                raise HarnessFailure("regression cases: catalog should not be empty")
            self.context["regressionCaseCount"] = len(cases)

            latest = self.call_success(
                "get latest AI regression run",
                "GET",
                "/admin/ai/config/regression/latest",
                token=token,
                assertion="latest regression run endpoint is readable even before any run is present",
            )
            if latest is not None and not isinstance(latest, dict):
                raise HarnessFailure("latest regression run: data should be null or object")

            history = require_dict(
                self.call_success(
                    "get AI regression history limit",
                    "GET",
                    "/admin/ai/config/regression/history",
                    query={"limit": 3},
                    token=token,
                    assertion="history endpoint honors valid limit and returns a run list",
                ),
                "get AI regression history limit",
            )
            if history.get("limit") != 3:
                raise HarnessFailure("regression history: returned limit should be 3")
            runs = require_list(history.get("runs"), "regression history runs")
            if not isinstance(history.get("totalCount"), int) or history.get("totalCount") != len(runs):
                raise HarnessFailure("regression history: totalCount should match runs length")

            scenario_health = require_dict(
                self.call_success(
                    "get AI regression scenario health",
                    "GET",
                    "/admin/ai/config/regression/scenario-health",
                    query={"limit": 3},
                    token=token,
                    assertion="scenario health endpoint honors valid limit and returns scenario list",
                ),
                "get AI regression scenario health",
            )
            if scenario_health.get("limit") != 3:
                raise HarnessFailure("scenario health: returned limit should be 3")
            scenarios = require_list(scenario_health.get("scenarios"), "scenario health scenarios")
            if not isinstance(scenario_health.get("totalCount"), int) or scenario_health.get("totalCount") != len(scenarios):
                raise HarnessFailure("scenario health: totalCount should match scenarios length")

            self.call_expected_error(
                "regression history rejects invalid limit",
                "GET",
                "/admin/ai/config/regression/history",
                query={"limit": 99},
                token=token,
                message_part="limit 取值范围",
                assertion="invalid history limit is a controlled business/validation error",
            )

            metrics_before = require_dict(
                self.call_success(
                    "get AI metrics before clear",
                    "GET",
                    "/admin/ai/config/metrics",
                    query={"recentLimit": 5},
                    token=token,
                    assertion="metrics endpoint returns overview and aggregate lists",
                ),
                "get AI metrics before clear",
            )
            self.assert_metrics_shape(metrics_before, "get AI metrics before clear")

            self.call_success(
                "clear AI metrics",
                "DELETE",
                "/admin/ai/config/metrics",
                token=token,
                assertion="admin can clear runtime metrics",
            )
            metrics_after = require_dict(
                self.call_success(
                    "read back cleared AI metrics",
                    "GET",
                    "/admin/ai/config/metrics",
                    query={"recentLimit": 5},
                    token=token,
                    assertion="read-back metrics show cleared zero invocation state",
                ),
                "read back cleared AI metrics",
            )
            self.assert_metrics_shape(metrics_after, "read back cleared AI metrics")
            overview_after = require_dict(metrics_after.get("overview"), "cleared metrics overview")
            if overview_after.get("totalInvocations") != 0:
                raise HarnessFailure("read back cleared AI metrics: totalInvocations should be 0 after clear")
            if require_list(metrics_after.get("recentCalls"), "cleared metrics recentCalls"):
                raise HarnessFailure("read back cleared AI metrics: recentCalls should be empty after clear")

            self.call_expected_error(
                "metrics rejects invalid recent limit",
                "GET",
                "/admin/ai/config/metrics",
                query={"recentLimit": 99},
                token=token,
                message_part="recentLimit",
                assertion="invalid metrics recentLimit is a controlled business/validation error",
            )

            rag_health = require_dict(
                self.call_success(
                    "get RAG service health",
                    "GET",
                    "/admin/ai/config/rag-service/health",
                    token=token,
                    assertion="RAG health reports configured endpoint, reachability, and sanitized key state",
                ),
                "get RAG service health",
            )
            for bool_key in ("ragEnabled", "apiKeyConfigured", "reachable"):
                if not isinstance(rag_health.get(bool_key), bool):
                    raise HarnessFailure(f"RAG health: {bool_key} should be boolean")
            if rag_health.get("apiKeyMasked") and "******" not in str(rag_health.get("apiKeyMasked")):
                raise HarnessFailure("RAG health: apiKeyMasked should be masked")
            if not isinstance(rag_health.get("message"), str) or not rag_health.get("message"):
                raise HarnessFailure("RAG health: message should be present")

            self.call_expected_error(
                "RAG documents rejects invalid limit",
                "GET",
                "/admin/ai/config/rag-service/documents",
                query={"limit": 0},
                token=token,
                message_part="limit 必须",
                assertion="invalid RAG document limit fails before any external service call",
            )
            self.call_expected_error(
                "RAG batch delete rejects empty ids",
                "POST",
                "/admin/ai/config/rag-service/documents/batch-delete",
                body={"documentIds": []},
                token=token,
                message_part="documentIds 不能为空",
                assertion="empty batch delete is rejected before any external service call",
            )
            missing_key_test = require_dict(
                self.call_success(
                    "AI config test reports missing API key",
                    "POST",
                    "/admin/ai/config/test",
                    body={"baseUrl": "https://example.invalid", "model": "connectivity-probe"},
                    token=token,
                    assertion="connectivity test action succeeds while data reports unavailable missing-key state",
                ),
                "AI config test reports missing API key",
            )
            if missing_key_test.get("available") is not False:
                raise HarnessFailure("AI config test reports missing API key: available should be false")
            if "缺少 API Key" not in str(missing_key_test.get("message")):
                raise HarnessFailure("AI config test reports missing API key: message should explain missing API key")
            if missing_key_test.get("apiKeyMasked") is not None:
                raise HarnessFailure("AI config test reports missing API key: apiKeyMasked should be null")

            if self.ai_base_url and self.ai_model and self.ai_api_key:
                live_data = require_dict(
                    self.call_success(
                        "test provided AI config live",
                        "POST",
                        "/admin/ai/config/test",
                        body={"baseUrl": self.ai_base_url, "model": self.ai_model, "apiKey": self.ai_api_key},
                        token=token,
                        assertion="provided AI config is tested without leaking the raw API key",
                        record_details={"aiBaseUrl": self.ai_base_url, "aiModel": self.ai_model},
                        timeout=90.0,
                    ),
                    "test provided AI config live",
                )
                if live_data.get("baseUrl") != self.ai_base_url.rstrip("/"):
                    raise HarnessFailure("live AI config test: baseUrl mismatch")
                if live_data.get("model") != self.ai_model:
                    raise HarnessFailure("live AI config test: model mismatch")
                if live_data.get("usedConfiguredApiKey") is not False:
                    raise HarnessFailure("live AI config test: should use request API key")
                masked = live_data.get("apiKeyMasked")
                if not isinstance(masked, str) or "******" not in masked:
                    raise HarnessFailure("live AI config test: apiKeyMasked should be present and masked")
                if contains_raw_secret(live_data, self.ai_api_key):
                    raise HarnessFailure("live AI config test: response leaked raw API key")
                if live_data.get("available") is not True:
                    raise HarnessFailure(f"live AI config test: expected available=true, got message={live_data.get('message')!r}")

            self.cleanup()
        except Exception as e:
            failure = f"{type(e).__name__}: {truncate(e, 1000)}"
            self.context["traceback"] = traceback.format_exc(limit=8)
            try:
                self.cleanup()
            except Exception as cleanup_error:
                self.context["cleanupError"] = truncate(cleanup_error, 1000)

        summary = {
            "runId": RUN_ID,
            "generatedAt": now_iso(),
            "baseUrl": self.base_url,
            "service": "admin-ai-config-controller",
            "status": "PASS" if failure is None else "FAIL",
            "failure": failure,
            "stepCount": len(self.results),
            "failureCount": sum(1 for item in self.results if item["status"] == "FAIL"),
            "byStatus": self.by_status(),
            "failedSteps": [item for item in self.results if item["status"] == "FAIL"],
            "context": self.context,
        }
        write_json(os.path.join(self.out_dir, "results.json"), self.results)
        write_json(os.path.join(self.out_dir, "summary.json"), summary)
        return summary

    def assert_metrics_shape(self, metrics: dict[str, Any], step: str) -> None:
        overview = require_dict(metrics.get("overview"), f"{step} overview")
        for key in (
            "totalInvocations",
            "successCount",
            "errorCount",
            "fallbackCount",
            "structuredParseFailureCount",
            "totalInputTokens",
            "totalOutputTokens",
            "totalTokens",
            "averageLatencyMs",
        ):
            if not isinstance(overview.get(key), int):
                raise HarnessFailure(f"{step}: overview.{key} should be integer")
        require_list(metrics.get("sceneStats"), f"{step} sceneStats")
        require_list(metrics.get("modelStats"), f"{step} modelStats")
        require_list(metrics.get("recentCalls"), f"{step} recentCalls")

    def cleanup(self) -> None:
        # Metrics are intentionally left cleared by this harness because the
        # clear/read-back pair is the state transition under test.
        return None

    def by_status(self) -> dict[str, int]:
        counts: dict[str, int] = {}
        for item in self.results:
            counts[item["status"]] = counts.get(item["status"], 0) + 1
        return counts


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:9999/api")
    parser.add_argument("--out-dir", required=True)
    parser.add_argument("--ai-base-url", default=os.environ.get("CODE_NEST_AI_TEST_BASE_URL"))
    parser.add_argument("--ai-model", default=os.environ.get("CODE_NEST_AI_TEST_MODEL"))
    parser.add_argument("--ai-api-key-env", default="CODE_NEST_AI_TEST_API_KEY")
    args = parser.parse_args()

    ai_api_key = os.environ.get(args.ai_api_key_env)
    harness = AiConfigCorrectnessHarness(
        args.base_url,
        args.out_dir,
        ai_base_url=args.ai_base_url,
        ai_model=args.ai_model,
        ai_api_key=ai_api_key,
    )
    summary = harness.run()
    print(json.dumps({k: v for k, v in summary.items() if k != "context"}, ensure_ascii=False))
    return 0 if summary["status"] == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
