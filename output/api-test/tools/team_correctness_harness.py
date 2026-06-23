#!/usr/bin/env python3
"""Service-level correctness harness for the user-team module.

This harness is deliberately stricter than the OpenAPI smoke harness:
- HTTP 2xx alone is not success.
- business code must be 200 for success-path operations.
- created/updated state must be read back and asserted.
- expected business errors are asserted by message/code and kept separate
  from implementation defects.

Tokens and captcha values are used in memory only and are not written to disk.
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
from typing import Any, Callable


SUCCESS_CODE = 200
TEST_PASSWORD = "ApiTest123456"
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
    timeout: float = 10.0,
) -> HttpResult:
    url = base_url.rstrip("/") + path
    if query:
        url += "?" + urllib.parse.urlencode(query, doseq=True)
    headers = {"Accept": "application/json", "User-Agent": "CodeNestTeamCorrectness/1.0"}
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


def decode_kryo_ascii_string(raw: bytes, start: int = 0) -> str | None:
    """Decode Kryo's compact ASCII string form from a raw Redisson value.

    Redisson's default codec stores small Java strings as ASCII bytes with the
    high bit set on the final character. The object header appears before the
    string payload, so callers try a few candidate offsets.
    """
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


def assert_business_error(http: HttpResult, step: str, message_part: str) -> None:
    if not http.ok:
        raise HarnessFailure(f"{step}: transport error during expected business error: {http.error}")
    if http.status is None or http.status >= 500:
        raise HarnessFailure(f"{step}: expected business error, got HTTP status={http.status}")
    code = result_code(http.json_body)
    message = result_message(http.json_body, http.text)
    if code == SUCCESS_CODE:
        raise HarnessFailure(f"{step}: expected business error but got success")
    if message_part not in message:
        raise HarnessFailure(f"{step}: expected message containing {message_part!r}, got {message!r}")


def require_dict(value: Any, step: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise HarnessFailure(f"{step}: expected response data object, got {type(value).__name__}")
    return value


def require_list(value: Any, step: str) -> list[Any]:
    if not isinstance(value, list):
        raise HarnessFailure(f"{step}: expected response data list, got {type(value).__name__}")
    return value


def find_by(items: list[Any], key: str, expected: Any) -> dict[str, Any] | None:
    for item in items:
        if isinstance(item, dict) and item.get(key) == expected:
            return item
    return None


class TeamCorrectnessHarness:
    def __init__(self, base_url: str, out_dir: str):
        self.base_url = base_url.rstrip("/")
        self.out_dir = out_dir
        self.results: list[dict[str, Any]] = []
        self.context: dict[str, Any] = {"runId": RUN_ID, "baseUrl": self.base_url}
        self.leader: dict[str, Any] = {}
        self.member: dict[str, Any] = {}
        self.created_ids: dict[str, Any] = {}

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
    ) -> Any:
        http = http_request(self.base_url, method, path, query=query, body=body, token=token)
        try:
            assert_success_http(http, f"{method} {path}")
            data = response_data(http.json_body)
            self.record(name, method, self.path_with_query(path, query), http, assertion, "PASS")
            return data
        except Exception as e:
            self.record(name, method, self.path_with_query(path, query), http, assertion, "FAIL")
            raise

    def call_expected_error(
        self,
        name: str,
        method: str,
        path: str,
        *,
        message_part: str,
        query: dict[str, Any] | None = None,
        body: Any = None,
        token: str | None = None,
        assertion: str = "expected business error",
    ) -> None:
        http = http_request(self.base_url, method, path, query=query, body=body, token=token)
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

    @staticmethod
    def path_with_query(path: str, query: dict[str, Any] | None) -> str:
        if not query:
            return path
        return path + "?" + urllib.parse.urlencode(query, doseq=True)

    def register_and_login_user(self, suffix: str) -> dict[str, Any]:
        username = f"team{suffix}{RUN_ID[-8:]}"
        email = f"{username}@example.com"
        phone_prefix = "136" if suffix == "lead" else "137"
        phone = phone_prefix + RUN_ID[-8:]

        key, code = generate_captcha(self.base_url)
        register_body = {
            "username": username,
            "password": TEST_PASSWORD,
            "confirmPassword": TEST_PASSWORD,
            "nickname": f"Team {suffix} {RUN_ID[-4:]}",
            "email": email,
            "phone": phone,
            "captcha": code,
            "captchaKey": key,
        }
        register_data = self.call_success(
            f"register {suffix} user",
            "POST",
            "/user/auth/register",
            body=register_body,
            assertion="new user can be registered for isolated team flow",
        )
        registered = require_dict(register_data, f"register {suffix} user")
        user_id = registered.get("id")
        if not isinstance(user_id, int):
            raise HarnessFailure(f"register {suffix} user: response missing numeric user id")

        key, code = generate_captcha(self.base_url)
        login_body = {
            "username": username,
            "password": TEST_PASSWORD,
            "captcha": code,
            "captchaKey": key,
            "rememberMe": False,
        }
        login_data = self.call_success(
            f"login {suffix} user",
            "POST",
            "/user/auth/login",
            body=login_body,
            assertion="newly registered user can obtain access token",
        )
        login = require_dict(login_data, f"login {suffix} user")
        token = login.get("accessToken")
        if not isinstance(token, str) or not token:
            raise HarnessFailure(f"login {suffix} user: response missing accessToken")
        return {
            "id": user_id,
            "username": username,
            "email": email,
            "phone": phone,
            "token": token,
        }

    def team_body(self, team_name: str, *, join_type: int = 2) -> dict[str, Any]:
        return {
            "teamName": team_name,
            "teamDesc": f"service correctness probe {RUN_ID}",
            "teamType": 1,
            "tags": "api,correctness",
            "maxMembers": 10,
            "joinType": join_type,
            "goalTitle": "Correctness Goal",
            "goalDesc": "verify team flow",
            "goalStartDate": "2026-06-06",
            "goalEndDate": "2026-12-31",
            "dailyTarget": 1,
        }

    def task_body(self, task_name: str, *, status_probe: bool = False) -> dict[str, Any]:
        return {
            "taskName": task_name,
            "taskDesc": "verify task state" if status_probe else "verify task",
            "taskType": 4,
            "targetValue": 1,
            "targetUnit": "times",
            "repeatType": 1,
            "requireContent": 0,
            "requireImage": 0,
            "startDate": "2026-06-06",
            "endDate": "2026-12-31",
        }

    def discussion_body(self, title: str, *, content: str, is_top: int = 0, is_essence: int = 0) -> dict[str, Any]:
        return {
            "category": 5,
            "title": title,
            "content": content,
            "images": [],
            "isTop": is_top,
            "isEssence": is_essence,
        }

    def run(self) -> dict[str, Any]:
        failure: str | None = None
        try:
            self.leader = self.register_and_login_user("lead")
            self.member = self.register_and_login_user("mem")
            self.context["leaderUserId"] = self.leader["id"]
            self.context["memberUserId"] = self.member["id"]

            leader_token = self.leader["token"]
            member_token = self.member["token"]
            team_name = f"API Correct Team {RUN_ID[-6:]}"

            team_data = self.call_success(
                "create team",
                "POST",
                "/user/team/create",
                body=self.team_body(team_name),
                token=leader_token,
                assertion="team creation returns team id and requested fields",
            )
            team = require_dict(team_data, "create team")
            team_id = team.get("id")
            if not isinstance(team_id, int):
                raise HarnessFailure("create team: missing numeric id")
            if team.get("teamName") != team_name or team.get("joinType") != 2 or team.get("currentMembers") != 1:
                raise HarnessFailure(f"create team: unexpected returned fields {team}")
            self.created_ids["teamId"] = team_id

            detail = require_dict(
                self.call_success(
                    "get team detail after create",
                    "GET",
                    f"/user/team/{team_id}",
                    token=leader_token,
                    assertion="detail echoes created fields and leader membership",
                ),
                "get team detail after create",
            )
            if detail.get("id") != team_id or detail.get("teamName") != team_name:
                raise HarnessFailure("team detail: id/name mismatch after create")
            if detail.get("joined") is not True or detail.get("myRole") != 1:
                raise HarnessFailure("team detail: leader should be joined with role=1")
            if not detail.get("inviteCode"):
                raise HarnessFailure("team detail: inviteCode should be present for leader")

            updated_name = f"API Correct Team U {RUN_ID[-5:]}"
            updated = require_dict(
                self.call_success(
                    "update team",
                    "PUT",
                    f"/user/team/{team_id}",
                    body=self.team_body(updated_name),
                    token=leader_token,
                    assertion="leader can update team name and core fields",
                ),
                "update team",
            )
            if updated.get("teamName") != updated_name:
                raise HarnessFailure("update team: response did not include updated name")
            detail = require_dict(
                self.call_success(
                    "read back updated team",
                    "GET",
                    f"/user/team/{team_id}",
                    token=leader_token,
                    assertion="read-back detail reflects updated team name",
                ),
                "read back updated team",
            )
            if detail.get("teamName") != updated_name:
                raise HarnessFailure("read back updated team: name was not persisted")

            invite_code = self.call_success(
                "get invite code",
                "GET",
                f"/user/team/{team_id}/invite-code",
                token=leader_token,
                assertion="leader can read invite code",
            )
            if not isinstance(invite_code, str) or not invite_code:
                raise HarnessFailure("get invite code: data should be non-empty string")
            refreshed_code = self.call_success(
                "refresh invite code",
                "POST",
                f"/user/team/{team_id}/invite-code/refresh",
                token=leader_token,
                assertion="leader can refresh invite code",
            )
            if not isinstance(refreshed_code, str) or not refreshed_code:
                raise HarnessFailure("refresh invite code: data should be non-empty string")
            by_code = require_dict(
                self.call_success(
                    "get team by invite code",
                    "GET",
                    f"/user/team/by-code/{urllib.parse.quote(refreshed_code, safe='')}",
                    assertion="refreshed invite code resolves to the same team",
                ),
                "get team by invite code",
            )
            if by_code.get("id") != team_id:
                raise HarnessFailure("get team by invite code: resolved team id mismatch")

            self.call_expected_error(
                "non-member cannot create task",
                "POST",
                f"/user/team/{team_id}/task",
                body=self.task_body("Blocked Task"),
                token=member_token,
                message_part="不是小组成员",
                assertion="permission guard rejects task creation by a non-member",
            )

            task_name = f"Daily Correctness Task {RUN_ID[-6:]}"
            task_id = self.call_success(
                "create task",
                "POST",
                f"/user/team/{team_id}/task",
                body=self.task_body(task_name),
                token=leader_token,
                assertion="leader can create a task and receive task id",
            )
            if not isinstance(task_id, int):
                raise HarnessFailure("create task: data should be numeric task id")
            self.created_ids["taskId"] = task_id

            task_detail = require_dict(
                self.call_success(
                    "get task detail",
                    "GET",
                    f"/user/team/task/{task_id}",
                    token=leader_token,
                    assertion="task detail returns created task fields",
                ),
                "get task detail",
            )
            if task_detail.get("taskId") != task_id or task_detail.get("taskName") != task_name:
                raise HarnessFailure("get task detail: id/name mismatch")
            if task_detail.get("status") != 1:
                raise HarnessFailure("get task detail: new task should be enabled")

            updated_task_name = f"Daily Correctness Task U {RUN_ID[-5:]}"
            self.call_success(
                "update task",
                "PUT",
                f"/user/team/task/{task_id}",
                body=self.task_body(updated_task_name, status_probe=True),
                token=leader_token,
                assertion="leader can update task",
            )
            task_detail = require_dict(
                self.call_success(
                    "read back updated task",
                    "GET",
                    f"/user/team/task/{task_id}",
                    token=leader_token,
                    assertion="task detail reflects updated task name",
                ),
                "read back updated task",
            )
            if task_detail.get("taskName") != updated_task_name:
                raise HarnessFailure("read back updated task: task name was not persisted")

            self.call_success(
                "disable task",
                "PUT",
                f"/user/team/task/{task_id}/status",
                query={"status": 0},
                token=leader_token,
                assertion="leader can disable task",
            )
            task_detail = require_dict(
                self.call_success(
                    "read back disabled task",
                    "GET",
                    f"/user/team/task/{task_id}",
                    token=leader_token,
                    assertion="task status is disabled after status update",
                ),
                "read back disabled task",
            )
            if task_detail.get("status") != 0:
                raise HarnessFailure("read back disabled task: status should be 0")
            self.call_success(
                "enable task",
                "PUT",
                f"/user/team/task/{task_id}/status",
                query={"status": 1},
                token=leader_token,
                assertion="leader can re-enable task",
            )
            task_list = require_list(
                self.call_success(
                    "list team tasks",
                    "GET",
                    f"/user/team/{team_id}/tasks",
                    token=leader_token,
                    assertion="team task list includes created task",
                ),
                "list team tasks",
            )
            if find_by(task_list, "taskId", task_id) is None:
                raise HarnessFailure("list team tasks: created task not found")
            today_tasks = require_list(
                self.call_success(
                    "list today tasks",
                    "GET",
                    f"/user/team/{team_id}/tasks/today",
                    token=leader_token,
                    assertion="today task list includes enabled daily task",
                ),
                "list today tasks",
            )
            if find_by(today_tasks, "taskId", task_id) is None:
                raise HarnessFailure("list today tasks: enabled daily task not found")

            self.call_success(
                "member applies to join",
                "POST",
                f"/user/team/{team_id}/join",
                body={"applyReason": "join correctness"},
                token=member_token,
                assertion="apply-mode team creates a pending application",
            )
            applications = require_list(
                self.call_success(
                    "leader lists pending applications",
                    "GET",
                    f"/user/team/{team_id}/applications",
                    token=leader_token,
                    assertion="leader sees member pending application",
                ),
                "leader lists pending applications",
            )
            app = find_by(applications, "userId", self.member["id"])
            if app is None or not isinstance(app.get("id"), int) or app.get("status") != 0:
                raise HarnessFailure("leader lists pending applications: expected pending app for member")
            application_id = app["id"]
            self.created_ids["applicationId"] = application_id
            self.call_success(
                "leader approves application",
                "POST",
                f"/user/team/{team_id}/application/{application_id}/approve",
                token=leader_token,
                assertion="leader can approve pending application",
            )
            members = require_list(
                self.call_success(
                    "list members after approval",
                    "GET",
                    f"/user/team/{team_id}/members",
                    token=leader_token,
                    assertion="approved member appears in member list",
                ),
                "list members after approval",
            )
            member_row = find_by(members, "userId", self.member["id"])
            if member_row is None or member_row.get("role") != 3 or member_row.get("status") != 1:
                raise HarnessFailure("list members after approval: approved member missing or wrong role/status")
            self.created_ids["approvedMemberUserId"] = self.member["id"]
            detail_as_member = require_dict(
                self.call_success(
                    "member reads joined team detail",
                    "GET",
                    f"/user/team/{team_id}",
                    token=member_token,
                    assertion="approved member sees joined=true and member role",
                ),
                "member reads joined team detail",
            )
            if detail_as_member.get("joined") is not True or detail_as_member.get("myRole") != 3:
                raise HarnessFailure("member reads joined team detail: expected joined member role")

            self.call_success(
                "promote member to admin",
                "PUT",
                f"/user/team/{team_id}/member/{self.member['id']}/role",
                query={"role": 2},
                token=leader_token,
                assertion="leader can promote member to admin",
            )
            members = require_list(
                self.call_success(
                    "read back promoted member",
                    "GET",
                    f"/user/team/{team_id}/members",
                    token=leader_token,
                    assertion="member role reflects admin promotion",
                ),
                "read back promoted member",
            )
            member_row = find_by(members, "userId", self.member["id"])
            if member_row is None or member_row.get("role") != 2:
                raise HarnessFailure("read back promoted member: role should be admin")
            self.call_success(
                "demote member back to member",
                "PUT",
                f"/user/team/{team_id}/member/{self.member['id']}/role",
                query={"role": 3},
                token=leader_token,
                assertion="leader can demote admin back to member",
            )
            self.call_success(
                "mute member",
                "POST",
                f"/user/team/{team_id}/member/{self.member['id']}/mute",
                query={"minutes": 1},
                token=leader_token,
                assertion="leader can mute a regular member",
            )
            muted = require_list(
                self.call_success(
                    "read back muted member",
                    "GET",
                    f"/user/team/{team_id}/members",
                    token=leader_token,
                    assertion="member shows active mute after mute operation",
                ),
                "read back muted member",
            )
            muted_row = find_by(muted, "userId", self.member["id"])
            if muted_row is None or muted_row.get("isMuted") is not True:
                raise HarnessFailure("read back muted member: isMuted should be true")
            self.call_success(
                "unmute member",
                "DELETE",
                f"/user/team/{team_id}/member/{self.member['id']}/mute",
                token=leader_token,
                assertion="leader can unmute a member",
            )
            unmuted = require_list(
                self.call_success(
                    "read back unmuted member",
                    "GET",
                    f"/user/team/{team_id}/members",
                    token=leader_token,
                    assertion="member mute state clears after unmute",
                ),
                "read back unmuted member",
            )
            unmuted_row = find_by(unmuted, "userId", self.member["id"])
            if unmuted_row is None or unmuted_row.get("isMuted") is not False:
                raise HarnessFailure("read back unmuted member: isMuted should be false")

            checkin_content = f"done correctness {RUN_ID}"
            checkin_id = self.call_success(
                "member checkin",
                "POST",
                f"/user/team/{team_id}/checkin",
                body={
                    "taskId": task_id,
                    "completeValue": 1,
                    "content": checkin_content,
                    "images": [],
                    "duration": 5,
                    "relatedLink": "https://example.com/code-nest",
                },
                token=member_token,
                assertion="member can check in to active team task and receives checkin id",
            )
            if not isinstance(checkin_id, int):
                raise HarnessFailure("member checkin: data should be numeric checkin id")
            self.created_ids["checkinId"] = checkin_id

            checkin = require_dict(
                self.call_success(
                    "get checkin detail",
                    "GET",
                    f"/user/team/checkin/{checkin_id}",
                    token=member_token,
                    assertion="checkin detail returns created values",
                ),
                "get checkin detail",
            )
            if checkin.get("id") != checkin_id or checkin.get("taskId") != task_id:
                raise HarnessFailure("get checkin detail: id/task mismatch")
            if checkin.get("content") != checkin_content or checkin.get("duration") != 5:
                raise HarnessFailure("get checkin detail: content/duration mismatch")
            task_after_checkin = require_dict(
                self.call_success(
                    "task detail after member checkin",
                    "GET",
                    f"/user/team/task/{task_id}",
                    token=member_token,
                    assertion="task detail marks current user as checked in today",
                ),
                "task detail after member checkin",
            )
            if task_after_checkin.get("todayCheckedIn") is not True:
                raise HarnessFailure("task detail after member checkin: todayCheckedIn should be true for member")
            self.call_success(
                "leader likes checkin",
                "POST",
                f"/user/team/checkin/{checkin_id}/like",
                token=leader_token,
                assertion="another member can like checkin",
            )
            liked_checkin = require_dict(
                self.call_success(
                    "read back liked checkin",
                    "GET",
                    f"/user/team/checkin/{checkin_id}",
                    token=leader_token,
                    assertion="checkin like count and liked flag reflect like operation",
                ),
                "read back liked checkin",
            )
            if liked_checkin.get("liked") is not True or liked_checkin.get("likeCount", 0) < 1:
                raise HarnessFailure("read back liked checkin: liked flag/count did not update")
            self.call_success(
                "leader unlikes checkin",
                "DELETE",
                f"/user/team/checkin/{checkin_id}/like",
                token=leader_token,
                assertion="checkin unlike operation succeeds",
            )
            checkin_list = require_list(
                self.call_success(
                    "list team checkins",
                    "GET",
                    f"/user/team/{team_id}/checkins",
                    query={"taskId": task_id, "page": 1, "pageSize": 20},
                    token=member_token,
                    assertion="team checkin feed includes created checkin",
                ),
                "list team checkins",
            )
            if find_by(checkin_list, "id", checkin_id) is None:
                raise HarnessFailure("list team checkins: created checkin not found")

            discussion_title = f"Correctness Discussion {RUN_ID[-6:]}"
            discussion_id = self.call_success(
                "leader creates discussion",
                "POST",
                f"/user/team/{team_id}/discussion",
                body=self.discussion_body(discussion_title, content="verify discussion create"),
                token=leader_token,
                assertion="leader can create discussion",
            )
            if not isinstance(discussion_id, int):
                raise HarnessFailure("leader creates discussion: data should be numeric discussion id")
            self.created_ids["discussionId"] = discussion_id
            discussion = require_dict(
                self.call_success(
                    "get discussion detail",
                    "GET",
                    f"/user/team/discussion/{discussion_id}",
                    token=leader_token,
                    assertion="discussion detail returns created fields",
                ),
                "get discussion detail",
            )
            if discussion.get("id") != discussion_id or discussion.get("title") != discussion_title:
                raise HarnessFailure("get discussion detail: id/title mismatch")
            discussion_updated_title = f"Correctness Discussion U {RUN_ID[-5:]}"
            self.call_success(
                "update discussion",
                "PUT",
                f"/user/team/discussion/{discussion_id}",
                body=self.discussion_body(discussion_updated_title, content="verify discussion update"),
                token=leader_token,
                assertion="owner can update discussion",
            )
            discussion = require_dict(
                self.call_success(
                    "read back updated discussion",
                    "GET",
                    f"/user/team/discussion/{discussion_id}",
                    token=leader_token,
                    assertion="discussion detail reflects updated title",
                ),
                "read back updated discussion",
            )
            if discussion.get("title") != discussion_updated_title:
                raise HarnessFailure("read back updated discussion: title was not persisted")
            self.call_success(
                "top discussion",
                "PUT",
                f"/user/team/discussion/{discussion_id}/top",
                query={"isTop": 1},
                token=leader_token,
                assertion="leader can mark discussion as top",
            )
            self.call_success(
                "essence discussion",
                "PUT",
                f"/user/team/discussion/{discussion_id}/essence",
                query={"isEssence": 1},
                token=leader_token,
                assertion="leader can mark discussion as essence",
            )
            discussion = require_dict(
                self.call_success(
                    "read back discussion flags",
                    "GET",
                    f"/user/team/discussion/{discussion_id}",
                    token=leader_token,
                    assertion="discussion top/essence flags persist",
                ),
                "read back discussion flags",
            )
            if discussion.get("isTop") != 1 or discussion.get("isEssence") != 1:
                raise HarnessFailure("read back discussion flags: flags did not persist")
            self.call_success(
                "member likes discussion",
                "POST",
                f"/user/team/discussion/{discussion_id}/like",
                token=member_token,
                assertion="member can like discussion",
            )
            discussion = require_dict(
                self.call_success(
                    "read back liked discussion",
                    "GET",
                    f"/user/team/discussion/{discussion_id}",
                    token=member_token,
                    assertion="discussion like count and liked flag reflect like operation",
                ),
                "read back liked discussion",
            )
            if discussion.get("liked") is not True or discussion.get("likeCount", 0) < 1:
                raise HarnessFailure("read back liked discussion: liked flag/count did not update")
            self.call_success(
                "member unlikes discussion",
                "DELETE",
                f"/user/team/discussion/{discussion_id}/like",
                token=member_token,
                assertion="discussion unlike operation succeeds",
            )
            discussions = require_list(
                self.call_success(
                    "list discussions",
                    "GET",
                    f"/user/team/{team_id}/discussions",
                    query={"category": 5, "page": 1, "pageSize": 20},
                    token=member_token,
                    assertion="discussion list includes created discussion",
                ),
                "list discussions",
            )
            if find_by(discussions, "id", discussion_id) is None:
                raise HarnessFailure("list discussions: created discussion not found")

            ranks = require_list(
                self.call_success(
                    "checkin rank",
                    "GET",
                    f"/user/team/{team_id}/rank/checkin",
                    query={"type": "total", "limit": 20},
                    token=member_token,
                    assertion="checkin rank accepts legal query and includes active members",
                ),
                "checkin rank",
            )
            if find_by(ranks, "userId", self.member["id"]) is None:
                raise HarnessFailure("checkin rank: member not present")
            self.call_success(
                "duration rank",
                "GET",
                f"/user/team/{team_id}/rank/duration",
                query={"type": "total", "limit": 20},
                token=member_token,
                assertion="duration rank accepts legal query",
            )
            my_rank = require_dict(
                self.call_success(
                    "my rank",
                    "GET",
                    f"/user/team/{team_id}/rank/my",
                    query={"rankType": "checkin"},
                    token=member_token,
                    assertion="my rank returns current user rank with legal rankType",
                ),
                "my rank",
            )
            if my_rank.get("userId") != self.member["id"]:
                raise HarnessFailure("my rank: expected current member user id")

            total_checkins = self.call_success(
                "total checkin days",
                "GET",
                f"/user/team/{team_id}/checkin/total",
                token=member_token,
                assertion="member total checkin days reflects at least one checkin",
            )
            if not isinstance(total_checkins, int) or total_checkins < 1:
                raise HarnessFailure("total checkin days: expected integer >= 1")
            streak = self.call_success(
                "streak days",
                "GET",
                f"/user/team/{team_id}/checkin/streak",
                token=member_token,
                assertion="member streak endpoint returns numeric streak",
            )
            if not isinstance(streak, int) or streak < 0:
                raise HarnessFailure("streak days: expected non-negative integer")
            calendar = require_list(
                self.call_success(
                    "checkin calendar",
                    "GET",
                    f"/user/team/{team_id}/checkin/calendar",
                    query={"year": 2026, "month": 6},
                    token=member_token,
                    assertion="checkin calendar returns list of checkin dates",
                ),
                "checkin calendar",
            )
            if "2026-06-06" not in calendar and not calendar:
                raise HarnessFailure("checkin calendar: expected at least one checkin date")

            stats = require_dict(
                self.call_success(
                    "team stats",
                    "GET",
                    f"/user/team/{team_id}/stats",
                    token=member_token,
                    assertion="team stats reflect created task/member/checkin/discussion counts",
                ),
                "team stats",
            )
            if stats.get("teamId") != team_id:
                raise HarnessFailure("team stats: team id mismatch")
            if stats.get("memberCount", 0) < 2 or stats.get("taskCount", 0) < 1:
                raise HarnessFailure("team stats: member/task counts did not reflect state")
            if stats.get("totalCheckins", 0) < 1 or stats.get("discussionCount", 0) < 1:
                raise HarnessFailure("team stats: checkin/discussion counts did not reflect state")
            my_stats = require_dict(
                self.call_success(
                    "my stats",
                    "GET",
                    f"/user/team/{team_id}/stats/my",
                    token=member_token,
                    assertion="my stats reflect member checkin state",
                ),
                "my stats",
            )
            if my_stats.get("userId") != self.member["id"] or my_stats.get("totalCheckins", 0) < 1:
                raise HarnessFailure("my stats: user id/checkin count mismatch")

            self.cleanup()
        except Exception as e:
            failure = f"{type(e).__name__}: {truncate(e)}"
            self.context["failureTraceback"] = traceback.format_exc(limit=6)
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

    def cleanup(self) -> None:
        leader_token = self.leader.get("token")
        member_token = self.member.get("token")
        team_id = self.created_ids.get("teamId")
        checkin_id = self.created_ids.get("checkinId")
        discussion_id = self.created_ids.get("discussionId")
        task_id = self.created_ids.get("taskId")
        member_id = self.created_ids.get("approvedMemberUserId")

        if discussion_id and leader_token:
            try:
                self.call_success(
                    "cleanup delete discussion",
                    "DELETE",
                    f"/user/team/discussion/{discussion_id}",
                    token=leader_token,
                    assertion="cleanup removes discussion created by harness",
                )
            except Exception:
                pass
        if checkin_id and member_token:
            try:
                self.call_success(
                    "cleanup delete checkin",
                    "DELETE",
                    f"/user/team/checkin/{checkin_id}",
                    token=member_token,
                    assertion="cleanup removes checkin created by harness",
                )
            except Exception:
                pass
        if task_id and leader_token:
            try:
                self.call_success(
                    "cleanup delete task",
                    "DELETE",
                    f"/user/team/task/{task_id}",
                    token=leader_token,
                    assertion="cleanup removes task created by harness",
                )
            except Exception:
                pass
        if team_id and member_id and leader_token:
            try:
                self.call_success(
                    "cleanup remove member",
                    "DELETE",
                    f"/user/team/{team_id}/member/{member_id}",
                    token=leader_token,
                    assertion="cleanup removes approved member before dissolve",
                )
            except Exception:
                pass
        if team_id and leader_token:
            try:
                self.call_success(
                    "cleanup dissolve team",
                    "DELETE",
                    f"/user/team/{team_id}",
                    token=leader_token,
                    assertion="cleanup dissolves team created by harness",
                )
            except Exception:
                pass

    def sanitized_context(self) -> dict[str, Any]:
        context = dict(self.context)
        for key, value in (("leader", self.leader), ("member", self.member)):
            if value:
                context[key] = {k: v for k, v in value.items() if k != "token"}
        context["createdIds"] = self.created_ids
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
            "service": "user-team-controller",
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
    parser = argparse.ArgumentParser(description="Run user-team service correctness checks")
    parser.add_argument("--base-url", default="http://127.0.0.1:9999/api")
    parser.add_argument("--out-dir", required=True)
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    harness = TeamCorrectnessHarness(args.base_url, args.out_dir)
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
