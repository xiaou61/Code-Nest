#!/usr/bin/env python3
"""OpenAPI driven HTTP smoke harness for Code-Nest.

The harness is intentionally conservative:
- every OpenAPI operation gets one primary result row;
- destructive collection operations are exercised through auth/validation paths;
- tokens and captcha values are never written to result files.
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


HTTP_METHODS = ("get", "post", "put", "delete", "patch")
JSON_CONTENT_TYPES = ("application/json", "*/*")
AUTH_CODES = {701, 702, 703, 704, 705}
SUCCESS_CODE = 200
BUSINESS_SERVER_ERROR_CODE = 500
RUN_ID = dt.datetime.now().strftime("%m%d%H%M%S")
TEST_PASSWORD = "ApiTest123456"


@dataclass
class HttpResult:
    ok: bool
    status: int | None
    elapsed_ms: int
    text: str
    json_body: Any | None
    error: str | None = None


def now_iso() -> str:
    return dt.datetime.now(dt.timezone.utc).astimezone().isoformat(timespec="seconds")


def load_json(path: str) -> Any:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: str, data: Any) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def truncate(value: Any, limit: int = 220) -> str:
    text = "" if value is None else str(value)
    text = re.sub(r"\s+", " ", text).strip()
    return text if len(text) <= limit else text[: limit - 3] + "..."


def resolve_ref(spec: dict[str, Any], schema: Any) -> Any:
    if not isinstance(schema, dict):
        return schema
    ref = schema.get("$ref")
    if not ref:
        return schema
    if not ref.startswith("#/"):
        return schema
    node: Any = spec
    for part in ref[2:].split("/"):
        node = node.get(part, {})
    return node


def merged_schema(spec: dict[str, Any], schema: Any) -> dict[str, Any]:
    schema = resolve_ref(spec, schema)
    if not isinstance(schema, dict):
        return {}
    result = dict(schema)
    for key in ("allOf", "oneOf", "anyOf"):
        items = result.pop(key, None)
        if not items:
            continue
        merged: dict[str, Any] = {}
        required: list[str] = []
        for item in items:
            sub = merged_schema(spec, item)
            merged.update(sub)
            for req in sub.get("required", []) or []:
                if req not in required:
                    required.append(req)
        if required:
            merged["required"] = required
        result.update(merged)
    return result


def extract_operations(spec: dict[str, Any]) -> list[dict[str, Any]]:
    operations: list[dict[str, Any]] = []
    for path, item in (spec.get("paths") or {}).items():
        if not isinstance(item, dict):
            continue
        path_params = item.get("parameters") or []
        for method in HTTP_METHODS:
            op = item.get(method)
            if not isinstance(op, dict):
                continue
            operations.append(
                {
                    "method": method.upper(),
                    "method_lc": method,
                    "path": path,
                    "operationId": op.get("operationId") or "",
                    "summary": op.get("summary") or "",
                    "tags": op.get("tags") or [],
                    "parameters": list(path_params) + list(op.get("parameters") or []),
                    "requestBody": op.get("requestBody"),
                    "security": op.get("security") or item.get("security") or [],
                }
            )
    return operations


def schema_type(schema: dict[str, Any]) -> str | None:
    typ = schema.get("type")
    if typ:
        return typ
    if "properties" in schema:
        return "object"
    if "items" in schema:
        return "array"
    return None


def int_value(name: str, schema: dict[str, Any], *, path_param: bool, unsafe_id: bool) -> int:
    lname = name.lower()
    if lname in {"pagesize", "page_size", "limit", "size"}:
        return 10
    if lname in {"pagenum", "page_num", "page", "current"}:
        return 1
    if lname in {"status", "state", "enabled", "isenabled", "isdefault", "istop", "isessence"}:
        minimum = schema.get("minimum")
        maximum = schema.get("maximum")
        if minimum is not None:
            return int(minimum)
        if maximum == 1:
            return 1
        return 0
    if lname in {"role"}:
        return int(schema.get("minimum") or 1)
    if "count" in lname or "amount" in lname or "score" in lname or "points" in lname:
        return 1
    if lname.endswith("id") or lname == "id" or path_param:
        return 999999999 if unsafe_id else 1
    return int(schema.get("minimum") or schema.get("default") or 1)


def string_value(name: str, schema: dict[str, Any]) -> str:
    lname = name.lower()
    if schema.get("enum"):
        return str(schema["enum"][0])
    if "email" in lname:
        return f"api_probe_{RUN_ID}@example.com"
    if "phone" in lname or "mobile" in lname:
        return "139" + RUN_ID[-8:]
    if "password" in lname:
        return TEST_PASSWORD
    if lname == "confirmpassword":
        return TEST_PASSWORD
    if "username" in lname:
        return f"apitest{RUN_ID[-8:]}"
    if "captcha" in lname:
        return "0000"
    if "date" in lname and "time" not in lname:
        return "2026-06-06"
    if "time" in lname:
        return "2026-06-06 12:00:00"
    if "url" in lname:
        return "https://example.com"
    if "lang" in lname or "language" in lname:
        return "java"
    if "type" in lname:
        return "test"
    if "title" in lname or lname.endswith("name") or "name" in lname:
        return f"API Probe {RUN_ID}"
    if "content" in lname or "description" in lname or "remark" in lname:
        return f"Code-Nest API probe content {RUN_ID}"
    if "keyword" in lname or "query" in lname or "search" in lname:
        return "test"
    return schema.get("default") or schema.get("example") or "test"


def sample_for_schema(
    spec: dict[str, Any],
    schema: Any,
    *,
    name: str = "value",
    depth: int = 0,
    unsafe_id: bool = False,
    path_param: bool = False,
) -> Any:
    schema = merged_schema(spec, schema)
    if not schema:
        return string_value(name, {})
    if schema.get("enum"):
        return schema["enum"][0]
    typ = schema_type(schema)
    fmt = schema.get("format")
    if typ == "integer":
        return int_value(name, schema, path_param=path_param, unsafe_id=unsafe_id)
    if typ == "number":
        return float(schema.get("minimum") or schema.get("default") or 1)
    if typ == "boolean":
        return bool(schema.get("default", True))
    if typ == "array":
        return [
            sample_for_schema(
                spec,
                schema.get("items") or {"type": "string"},
                name=name,
                depth=depth + 1,
                unsafe_id=unsafe_id,
            )
        ]
    if typ == "object":
        if depth > 3:
            return {}
        props = schema.get("properties") or {}
        required = set(schema.get("required") or [])
        keys = list(props.keys())
        selected = [k for k in keys if k in required]
        # Springdoc often omits bean validation metadata. Include useful optional
        # scalar fields so requests reach controller/service validation paths.
        for k in keys:
            if k not in selected and len(selected) < 24:
                selected.append(k)
        body: dict[str, Any] = {}
        for prop in selected:
            if prop.lower() in {"id", "createby", "updateby", "createtime", "updatetime"} and prop not in required:
                continue
            body[prop] = sample_for_schema(
                spec,
                props[prop],
                name=prop,
                depth=depth + 1,
                unsafe_id=unsafe_id,
            )
        if "password" in body and "confirmPassword" in props:
            body["confirmPassword"] = body["password"]
        return body
    if fmt == "date":
        return "2026-06-06"
    if fmt in {"date-time", "datetime"}:
        return "2026-06-06 12:00:00"
    return string_value(name, schema)


def request_body_sample(spec: dict[str, Any], request_body: Any, *, unsafe_id: bool) -> tuple[Any, str]:
    if not request_body:
        return None, ""
    content = (request_body or {}).get("content") or {}
    for ctype, media in content.items():
        if ctype in JSON_CONTENT_TYPES or "json" in ctype:
            schema = (media or {}).get("schema") or {}
            return sample_for_schema(spec, schema, unsafe_id=unsafe_id), "application/json"
    for ctype, media in content.items():
        if "multipart/form-data" in ctype:
            schema = (media or {}).get("schema") or {"type": "object"}
            return sample_for_schema(spec, schema, unsafe_id=unsafe_id), "multipart/form-data"
    return {}, "application/json"


def is_high_risk_operation(op: dict[str, Any]) -> bool:
    method = op["method"]
    path = op["path"].lower()
    oid = (op.get("operationId") or "").lower()
    text = f"{path} {oid} {' '.join(op.get('tags') or [])}".lower()
    risky_words = (
        "clear",
        "clean",
        "flush",
        "batch-delete",
        "batchdelete",
        "batch",
        "logout",
        "upload",
        "import",
        "export",
        "migrate",
        "migration",
        "submit",
        "judge",
        "run",
        "debug",
        "test",
        "ai/config/test",
        "regression/run",
        "rag",
        "optimize",
        "send",
        "kick",
        "mute",
        "transfer",
        "reset-password",
    )
    if method == "DELETE" and "{" not in path:
        return True
    return method in {"POST", "PUT", "DELETE", "PATCH"} and any(word in text for word in risky_words)


def choose_auth_mode(op: dict[str, Any], *, high_risk: bool) -> str:
    path = op["path"].lower()
    tags = " ".join(op.get("tags") or []).lower()
    if path in {"/auth/login", "/captcha/generate", "/captcha/verify"}:
        return "none"
    if path.startswith("/user/auth/"):
        return "none"
    if path.startswith("/sensitive/") and not path.startswith("/sensitive/check"):
        return "admin"
    if high_risk:
        return "none-safety"
    if path.startswith("/auth/"):
        return "admin"
    if path.startswith("/admin/") or "admin" in tags or "管理端" in tags or "管理后台" in tags or "日志管理" in tags:
        return "admin"
    user_prefixes = (
        "/user/",
        "/resume",
        "/flashcard",
        "/notification",
        "/points",
        "/lottery",
        "/chat",
        "/team",
    )
    if path.startswith(user_prefixes) or "user" in tags or "用户端" in tags:
        return "user"
    if op["method"] in {"POST", "PUT", "DELETE", "PATCH"}:
        return "user"
    return "none"


def param_value(spec: dict[str, Any], param: dict[str, Any], *, unsafe_id: bool) -> Any:
    name = param.get("name") or "param"
    schema = merged_schema(spec, param.get("schema") or {})
    return sample_for_schema(spec, schema, name=name, unsafe_id=unsafe_id, path_param=param.get("in") == "path")


def build_request(spec: dict[str, Any], op: dict[str, Any], *, unsafe_id: bool) -> tuple[str, dict[str, Any], Any, str]:
    path = op["path"]
    query: dict[str, Any] = {}
    for param in op.get("parameters") or []:
        if not isinstance(param, dict):
            continue
        loc = param.get("in")
        name = param.get("name")
        if not name:
            continue
        value = param_value(spec, param, unsafe_id=unsafe_id)
        if loc == "path":
            path = path.replace("{" + name + "}", urllib.parse.quote(str(value), safe=""))
        elif loc == "query":
            query[name] = value
    body, ctype = request_body_sample(spec, op.get("requestBody"), unsafe_id=unsafe_id)
    return path, query, body, ctype


def http_request(
    base_url: str,
    method: str,
    path: str,
    *,
    query: dict[str, Any] | None = None,
    body: Any = None,
    content_type: str = "",
    token: str | None = None,
    timeout: float = 8.0,
) -> HttpResult:
    url = base_url.rstrip("/") + path
    if query:
        url += "?" + urllib.parse.urlencode(query, doseq=True)
    headers = {"Accept": "application/json", "User-Agent": "CodeNestApiHarness/1.0"}
    data: bytes | None = None
    if token:
        headers["Authorization"] = "Bearer " + token
    if body is not None:
        if content_type == "multipart/form-data":
            boundary = "----CodeNestApiHarnessBoundary"
            chunks: list[bytes] = []
            body_dict = body if isinstance(body, dict) else {}
            for key, value in body_dict.items():
                chunks.append(f"--{boundary}\r\n".encode())
                if "file" in key.lower():
                    chunks.append(
                        (
                            f'Content-Disposition: form-data; name="{key}"; filename="api-probe.txt"\r\n'
                            "Content-Type: text/plain\r\n\r\n"
                        ).encode()
                    )
                    chunks.append(b"code-nest api probe\n")
                else:
                    chunks.append(f'Content-Disposition: form-data; name="{key}"\r\n\r\n'.encode())
                    chunks.append(str(value).encode())
                chunks.append(b"\r\n")
            chunks.append(f"--{boundary}--\r\n".encode())
            data = b"".join(chunks)
            headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
        else:
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
        return HttpResult(False, None, elapsed, "", None, f"{type(e).__name__}: {truncate(e, 500)}")


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


def classify(http: HttpResult) -> str:
    if not http.ok:
        return "FAIL_TRANSPORT"
    if http.status is None:
        return "FAIL_TRANSPORT"
    code = result_code(http.json_body)
    if http.status >= 500:
        return "FAIL_HTTP_5XX"
    if code == BUSINESS_SERVER_ERROR_CODE:
        return "FAIL_BUSINESS_500"
    if 200 <= http.status < 300 and code == SUCCESS_CODE:
        return "PASS_SUCCESS"
    if 200 <= http.status < 300 and code in AUTH_CODES:
        return "PASS_AUTH"
    if http.status in {400, 405, 413, 415} or code in {400, 401, 403, 404, 405, 409, 415, 601}:
        return "PASS_VALIDATION"
    if 200 <= http.status < 300:
        return "PASS_BUSINESS_ERROR"
    if 300 <= http.status < 500:
        return "PASS_HTTP_CLIENT_ERROR"
    return "UNKNOWN"


def latest_log_files() -> list[str]:
    candidates = []
    for pattern in (
        "logs/code-nest*.log",
        "logs/*.log",
        "/opt/code-nest/logs/code-nest*.log",
        "/opt/code-nest/logs/*.log",
    ):
        candidates.extend(glob.glob(pattern))
    return sorted(set(candidates), key=lambda p: os.path.getmtime(p), reverse=True)[:4]


def captcha_code_from_logs(captcha_key: str) -> str | None:
    # Captcha values are used in memory only and never returned in result rows.
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


def decode_kryo_ascii_string(raw: bytes, start: int = 0) -> str | None:
    chars: list[int] = []
    for byte in raw[start:]:
        if byte & 0x80:
            chars.append(byte & 0x7F)
            try:
                return bytes(chars).decode("ascii")
            except UnicodeDecodeError:
                return None
        if byte < 0x20 or byte > 0x7E:
            return None
        chars.append(byte)
    return None


def captcha_candidates_from_redis_raw(raw: bytes) -> list[str]:
    candidates: list[str] = []

    def add(text: str | None) -> None:
        if text and re.fullmatch(r"[A-Za-z0-9]{4,6}", text) and text not in candidates:
            candidates.append(text)

    for payload in (raw, raw[-4:], raw[1:] if len(raw) > 1 else b""):
        if not payload:
            continue
        try:
            add(payload.decode("ascii"))
        except UnicodeDecodeError:
            pass

    for start in range(min(3, len(raw))):
        add(decode_kryo_ascii_string(raw, start))

    return candidates


def captcha_code_from_redis(captcha_key: str) -> str | None:
    # Redisson business data is configured on DB 3 in application-dev.yml.
    for db in (3, 0, 1, 2, 4, 5, 6, 7):
        try:
            raw = redis_get_raw(db, "user:captcha:" + captcha_key)
        except Exception:
            continue
        if not raw:
            continue
        for candidate in captcha_candidates_from_redis_raw(raw):
            return candidate
    return None


def generate_captcha(base_url: str) -> tuple[str | None, str | None, HttpResult]:
    http = http_request(base_url, "GET", "/captcha/generate", timeout=8)
    key = None
    if isinstance(http.json_body, dict):
        data = http.json_body.get("data") or {}
        if isinstance(data, dict):
            key = data.get("captchaKey")
    if not key:
        return None, None, http
    code = captcha_code_from_logs(key) or captcha_code_from_redis(key)
    return key, code, http


def setup_admin_token(base_url: str) -> tuple[str | None, dict[str, Any]]:
    payload = {"username": "admin", "password": "123456", "rememberMe": False}
    http = http_request(base_url, "POST", "/auth/login", body=payload, content_type="application/json", timeout=8)
    token = None
    if isinstance(http.json_body, dict):
        data = http.json_body.get("data") or {}
        if isinstance(data, dict):
            token = data.get("accessToken")
    return token, {
        "endpoint": "POST /auth/login",
        "status": http.status,
        "businessCode": result_code(http.json_body),
        "classification": classify(http),
        "tokenAcquired": bool(token),
        "message": result_message(http.json_body, http.text),
    }


def setup_user(base_url: str) -> tuple[str | None, dict[str, Any], dict[str, str]]:
    username = f"apitest{RUN_ID[-8:]}"
    email = f"{username}@example.com"
    phone = "139" + RUN_ID[-8:]
    profile = {"username": username, "email": email, "phone": phone, "password": TEST_PASSWORD}
    setup: dict[str, Any] = {"userCreatedOrExisting": False, "tokenAcquired": False}
    key, code, cap_http = generate_captcha(base_url)
    setup["captchaForRegister"] = {"status": cap_http.status, "classification": classify(cap_http), "codeAcquired": bool(code)}
    if key and code:
        payload = {
            "username": username,
            "password": TEST_PASSWORD,
            "confirmPassword": TEST_PASSWORD,
            "nickname": "API Probe",
            "email": email,
            "phone": phone,
            "captcha": code,
            "captchaKey": key,
        }
        http = http_request(base_url, "POST", "/user/auth/register", body=payload, content_type="application/json", timeout=10)
        setup["register"] = {
            "status": http.status,
            "businessCode": result_code(http.json_body),
            "classification": classify(http),
            "message": result_message(http.json_body, http.text),
        }
        if result_code(http.json_body) in {200, 603, 600}:
            setup["userCreatedOrExisting"] = True
    key, code, cap_http = generate_captcha(base_url)
    setup["captchaForLogin"] = {"status": cap_http.status, "classification": classify(cap_http), "codeAcquired": bool(code)}
    token = None
    if key and code:
        payload = {"username": username, "password": TEST_PASSWORD, "captcha": code, "captchaKey": key, "rememberMe": False}
        http = http_request(base_url, "POST", "/user/auth/login", body=payload, content_type="application/json", timeout=10)
        if isinstance(http.json_body, dict):
            data = http.json_body.get("data") or {}
            if isinstance(data, dict):
                token = data.get("accessToken")
        setup["login"] = {
            "status": http.status,
            "businessCode": result_code(http.json_body),
            "classification": classify(http),
            "tokenAcquired": bool(token),
            "message": result_message(http.json_body, http.text),
        }
    setup["tokenAcquired"] = bool(token)
    return token, setup, profile


def run_operation(
    spec: dict[str, Any],
    base_url: str,
    op: dict[str, Any],
    *,
    admin_token: str | None,
    user_token: str | None,
    user_profile: dict[str, str],
) -> dict[str, Any]:
    high_risk = is_high_risk_operation(op)
    auth_mode = choose_auth_mode(op, high_risk=high_risk)
    unsafe_id = op["method"] in {"PUT", "DELETE", "PATCH"} or high_risk
    path, query, body, content_type = build_request(spec, op, unsafe_id=unsafe_id)

    token = None
    if auth_mode == "admin":
        token = admin_token
    elif auth_mode == "user":
        token = user_token

    # Endpoint-specific success paths for core auth/captcha operations.
    if op["method"] == "POST" and op["path"] == "/auth/login":
        body = {"username": "admin", "password": "123456", "rememberMe": False}
        content_type = "application/json"
    elif op["method"] == "POST" and op["path"] == "/user/auth/login":
        key, code, _ = generate_captcha(base_url)
        if key and code:
            body = {
                "username": user_profile.get("username") or f"apitest{RUN_ID[-8:]}",
                "password": user_profile.get("password") or TEST_PASSWORD,
                "captcha": code,
                "captchaKey": key,
                "rememberMe": False,
            }
            content_type = "application/json"
    elif op["method"] == "POST" and op["path"] == "/user/auth/register":
        key, code, _ = generate_captcha(base_url)
        if key and code:
            unique = f"apireg{RUN_ID[-7:]}"
            body = {
                "username": unique,
                "password": TEST_PASSWORD,
                "confirmPassword": TEST_PASSWORD,
                "nickname": "API Probe Register",
                "email": f"{unique}@example.com",
                "phone": "138" + RUN_ID[-8:],
                "captcha": code,
                "captchaKey": key,
            }
            content_type = "application/json"
    elif op["method"] == "POST" and op["path"] == "/captcha/verify":
        key, code, _ = generate_captcha(base_url)
        if key and code:
            query = {"captchaKey": key, "captcha": code}
            body = None
            content_type = ""

    http = http_request(
        base_url,
        op["method"],
        path,
        query=query,
        body=body,
        content_type=content_type,
        token=token,
        timeout=8,
    )
    classification = classify(http)
    return {
        "method": op["method"],
        "path": op["path"],
        "operationId": op.get("operationId") or "",
        "summary": op.get("summary") or "",
        "tags": op.get("tags") or [],
        "authMode": auth_mode,
        "highRiskSafetyMode": high_risk,
        "status": http.status,
        "businessCode": result_code(http.json_body),
        "classification": classification,
        "elapsedMs": http.elapsed_ms,
        "message": result_message(http.json_body, http.text) if http.ok else truncate(http.error),
        "transportError": http.error,
        "jsonResponse": isinstance(http.json_body, dict),
        "testedAt": now_iso(),
    }


def summarize(results: list[dict[str, Any]]) -> dict[str, Any]:
    by_class: dict[str, int] = {}
    by_status: dict[str, int] = {}
    by_auth: dict[str, int] = {}
    for row in results:
        by_class[row["classification"]] = by_class.get(row["classification"], 0) + 1
        by_status[str(row.get("status"))] = by_status.get(str(row.get("status")), 0) + 1
        by_auth[row["authMode"]] = by_auth.get(row["authMode"], 0) + 1
    failures = [r for r in results if str(r["classification"]).startswith("FAIL")]
    return {
        "runId": RUN_ID,
        "generatedAt": now_iso(),
        "operationCount": len(results),
        "classificationCounts": dict(sorted(by_class.items())),
        "httpStatusCounts": dict(sorted(by_status.items())),
        "authModeCounts": dict(sorted(by_auth.items())),
        "failureCount": len(failures),
        "failedOperations": [
            {
                "method": r["method"],
                "path": r["path"],
                "operationId": r["operationId"],
                "classification": r["classification"],
                "status": r["status"],
                "businessCode": r["businessCode"],
                "message": r["message"],
            }
            for r in failures[:200]
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--spec", required=True)
    parser.add_argument("--base-url", default="http://127.0.0.1:9999/api")
    parser.add_argument("--out-dir", required=True)
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--include-file", default="", help="Optional JSON/TSV list of METHOD path operations to run")
    args = parser.parse_args()

    spec = load_json(args.spec)
    operations = extract_operations(spec)
    if args.include_file:
        include_keys: set[tuple[str, str]] = set()
        raw = open(args.include_file, "r", encoding="utf-8").read().strip()
        if raw:
            try:
                data = json.loads(raw)
                for item in data:
                    if isinstance(item, dict):
                        include_keys.add((str(item.get("method", "")).upper(), str(item.get("path", ""))))
                    elif isinstance(item, str):
                        parts = item.split(maxsplit=1)
                        if len(parts) == 2:
                            include_keys.add((parts[0].upper(), parts[1]))
            except Exception:
                for line in raw.splitlines():
                    line = line.strip()
                    if not line or line.startswith("#"):
                        continue
                    parts = line.split("\t") if "\t" in line else line.split(maxsplit=1)
                    if len(parts) >= 2:
                        include_keys.add((parts[0].upper(), parts[1]))
        operations = [op for op in operations if (op["method"], op["path"]) in include_keys]
    if args.limit:
        operations = operations[: args.limit]

    os.makedirs(args.out_dir, exist_ok=True)
    admin_token, admin_setup = setup_admin_token(args.base_url)
    user_token, user_setup, user_profile = setup_user(args.base_url)
    setup = {
        "runId": RUN_ID,
        "startedAt": now_iso(),
        "baseUrl": args.base_url,
        "admin": admin_setup,
        "user": user_setup,
        "operationCountPlanned": len(operations),
    }
    write_json(os.path.join(args.out_dir, "setup.json"), setup)

    results: list[dict[str, Any]] = []
    for idx, op in enumerate(operations, start=1):
        try:
            row = run_operation(
                spec,
                args.base_url,
                op,
                admin_token=admin_token,
                user_token=user_token,
                user_profile=user_profile,
            )
        except Exception as e:
            row = {
                "method": op["method"],
                "path": op["path"],
                "operationId": op.get("operationId") or "",
                "summary": op.get("summary") or "",
                "tags": op.get("tags") or [],
                "authMode": "unknown",
                "highRiskSafetyMode": False,
                "status": None,
                "businessCode": None,
                "classification": "FAIL_HARNESS_ERROR",
                "elapsedMs": 0,
                "message": f"{type(e).__name__}: {truncate(e)}",
                "transportError": traceback.format_exc(limit=2),
                "jsonResponse": False,
                "testedAt": now_iso(),
            }
        results.append(row)
        if idx % 50 == 0:
            print(json.dumps({"progress": idx, "total": len(operations), "latest": row["classification"]}), flush=True)
            write_json(os.path.join(args.out_dir, "results.partial.json"), results)

    summary = summarize(results)
    setup["finishedAt"] = now_iso()
    write_json(os.path.join(args.out_dir, "setup.json"), setup)
    write_json(os.path.join(args.out_dir, "results.json"), results)
    write_json(os.path.join(args.out_dir, "summary.json"), summary)
    print(json.dumps(summary, ensure_ascii=False), flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
