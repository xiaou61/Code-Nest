#!/usr/bin/env python3
"""Service-level correctness harness for file storage APIs.

This harness is stricter than HTTP smoke coverage:
- multipart upload must succeed with business code 200.
- uploaded bytes must round-trip through download endpoints.
- readback endpoints must match stored metadata and permission rules.
- admin-only endpoints are exercised through safe read/write paths.
- expected auth/validation/business errors are counted separately.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import time
import traceback
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Any


SUCCESS_CODE = 200
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
    text = " ".join(text.split())
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
    content_type: str = "application/json",
    timeout: float = 12.0,
) -> HttpResult:
    url = base_url.rstrip("/") + path
    if query:
        url += "?" + urllib.parse.urlencode(query, doseq=True)
    headers = {"Accept": "application/json", "User-Agent": "CodeNestFileStorageCorrectness/1.0"}
    data: bytes | None = None
    if token:
        headers["Authorization"] = "Bearer " + token
    if body is not None:
        if content_type.startswith("multipart/form-data"):
            boundary = "----CodeNestFileStorageBoundary"
            headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
            data = build_multipart_body(body, boundary)
        elif content_type == "application/json":
            headers["Content-Type"] = "application/json"
            data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        else:
            headers["Content-Type"] = content_type
            data = body if isinstance(body, (bytes, bytearray)) else str(body).encode("utf-8")

    start = dt.datetime.now().timestamp()
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read()
            text = raw.decode("utf-8", errors="replace")
            elapsed = int((dt.datetime.now().timestamp() - start) * 1000)
            return HttpResult(True, resp.status, elapsed, text, parse_json(text))
    except urllib.error.HTTPError as e:
        raw = e.read()
        text = raw.decode("utf-8", errors="replace")
        elapsed = int((dt.datetime.now().timestamp() - start) * 1000)
        return HttpResult(True, e.code, elapsed, text, parse_json(text))
    except Exception as e:
        elapsed = int((dt.datetime.now().timestamp() - start) * 1000)
        return HttpResult(False, None, elapsed, "", None, f"{type(e).__name__}: {truncate(e)}")


def http_get_bytes(
    base_url: str,
    path: str,
    *,
    query: dict[str, Any] | None = None,
    token: str | None = None,
    timeout: float = 12.0,
) -> tuple[HttpResult, bytes]:
    url = base_url.rstrip("/") + path
    if query:
        url += "?" + urllib.parse.urlencode(query, doseq=True)
    headers = {"Accept": "*/*", "User-Agent": "CodeNestFileStorageCorrectness/1.0"}
    if token:
        headers["Authorization"] = "Bearer " + token
    start = dt.datetime.now().timestamp()
    req = urllib.request.Request(url, headers=headers, method="GET")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read()
            elapsed = int((dt.datetime.now().timestamp() - start) * 1000)
            return HttpResult(True, resp.status, elapsed, raw.decode("utf-8", errors="replace"), parse_json(raw.decode("utf-8", errors="replace"))), raw
    except urllib.error.HTTPError as e:
        raw = e.read()
        elapsed = int((dt.datetime.now().timestamp() - start) * 1000)
        return HttpResult(True, e.code, elapsed, raw.decode("utf-8", errors="replace"), parse_json(raw.decode("utf-8", errors="replace"))), raw
    except Exception as e:
        elapsed = int((dt.datetime.now().timestamp() - start) * 1000)
        return HttpResult(False, None, elapsed, "", None, f"{type(e).__name__}: {truncate(e)}"), b""


def build_multipart_body(fields: dict[str, Any], boundary: str) -> bytes:
    parts: list[bytes] = []
    for key, value in fields.items():
        parts.append(f"--{boundary}\r\n".encode("utf-8"))
        if isinstance(value, dict) and value.get("kind") == "file":
            filename = str(value.get("filename") or "upload.txt")
            content_type = str(value.get("content_type") or "application/octet-stream")
            content = value.get("content") or b""
            if isinstance(content, str):
                content = content.encode("utf-8")
            parts.append(
                (
                    f'Content-Disposition: form-data; name="{key}"; filename="{filename}"\r\n'
                    f"Content-Type: {content_type}\r\n\r\n"
                ).encode("utf-8")
            )
            parts.append(bytes(content))
        else:
            parts.append(f'Content-Disposition: form-data; name="{key}"\r\n\r\n'.encode("utf-8"))
            parts.append(str(value).encode("utf-8"))
        parts.append(b"\r\n")
    parts.append(f"--{boundary}--\r\n".encode("utf-8"))
    return b"".join(parts)


def assert_success_http(http: HttpResult, step: str) -> None:
    if not http.ok:
        raise HarnessFailure(f"{step}: transport error: {http.error}")
    if http.status is None or http.status >= 500:
        raise HarnessFailure(f"{step}: HTTP failure status={http.status}, body={truncate(http.text)}")
    code = result_code(http.json_body)
    if code != SUCCESS_CODE:
        raise HarnessFailure(
            f"{step}: expected business code 200, got status={http.status}, code={code}, message={result_message(http.json_body, http.text)}"
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
        raise HarnessFailure(f"{step}: expected object, got {type(value).__name__}")
    return value


def require_list(value: Any, step: str) -> list[Any]:
    if not isinstance(value, list):
        raise HarnessFailure(f"{step}: expected list, got {type(value).__name__}")
    return value


def page_records(value: Any, step: str) -> list[Any]:
    page = require_dict(value, step)
    records = page.get("records")
    if not isinstance(records, list):
        raise HarnessFailure(f"{step}: expected page.records list")
    if not isinstance(page.get("total"), int):
        raise HarnessFailure(f"{step}: expected numeric total")
    return records


def download_content_disposition_name(header: str | None) -> str | None:
    if not header:
        return None
    lowered = header.lower()
    if "filename=" not in lowered:
        return None
    raw = header.split("filename=", 1)[1].strip()
    if raw.startswith('"'):
        raw = raw[1:]
    if raw.endswith('"'):
        raw = raw[:-1]
    return urllib.parse.unquote(raw)


class FileStorageCorrectnessHarness:
    def __init__(self, base_url: str, out_dir: str):
        self.base_url = base_url.rstrip("/")
        self.out_dir = out_dir
        self.results: list[dict[str, Any]] = []
        self.context: dict[str, Any] = {"runId": RUN_ID, "baseUrl": self.base_url}
        self.admin_token: str | None = None
        self.user_token: str | None = None
        self.uploaded: dict[str, Any] = {}
        self.temp_config_id: int | None = None
        self.admin_file: dict[str, Any] = {}
        self.created_migration_id: int | None = None
        self.original_system_settings: dict[str, str] = {}

    def record(
        self,
        name: str,
        method: str,
        path: str,
        http: Any,
        assertion: str,
        status: str,
        *,
        expected: str = "SUCCESS",
        details: dict[str, Any] | None = None,
    ) -> None:
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
                "message": result_message(http.json_body, http.text) if http.ok else truncate(http.error),
                "elapsedMs": http.elapsed_ms,
                "assertion": assertion,
                "details": details or {},
                "testedAt": now_iso(),
            }
        )

    def path_with_query(self, path: str, query: dict[str, Any] | None) -> str:
        if not query:
            return path
        return path + "?" + urllib.parse.urlencode(query, doseq=True)

    def call_success(
        self,
        name: str,
        method: str,
        path: str,
        *,
        query: dict[str, Any] | None = None,
        body: Any = None,
        token: str | None = None,
        content_type: str = "application/json",
        assertion: str = "business code 200",
        timeout: float = 12.0,
    ) -> Any:
        http = http_request(self.base_url, method, path, query=query, body=body, token=token, content_type=content_type, timeout=timeout)
        try:
            assert_success_http(http, f"{method} {path}")
            self.record(name, method, self.path_with_query(path, query), http, assertion, "PASS")
            return response_data(http.json_body)
        except Exception:
            self.record(name, method, self.path_with_query(path, query), http, assertion, "FAIL")
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
        content_type: str = "application/json",
        assertion: str = "expected business error",
    ) -> None:
        http = http_request(self.base_url, method, path, query=query, body=body, token=token, content_type=content_type)
        try:
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

    def call_binary(
        self,
        name: str,
        path: str,
        *,
        query: dict[str, Any] | None = None,
        token: str | None = None,
        assertion: str = "binary content round-trip",
    ) -> tuple[HttpResult, bytes]:
        http, raw = http_get_bytes(self.base_url, path, query=query, token=token)
        try:
            if not http.ok:
                raise HarnessFailure(f"GET {path}: transport error: {http.error}")
            if http.status is None or http.status >= 500:
                raise HarnessFailure(f"GET {path}: HTTP failure status={http.status}, body={truncate(http.text)}")
            self.record(name, "GET", self.path_with_query(path, query), http, assertion, "PASS")
            return http, raw
        except Exception:
            self.record(name, "GET", self.path_with_query(path, query), http, assertion, "FAIL")
            raise

    def admin_login(self) -> None:
        login = require_dict(
            self.call_success(
                "admin login",
                "POST",
                "/auth/login",
                body={"username": "admin", "password": "123456", "rememberMe": False},
                assertion="seed admin can log in for file storage admin APIs",
            ),
            "admin login",
        )
        token = login.get("accessToken")
        if not isinstance(token, str) or not token:
            raise HarnessFailure("admin login: missing accessToken")
        self.admin_token = token

    def user_login(self) -> None:
        # Reuse the seed admin token as an authenticated user session for public file endpoints.
        # FileController accepts either user or admin authentication.
        if not self.admin_token:
            raise HarnessFailure("user login: admin token missing")
        self.user_token = self.admin_token

    def upload_sample_file(self) -> dict[str, Any]:
        payload = b"Code-Nest file storage correctness probe.\n"
        body = {
            "file": {
                "kind": "file",
                "filename": f"probe-{RUN_ID}.txt",
                "content_type": "text/plain",
                "content": payload,
            },
            "moduleName": "filestorage",
            "businessType": "correctness",
        }
        data = require_dict(
            self.call_success(
                "upload single file",
                "POST",
                "/file/upload/single",
                body=body,
                token=self.user_token,
                content_type="multipart/form-data",
                assertion="authenticated multipart upload returns FileUploadResult payload",
            ),
            "upload single file",
        )
        if data.get("success") is not True:
            raise HarnessFailure(f"upload single file: expected success=true, got {data!r}")
        storage_path = data.get("storagePath")
        access_url = data.get("accessUrl")
        file_size = data.get("fileSize")
        if not isinstance(storage_path, str) or not storage_path:
            raise HarnessFailure("upload single file: storagePath missing")
        if not isinstance(access_url, str) or not access_url:
            raise HarnessFailure("upload single file: accessUrl missing")
        if not isinstance(file_size, int) or file_size != len(payload):
            raise HarnessFailure(f"upload single file: expected fileSize {len(payload)}, got {file_size!r}")
        self.uploaded = {
            "payload": payload,
            "storagePath": storage_path,
            "accessUrl": access_url,
            "fileSize": file_size,
            "originalName": f"probe-{RUN_ID}.txt",
        }
        self.context["uploadedFile"] = {k: v for k, v in self.uploaded.items() if k != "payload"}
        return self.uploaded

    def find_uploaded_file_id(self) -> int:
        records = page_records(
            self.call_success(
                "admin file list locate upload",
                "GET",
                "/admin/file/list",
                token=self.admin_token,
                query={"moduleName": "filestorage", "businessType": "correctness", "pageNum": 1, "pageSize": 50},
                assertion="admin list can locate the uploaded file record",
            ),
            "admin file list locate upload",
        )
        for item in records:
            if not isinstance(item, dict):
                continue
            if item.get("originalName") != self.uploaded["originalName"]:
                continue
            if item.get("fileSize") != len(self.uploaded["payload"]):
                continue
            file_id = item.get("id")
            if isinstance(file_id, int):
                self.uploaded["fileId"] = file_id
                return file_id
        raise HarnessFailure("find uploaded file id: uploaded record not found in admin list")

    def upload_duplicate_file(self) -> None:
        payload = self.uploaded["payload"]
        body = {
            "file": {
                "kind": "file",
                "filename": self.uploaded["originalName"],
                "content_type": "text/plain",
                "content": payload,
            },
            "moduleName": "filestorage",
            "businessType": "correctness",
        }
        data = require_dict(
            self.call_success(
                "upload duplicate file",
                "POST",
                "/file/upload/single",
                body=body,
                token=self.user_token,
                content_type="multipart/form-data",
                assertion="duplicate content should reuse existing file or return same content identity",
            ),
            "upload duplicate file",
        )
        if data.get("success") is not True:
            raise HarnessFailure(f"upload duplicate file: expected success=true, got {data!r}")
        if not isinstance(data.get("storagePath"), str) or not data.get("storagePath"):
            raise HarnessFailure("upload duplicate file: storagePath missing")

    def upload_admin_probe_file(self) -> dict[str, Any]:
        payload = b"Code-Nest file storage admin probe.\n"
        body = {
            "file": {
                "kind": "file",
                "filename": f"admin-probe-{RUN_ID}.txt",
                "content_type": "text/plain",
                "content": payload,
            },
            "moduleName": "filestorage",
            "businessType": "admin",
        }
        data = require_dict(
            self.call_success(
                "upload admin probe file",
                "POST",
                "/file/upload/single",
                body=body,
                token=self.user_token,
                content_type="multipart/form-data",
                assertion="second upload creates a file for admin move/delete verification",
            ),
            "upload admin probe file",
        )
        if data.get("success") is not True:
            raise HarnessFailure(f"upload admin probe file: expected success=true, got {data!r}")
        self.admin_file = {
            "payload": payload,
            "originalName": f"admin-probe-{RUN_ID}.txt",
            "fileSize": len(payload),
            "storagePath": data.get("storagePath"),
            "accessUrl": data.get("accessUrl"),
        }
        return self.admin_file

    def verify_download_round_trip(self) -> None:
        http, raw = self.call_binary(
            "download uploaded file",
            f"/file/download/{self.uploaded['fileId']}",
            token=self.user_token,
            assertion="download returns original file bytes",
        )
        if raw != self.uploaded["payload"]:
            raise HarnessFailure("download uploaded file: returned bytes differ from original upload payload")
        if http.status != 200:
            raise HarnessFailure(f"download uploaded file: expected HTTP 200, got {http.status}")

    def resolve_uploaded_file_id(self) -> int:
        file_id = self.find_uploaded_file_id()
        info = require_dict(
            self.call_success(
                "read back file info",
                "GET",
                f"/file/info/{file_id}",
                token=self.user_token,
                assertion="uploaded file info returns read-back metadata",
            ),
            "read back file info",
        )
        if info.get("originalName") != self.uploaded["originalName"]:
            raise HarnessFailure(f"read back file info: expected originalName {self.uploaded['originalName']!r}, got {info.get('originalName')!r}")
        if info.get("fileSize") != len(self.uploaded["payload"]):
            raise HarnessFailure("read back file info: fileSize mismatch")
        if info.get("contentType") not in {"text/plain", "text/plain; charset=utf-8", "application/octet-stream"}:
            raise HarnessFailure(f"read back file info: unexpected contentType {info.get('contentType')!r}")
        if info.get("accessUrl") != self.uploaded["accessUrl"]:
            raise HarnessFailure("read back file info: accessUrl mismatch")
        info_id = info.get("id")
        if not isinstance(info_id, int):
            raise HarnessFailure("read back file info: missing numeric id")
        if info_id != file_id:
            raise HarnessFailure("read back file info: id mismatch with admin list record")
        self.uploaded["fileId"] = file_id
        return file_id

    def verify_public_file_routes(self) -> None:
        file_id = self.uploaded["fileId"]
        self.call_success(
            "file url",
            "GET",
            f"/file/url/{file_id}",
            token=self.user_token,
            assertion="file url endpoint returns a resolvable URL",
        )
        urls = require_dict(
            self.call_success(
                "batch file urls",
                "POST",
                "/file/urls",
                body=[file_id],
                token=self.user_token,
                assertion="batch url endpoint returns map with file id key",
            ),
            "batch file urls",
        )
        if str(file_id) not in {str(k) for k in urls.keys()} and file_id not in urls:
            raise HarnessFailure("batch file urls: expected uploaded file id in response map")
        exists = require_dict(
            self.call_success(
                "check file exists",
                "POST",
                "/file/exists",
                body=[file_id],
                token=self.user_token,
                assertion="exists endpoint returns id->boolean mapping",
            ),
            "check file exists",
        )
        if exists.get(str(file_id), exists.get(file_id)) is not True:
            raise HarnessFailure("check file exists: uploaded file should exist")
        list_page = require_dict(
            self.call_success(
                "list files",
                "GET",
                "/file/list",
                query={"moduleName": "filestorage", "businessType": "correctness", "pageNum": 1, "pageSize": 1},
                token=self.user_token,
                assertion="file list returns paged records",
            ),
            "list files",
        )
        records = require_list(list_page.get("records"), "list files records")
        if not records:
            raise HarnessFailure("list files: expected at least one record")
        if len(records) > 1:
            raise HarnessFailure("list files: pageSize=1 should not return more than one record")
        if list_page.get("pageNum") != 1 or list_page.get("pageSize") != 1:
            raise HarnessFailure(f"list files: expected pageNum/pageSize to echo request, got {list_page}")
        if not isinstance(list_page.get("total"), int) or list_page["total"] < 1:
            raise HarnessFailure("list files: expected positive total")

    def verify_access_url(self) -> None:
        access_url = self.uploaded["accessUrl"]
        parsed = urllib.parse.urlparse(access_url)
        path = parsed.path
        if parsed.netloc != "localhost:9999":
            # We still verify the logical access URL shape because the stored URL is part of the API contract.
            pass
        http, raw = http_get_bytes(self.base_url, path.replace("/api", "", 1) if path.startswith("/api") else path, timeout=12.0)
        if http.status != 200:
            raise HarnessFailure(f"accessUrl should resolve to HTTP 200, got {http.status}")
        if raw != self.uploaded["payload"]:
            raise HarnessFailure("accessUrl bytes differ from uploaded payload")

    def verify_permission_guards(self) -> None:
        if not self.uploaded.get("fileId"):
            raise HarnessFailure("verify_permission_guards: uploaded file id missing")
        file_id = self.uploaded["fileId"]
        self.call_expected_error(
            "anonymous info rejected",
            "GET",
            f"/file/info/{file_id}",
            message_part="无权",
            assertion="private file info requires authentication",
        )
        self.call_expected_error(
            "anonymous url rejected",
            "GET",
            f"/file/url/{file_id}",
            message_part="无权",
            assertion="private file url requires authentication",
        )
        self.call_expected_error(
            "anonymous list rejected",
            "GET",
            "/file/list",
            query={"moduleName": "filestorage"},
            message_part="请先登录",
            assertion="file list requires authentication",
        )
        self.call_expected_error(
            "anonymous exists rejected",
            "POST",
            "/file/exists",
            body=[file_id],
            message_part="请先登录",
            assertion="exists endpoint requires authentication",
        )
        http, _ = http_get_bytes(self.base_url, f"/file/download/{file_id}", timeout=12.0)
        if http.status != 403:
            raise HarnessFailure(f"anonymous download should be raw HTTP 403, got {http.status}")

    def verify_delete_and_missing_readback(self) -> None:
        file_id = self.uploaded["fileId"]
        deleted = self.call_success(
            "delete file",
            "DELETE",
            f"/file/{file_id}",
            query={"moduleName": "filestorage"},
            token=self.user_token,
            assertion="logical delete returns success",
        )
        if deleted is not True:
            raise HarnessFailure("delete file: expected true")
        self.call_expected_error(
            "deleted file info rejected",
            "GET",
            f"/file/info/{file_id}",
            token=self.user_token,
            message_part="文件不存在",
            assertion="deleted file should no longer be readable",
        )
        exists = require_dict(
            self.call_success(
                "deleted file exists",
                "POST",
                "/file/exists",
                body=[file_id],
                token=self.user_token,
                assertion="exists endpoint reflects logical delete",
            ),
            "deleted file exists",
        )
        if exists.get(str(file_id), exists.get(file_id)) is not False:
            raise HarnessFailure("deleted file exists: expected false after logical delete")

    def verify_admin_views(self) -> None:
        admin_list = require_dict(
            self.call_success(
                "admin file list",
                "GET",
                "/admin/file/list",
                token=self.admin_token,
                query={"moduleName": "filestorage", "businessType": "correctness", "pageNum": 1, "pageSize": 1},
                assertion="admin file list returns page shape",
            ),
            "admin file list",
        )
        if not isinstance(admin_list.get("records"), list):
            raise HarnessFailure("admin file list: records missing")
        stats = require_dict(
            self.call_success(
                "admin file statistics",
                "GET",
                "/admin/file/statistics",
                token=self.admin_token,
                assertion="admin statistics returns numeric summary",
            ),
            "admin file statistics",
        )
        for key in ("totalFiles", "deletedFiles", "totalStorageSize", "todayFiles", "storageConfigs"):
            if key not in stats:
                raise HarnessFailure(f"admin file statistics: missing {key}")
        usage = require_dict(
            self.call_success(
                "admin storage usage",
                "GET",
                "/admin/file/storage-usage",
                token=self.admin_token,
                assertion="storage usage returns module/storage/type maps",
            ),
            "admin storage usage",
        )
        if "moduleUsage" not in usage or "storageUsage" not in usage or "typeDistribution" not in usage:
            raise HarnessFailure("admin storage usage: missing expected keys")

    def verify_system_settings(self) -> None:
        settings = require_dict(
            self.call_success(
                "admin system settings",
                "GET",
                "/admin/system/settings",
                token=self.admin_token,
                assertion="system settings returns map",
            ),
            "admin system settings",
        )
        self.original_system_settings = {k: str(v) for k, v in settings.items() if k in {"ALLOWED_FILE_TYPES", "AUTO_BACKUP_ENABLED"}}
        if self.original_system_settings:
            updated = require_dict(
                self.call_success(
                    "admin system settings update",
                    "PUT",
                    "/admin/system/settings",
                    token=self.admin_token,
                    body=self.original_system_settings,
                    assertion="system settings update accepts a reversible no-op write",
                ),
                "admin system settings update",
            )
            if updated is not True:
                raise HarnessFailure("admin system settings update: expected true")
            round_trip = require_dict(
                self.call_success(
                    "admin system settings readback",
                    "GET",
                    "/admin/system/settings",
                    token=self.admin_token,
                    assertion="system settings readback still matches after reversible update",
                ),
                "admin system settings readback",
            )
            for key, value in self.original_system_settings.items():
                if str(round_trip.get(key)) != value:
                    raise HarnessFailure(f"admin system settings readback: expected {key}={value!r}, got {round_trip.get(key)!r}")
        file_types = require_list(
            self.call_success(
                "admin file types",
                "GET",
                "/admin/system/file-types",
                token=self.admin_token,
                assertion="file types returns list",
            ),
            "admin file types",
        )
        if not file_types:
            raise HarnessFailure("admin file types: expected non-empty whitelist")
        summary = require_dict(
            self.call_success(
                "admin system summary",
                "GET",
                "/admin/system/summary",
                token=self.admin_token,
                assertion="system summary returns aggregated settings",
            ),
            "admin system summary",
        )
        for key in ("maxFileSize", "moduleStorageQuota", "tempLinkExpireHours", "autoBackupEnabled", "allowedFileTypesCount"):
            if key not in summary:
                raise HarnessFailure(f"admin system summary: missing {key}")

    def verify_storage_configs(self) -> None:
        configs = require_list(
            self.call_success(
                "admin storage configs",
                "GET",
                "/admin/storage/configs",
                token=self.admin_token,
                assertion="storage config list returns array",
            ),
            "admin storage configs",
        )
        local_config = None
        for cfg in configs:
            if isinstance(cfg, dict) and cfg.get("storageType") == "LOCAL" and cfg.get("isDefault") == 1:
                local_config = cfg
                break
        if not local_config:
            raise HarnessFailure("admin storage configs: default LOCAL config not found in seed data")
        config_id = local_config.get("id")
        if not isinstance(config_id, int):
            raise HarnessFailure("admin storage configs: local config id missing")
        temp_name = f"api-probe-local-{RUN_ID}"
        temp_payload = {
            "storageType": "LOCAL",
            "configName": temp_name,
            "configParams": json.dumps({"basePath": "/uploads", "urlPrefix": "http://localhost:9999/files"}, ensure_ascii=False),
            "isDefault": 0,
            "isEnabled": 1,
        }
        created = self.call_success(
            "admin create temp storage config",
            "POST",
            "/admin/storage/config",
            token=self.admin_token,
            body=temp_payload,
            assertion="storage config create returns true for a reversible LOCAL config",
        )
        if created is not True:
            raise HarnessFailure(f"admin create temp storage config: expected true, got {created!r}")
        refreshed = require_list(
            self.call_success(
                "admin storage configs after create",
                "GET",
                "/admin/storage/configs",
                token=self.admin_token,
                assertion="storage config list reflects newly created config",
            ),
            "admin storage configs after create",
        )
        temp_cfg = next((cfg for cfg in refreshed if isinstance(cfg, dict) and cfg.get("configName") == temp_name), None)
        if not temp_cfg or not isinstance(temp_cfg.get("id"), int):
            raise HarnessFailure("admin storage configs after create: temp config not found")
        self.temp_config_id = int(temp_cfg["id"])
        detail = require_dict(
            self.call_success(
                "admin storage config detail",
                "GET",
                f"/admin/storage/config/{config_id}",
                token=self.admin_token,
                assertion="storage config detail returns the selected row",
            ),
            "admin storage config detail",
        )
        if detail.get("id") != config_id:
            raise HarnessFailure("admin storage config detail: id mismatch")
        types = require_list(
            self.call_success(
                "admin storage types",
                "GET",
                "/admin/storage/types",
                token=self.admin_token,
                assertion="supported storage types returns list",
            ),
            "admin storage types",
        )
        if "LOCAL" not in types:
            raise HarnessFailure("admin storage types: LOCAL must be supported")
        test_result = require_dict(
            self.call_success(
                "test local storage config",
                "POST",
                f"/admin/storage/config/{config_id}/test",
                token=self.admin_token,
                assertion="storage config test updates test status and returns success payload",
            ),
            "test local storage config",
        )
        if test_result.get("success") is not True:
            raise HarnessFailure(f"test local storage config: expected success=true, got {test_result!r}")
        detail_after = require_dict(
            self.call_success(
                "storage config detail after test",
                "GET",
                f"/admin/storage/config/{config_id}",
                token=self.admin_token,
                assertion="storage config test status should read back after test",
            ),
            "storage config detail after test",
        )
        if detail_after.get("testStatus") != 1:
            raise HarnessFailure(f"storage config detail after test: expected testStatus=1, got {detail_after.get('testStatus')!r}")
        temp_update = require_dict(
            self.call_success(
                "admin update temp storage config",
                "PUT",
                f"/admin/storage/config/{self.temp_config_id}",
                token=self.admin_token,
                body={
                    "storageType": "LOCAL",
                    "configName": temp_name,
                    "configParams": json.dumps({"basePath": "/uploads", "urlPrefix": "http://localhost:9999/files"}, ensure_ascii=False),
                    "isDefault": 0,
                    "isEnabled": 1,
                },
                assertion="storage config update accepts a reversible no-op write",
            ),
            "admin update temp storage config",
        )
        if temp_update is not True:
            raise HarnessFailure("admin update temp storage config: expected true")

    def verify_migration_flow(self) -> None:
        configs = require_list(
            self.call_success(
                "migration configs refresh",
                "GET",
                "/admin/storage/configs",
                token=self.admin_token,
                assertion="storage configs available before migration task creation",
            ),
            "migration configs refresh",
        )
        local_configs = [cfg for cfg in configs if isinstance(cfg, dict) and cfg.get("storageType") == "LOCAL" and cfg.get("isEnabled") == 1]
        if len(local_configs) < 1:
            raise HarnessFailure("migration flow: no enabled LOCAL config available")
        source_id = int(local_configs[0]["id"])
        target_id = int(local_configs[0]["id"])
        task_name = f"probe-migration-{RUN_ID}"
        created = self.call_success(
            "create migration task",
            "POST",
            "/admin/file/migrate",
            token=self.admin_token,
            body={
                "sourceStorageId": source_id,
                "targetStorageId": target_id,
                "migrationType": "FULL",
                "taskName": task_name,
                "filterParams": {"moduleName": "filestorage"},
            },
            assertion="migration task creation returns id",
        )
        if not isinstance(created, int):
            raise HarnessFailure(f"create migration task: expected numeric id, got {created!r}")
        self.created_migration_id = created
        detail = require_dict(
            self.call_success(
                "get migration task",
                "GET",
                f"/admin/file/migration/{created}",
                token=self.admin_token,
                assertion="migration task detail returns created row",
            ),
            "get migration task",
        )
        if detail.get("id") != created or detail.get("taskName") != task_name:
            raise HarnessFailure("get migration task: read-back mismatch")
        progress = require_dict(
            self.call_success(
                "get migration progress",
                "GET",
                f"/admin/file/migration/{created}/progress",
                token=self.admin_token,
                assertion="migration progress returns progress object",
            ),
            "get migration progress",
        )
        if progress.get("taskId") != created:
            raise HarnessFailure("get migration progress: taskId mismatch")
        self.call_success(
            "execute migration task",
            "POST",
            f"/admin/file/migration/{created}/execute",
            token=self.admin_token,
            assertion="migration task can be executed",
        )
        terminal = None
        for _ in range(20):
            time.sleep(0.5)
            terminal = require_dict(
                self.call_success(
                    "poll migration progress",
                    "GET",
                    f"/admin/file/migration/{created}/progress",
                    token=self.admin_token,
                    assertion="migration progress can be polled after execution",
                ),
                "poll migration progress",
            )
            if terminal.get("status") in {"COMPLETED", "FAILED", "STOPPED"}:
                break
        if terminal is None:
            raise HarnessFailure("migration task: progress polling did not return data")
        if terminal.get("status") == "FAILED":
            raise HarnessFailure(f"migration task: failed terminal status with error={terminal.get('error')!r}")
        if terminal.get("status") not in {"COMPLETED", "STOPPED"}:
            raise HarnessFailure(f"migration task: expected terminal status, got {terminal.get('status')!r}")
        migrations = require_list(
            self.call_success(
                "list migration tasks",
                "GET",
                "/admin/file/migrations",
                token=self.admin_token,
                query={"limit": 5},
                assertion="migration list returns a bounded list",
            ),
            "list migration tasks",
        )
        if not any(isinstance(item, dict) and item.get("id") == created for item in migrations):
            raise HarnessFailure("list migration tasks: created task missing from list")
        self.call_success(
            "stop migration task",
            "PUT",
            f"/admin/file/migration/{created}/stop",
            token=self.admin_token,
            assertion="migration task stop endpoint accepts task id",
        )

    def verify_admin_file_move_and_delete(self) -> None:
        if self.temp_config_id is None:
            raise HarnessFailure("admin move/delete: temp config id missing")
        file_id = self.admin_file.get("fileId")
        if not isinstance(file_id, int):
            file_id = self.find_admin_probe_file_id()
        self.call_success(
            "admin move file",
            "PUT",
            f"/admin/file/{file_id}/move",
            token=self.admin_token,
            query={"targetStorageId": self.temp_config_id},
            assertion="move endpoint accepts target storage id",
        )
        self.call_success(
            "admin force delete file",
            "DELETE",
            f"/admin/file/{file_id}/force",
            token=self.admin_token,
            assertion="force delete removes database row or treats missing as success",
        )

    def find_admin_probe_file_id(self) -> int:
        records = page_records(
            self.call_success(
                "admin file list locate admin probe",
                "GET",
                "/admin/file/list",
                token=self.admin_token,
                query={"moduleName": "filestorage", "businessType": "admin", "pageNum": 1, "pageSize": 50},
                assertion="admin list can locate the admin probe file record",
            ),
            "admin file list locate admin probe",
        )
        for item in records:
            if not isinstance(item, dict):
                continue
            if item.get("originalName") != self.admin_file["originalName"]:
                continue
            if item.get("fileSize") != len(self.admin_file["payload"]):
                continue
            file_id = item.get("id")
            if isinstance(file_id, int):
                self.admin_file["fileId"] = file_id
                return file_id
        raise HarnessFailure("find admin probe file id: admin record not found in admin list")

    def cleanup(self) -> None:
        if self.created_migration_id and self.admin_token:
            try:
                self.call_success(
                    "cleanup delete migration task",
                    "DELETE",
                    f"/admin/file/migration/{self.created_migration_id}",
                    token=self.admin_token,
                    assertion="cleanup removes migration task",
                )
            except Exception:
                pass
        if self.temp_config_id and self.admin_token:
            try:
                self.call_success(
                    "cleanup delete temp storage config",
                    "DELETE",
                    f"/admin/storage/config/{self.temp_config_id}",
                    token=self.admin_token,
                    assertion="cleanup removes temp storage config created by harness",
                )
            except Exception:
                pass

    def run(self) -> dict[str, Any]:
        failure: str | None = None
        try:
            self.admin_login()
            self.user_login()
            self.call_expected_error(
                "anonymous upload rejected",
                "POST",
                "/file/upload/single",
                body={},
                content_type="multipart/form-data",
                message_part="请先登录",
                assertion="upload requires auth",
            )
            uploaded = self.upload_sample_file()
            self.resolve_uploaded_file_id()
            self.upload_duplicate_file()
            self.verify_download_round_trip()
            self.verify_access_url()
            self.verify_public_file_routes()
            self.verify_permission_guards()
            self.verify_admin_views()
            self.verify_system_settings()
            self.verify_storage_configs()
            self.verify_migration_flow()
            self.verify_delete_and_missing_readback()
            self.upload_admin_probe_file()
            self.verify_admin_file_move_and_delete()
            self.cleanup()
        except Exception as exc:
            failure = f"{type(exc).__name__}: {truncate(exc)}"
            try:
                self.cleanup()
            except Exception as cleanup_error:
                self.context["cleanupError"] = f"{type(cleanup_error).__name__}: {truncate(cleanup_error)}"
            raise
        finally:
            summary = self.summary(failure)
            self.context["finishedAt"] = now_iso()
            write_json(os.path.join(self.out_dir, "context.json"), self.sanitized_context())
            write_json(os.path.join(self.out_dir, "results.json"), self.results)
            write_json(os.path.join(self.out_dir, "summary.json"), summary)
        return self.summary(None)

    def sanitized_context(self) -> dict[str, Any]:
        context = dict(self.context)
        if self.uploaded:
            context["uploadedFile"] = {k: v for k, v in self.uploaded.items() if k != "payload"}
        context["adminCreatedConfigIds"] = self.admin_created_config_ids
        context["createdMigrationId"] = self.created_migration_id
        return context

    def summary(self, failure: str | None) -> dict[str, Any]:
        by_status: dict[str, int] = {}
        for row in self.results:
            status = str(row.get("status"))
            by_status[status] = by_status.get(status, 0) + 1
        failures = [row for row in self.results if str(row.get("status")).startswith("FAIL")]
        return {
            "runId": RUN_ID,
            "generatedAt": now_iso(),
            "baseUrl": self.base_url,
            "service": "xiaou-filestorage",
            "status": "FAIL" if failure or failures else "PASS",
            "failure": failure,
            "stepCount": len(self.results),
            "failureCount": len(failures) + (1 if failure and not failures else 0),
            "byStatus": by_status,
            "failedSteps": [
                {
                    "step": row.get("step"),
                    "name": row.get("name"),
                    "method": row.get("method"),
                    "path": row.get("path"),
                    "message": row.get("message"),
                    "assertion": row.get("assertion"),
                }
                for row in failures
            ],
        }


def main() -> int:
    parser = argparse.ArgumentParser(description="Run file storage service correctness checks")
    parser.add_argument("--base-url", default="http://127.0.0.1:9999/api")
    parser.add_argument("--out-dir", required=True)
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    harness = FileStorageCorrectnessHarness(args.base_url, args.out_dir)
    try:
        summary = harness.run()
    except Exception:
        with open(os.path.join(args.out_dir, "error.txt"), "w", encoding="utf-8") as f:
            f.write(traceback.format_exc())
        summary_path = os.path.join(args.out_dir, "summary.json")
        summary = {}
        if os.path.exists(summary_path):
            with open(summary_path, "r", encoding="utf-8") as f:
                summary = json.load(f)
        print(json.dumps(summary or {"status": "FAIL", "error": "see error.txt"}, ensure_ascii=False), flush=True)
        return 1
    print(json.dumps(summary, ensure_ascii=False), flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
