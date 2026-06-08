#!/usr/bin/env python3
"""Service-level correctness harness for notification APIs.

This is stricter than HTTP smoke coverage:
- admin send/create success paths must be visible in user/admin readbacks.
- unread/read/delete state changes must be asserted after each operation.
- announcement read state must be isolated per user via read records.
- expected auth, validation, and business errors are counted separately.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import time
import traceback
import urllib.parse
from typing import Any

from team_correctness_harness import (
    TEST_PASSWORD,
    HarnessFailure,
    assert_business_error,
    assert_success_http,
    generate_captcha,
    http_request,
    now_iso,
    response_data,
    result_code,
    result_message,
    truncate,
    write_json,
)


RUN_ID = dt.datetime.now().strftime("%m%d%H%M%S")


def require_dict(value: Any, step: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise HarnessFailure(f"{step}: expected response data object, got {type(value).__name__}")
    return value


def require_list(value: Any, step: str) -> list[Any]:
    if not isinstance(value, list):
        raise HarnessFailure(f"{step}: expected response data list, got {type(value).__name__}")
    return value


def page_records(value: Any, step: str) -> list[Any]:
    page = require_dict(value, step)
    return require_list(page.get("records"), f"{step} records")


def find_by(items: list[Any], key: str, expected: Any) -> dict[str, Any] | None:
    for item in items:
        if isinstance(item, dict) and item.get(key) == expected:
            return item
    return None


def find_by_title(items: list[Any], expected_title: str) -> dict[str, Any] | None:
    return find_by(items, "title", expected_title)


class NotificationCorrectnessHarness:
    def __init__(self, base_url: str, out_dir: str):
        self.base_url = base_url.rstrip("/")
        self.out_dir = out_dir
        self.results: list[dict[str, Any]] = []
        self.context: dict[str, Any] = {"runId": RUN_ID, "baseUrl": self.base_url}
        self.admin_token: str | None = None
        self.primary_user: dict[str, Any] = {}
        self.secondary_user: dict[str, Any] = {}
        self.created_ids: dict[str, Any] = {}

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

    def admin_login(self) -> None:
        login = require_dict(
            self.call_success(
                "admin login",
                "POST",
                "/auth/login",
                body={"username": "admin", "password": "123456", "rememberMe": False},
                assertion="seed admin can log in for notification management",
            ),
            "admin login",
        )
        token = login.get("accessToken")
        if not isinstance(token, str) or not token:
            raise HarnessFailure("admin login: missing accessToken")
        self.admin_token = token

    def register_and_login_user(self, suffix: str) -> dict[str, Any]:
        username = f"notice{suffix}{RUN_ID[-8:]}"
        phone_prefix = "170" if suffix == "a" else "171"
        phone = phone_prefix + RUN_ID[-8:]

        key, code = generate_captcha(self.base_url)
        registered = require_dict(
            self.call_success(
                f"register notification user {suffix}",
                "POST",
                "/user/auth/register",
                body={
                    "username": username,
                    "password": TEST_PASSWORD,
                    "confirmPassword": TEST_PASSWORD,
                    "nickname": f"Notice User {suffix.upper()} {RUN_ID[-4:]}",
                    "email": f"{username}@example.com",
                    "phone": phone,
                    "captcha": code,
                    "captchaKey": key,
                },
                assertion="new isolated user can be registered for notification flow",
            ),
            f"register notification user {suffix}",
        )
        user_id = registered.get("id")
        if not isinstance(user_id, int):
            raise HarnessFailure(f"register notification user {suffix}: missing numeric user id")

        key, code = generate_captcha(self.base_url)
        login = require_dict(
            self.call_success(
                f"login notification user {suffix}",
                "POST",
                "/user/auth/login",
                body={
                    "username": username,
                    "password": TEST_PASSWORD,
                    "captcha": code,
                    "captchaKey": key,
                    "rememberMe": False,
                },
                assertion="registered notification user can obtain access token",
            ),
            f"login notification user {suffix}",
        )
        token = login.get("accessToken")
        if not isinstance(token, str) or not token:
            raise HarnessFailure(f"login notification user {suffix}: missing accessToken")
        return {"id": user_id, "username": username, "phone": phone, "token": token}

    def unread_count(self, token: str, step: str) -> int:
        value = self.call_success(
            step,
            "GET",
            "/notification/unread-count",
            token=token,
            assertion="user unread count returns an integer business value",
        )
        if not isinstance(value, int):
            raise HarnessFailure(f"{step}: expected integer unread count, got {type(value).__name__}")
        return value

    def user_messages(
        self,
        token: str,
        step: str,
        *,
        title: str | None = None,
        status: str | None = None,
        type_: str | None = None,
        page_size: int = 20,
    ) -> list[Any]:
        body: dict[str, Any] = {"pageNum": 1, "pageSize": page_size}
        if title:
            body["title"] = title
        if status:
            body["status"] = status
        if type_:
            body["type"] = type_
        return page_records(
            self.call_success(
                step,
                "POST",
                "/notification/list",
                body=body,
                token=token,
                assertion="user notification list returns page records matching query",
            ),
            step,
        )

    def admin_messages(self, step: str, *, title: str | None = None, status: str | None = None, type_: str | None = None) -> list[Any]:
        if not self.admin_token:
            raise HarnessFailure("admin token missing")
        body: dict[str, Any] = {"pageNum": 1, "pageSize": 50}
        if title:
            body["title"] = title
        if status:
            body["status"] = status
        if type_:
            body["type"] = type_
        return page_records(
            self.call_success(
                step,
                "POST",
                "/admin/notification/list",
                body=body,
                token=self.admin_token,
                assertion="admin notification list returns page records matching query",
            ),
            step,
        )

    def wait_for_user_message(self, token: str, title: str, step: str, *, type_: str | None = None) -> dict[str, Any]:
        last_records: list[Any] = []
        for _ in range(12):
            records = self.user_messages(token, step, title=title, type_=type_)
            last_records = records
            found = find_by_title(records, title)
            if found:
                return found
            time.sleep(0.35)
        raise HarnessFailure(f"{step}: message title {title!r} not visible in user list; lastRecords={truncate(last_records)}")

    def wait_for_admin_message(self, title: str, step: str, *, type_: str | None = None) -> dict[str, Any]:
        last_records: list[Any] = []
        for _ in range(12):
            records = self.admin_messages(step, title=title, type_=type_)
            last_records = records
            found = find_by_title(records, title)
            if found:
                return found
            time.sleep(0.35)
        raise HarnessFailure(f"{step}: message title {title!r} not visible in admin list; lastRecords={truncate(last_records)}")

    def admin_statistics(self, step: str) -> dict[str, Any]:
        if not self.admin_token:
            raise HarnessFailure("admin token missing")
        return require_dict(
            self.call_success(
                step,
                "POST",
                "/admin/notification/statistics",
                body={},
                token=self.admin_token,
                assertion="admin notification statistics returns typed counters",
            ),
            step,
        )

    def run(self) -> dict[str, Any]:
        failure: str | None = None
        try:
            self.admin_login()
            self.primary_user = self.register_and_login_user("a")
            self.secondary_user = self.register_and_login_user("b")
            if not self.admin_token:
                raise HarnessFailure("admin token missing after login")

            user_a_id = self.primary_user["id"]
            user_b_id = self.secondary_user["id"]
            user_a_token = self.primary_user["token"]
            user_b_token = self.secondary_user["token"]
            self.context["primaryUserId"] = user_a_id
            self.context["secondaryUserId"] = user_b_id

            self.call_expected_error(
                "notification list requires user auth",
                "POST",
                "/notification/list",
                body={"pageNum": 1, "pageSize": 10},
                message_part="Token",
                assertion="anonymous user cannot list notifications",
            )
            self.call_expected_error(
                "admin statistics requires admin auth",
                "POST",
                "/admin/notification/statistics",
                body={},
                message_part="Token",
                assertion="anonymous caller cannot read admin notification statistics",
            )

            unread_a_initial = self.unread_count(user_a_token, "initial unread count user a")
            unread_b_initial = self.unread_count(user_b_token, "initial unread count user b")
            stats_before = self.admin_statistics("admin statistics before notification writes")

            personal_title = f"Correct Personal {RUN_ID[-6:]}"
            self.call_success(
                "admin batch send personal messages",
                "POST",
                "/admin/notification/batch-send",
                body={
                    "receiverIds": [user_a_id, user_b_id],
                    "title": personal_title,
                    "content": f"personal correctness content {RUN_ID}",
                    "type": "PERSONAL",
                },
                token=self.admin_token,
                assertion="admin can send one personal message to each target user",
            )
            personal_a = self.wait_for_user_message(user_a_token, personal_title, "user a sees personal message", type_="PERSONAL")
            personal_b = self.wait_for_user_message(user_b_token, personal_title, "user b sees personal message", type_="PERSONAL")
            for label, message, expected_user_id in (("user a", personal_a, user_a_id), ("user b", personal_b, user_b_id)):
                if message.get("receiverId") != expected_user_id:
                    raise HarnessFailure(f"{label} personal message: receiverId mismatch")
                if message.get("type") != "PERSONAL" or message.get("status") != "UNREAD":
                    raise HarnessFailure(f"{label} personal message: expected PERSONAL/UNREAD, got {message}")
            self.created_ids["personalMessageA"] = personal_a.get("id")
            self.created_ids["personalMessageB"] = personal_b.get("id")
            if self.unread_count(user_a_token, "unread after personal user a") < unread_a_initial + 1:
                raise HarnessFailure("unread after personal user a: count did not increase")
            if self.unread_count(user_b_token, "unread after personal user b") < unread_b_initial + 1:
                raise HarnessFailure("unread after personal user b: count did not increase")

            detail_a = require_dict(
                self.call_success(
                    "user a opens personal detail",
                    "GET",
                    f"/notification/{personal_a['id']}",
                    token=user_a_token,
                    assertion="opening personal detail returns the message and marks it read",
                ),
                "user a opens personal detail",
            )
            if detail_a.get("id") != personal_a.get("id") or detail_a.get("title") != personal_title:
                raise HarnessFailure("user a opens personal detail: id/title mismatch")
            personal_a_after = self.wait_for_user_message(user_a_token, personal_title, "user a personal readback after detail", type_="PERSONAL")
            if personal_a_after.get("status") != "READ":
                raise HarnessFailure("user a personal readback after detail: status should be READ")
            personal_b_after = self.wait_for_user_message(user_b_token, personal_title, "user b personal remains unread", type_="PERSONAL")
            if personal_b_after.get("status") != "UNREAD":
                raise HarnessFailure("user b personal remains unread: another user's read should not affect it")

            self.call_success(
                "user b marks personal as read",
                "POST",
                "/notification/mark-read",
                body={"messageId": personal_b["id"]},
                token=user_b_token,
                assertion="single mark-read changes personal message status",
            )
            personal_b_read = self.wait_for_user_message(user_b_token, personal_title, "user b personal readback after mark-read", type_="PERSONAL")
            if personal_b_read.get("status") != "READ":
                raise HarnessFailure("user b personal readback after mark-read: status should be READ")

            system_title = f"Correct System {RUN_ID[-6:]}"
            self.call_success(
                "admin batch send system messages",
                "POST",
                "/admin/notification/batch-send",
                body={
                    "receiverIds": [user_a_id],
                    "title": system_title,
                    "content": f"system correctness content {RUN_ID}",
                    "type": "SYSTEM",
                },
                token=self.admin_token,
                assertion="admin can send a SYSTEM batch message and preserve requested type",
            )
            system_a = self.wait_for_user_message(user_a_token, system_title, "user a sees system message", type_="SYSTEM")
            if system_a.get("type") != "SYSTEM" or system_a.get("receiverId") != user_a_id or system_a.get("status") != "UNREAD":
                raise HarnessFailure(f"user a system message: expected SYSTEM/UNREAD for receiver, got {system_a}")
            self.created_ids["systemMessageA"] = system_a.get("id")
            admin_system = self.wait_for_admin_message(system_title, "admin sees system message", type_="SYSTEM")
            if admin_system.get("type") != "SYSTEM" or admin_system.get("receiverId") != user_a_id:
                raise HarnessFailure("admin sees system message: type/receiver mismatch")

            announcement_title = f"Correct Announcement {RUN_ID[-6:]}"
            self.call_success(
                "admin publishes announcement",
                "POST",
                "/admin/notification/announcement",
                body={
                    "title": announcement_title,
                    "content": f"announcement correctness content {RUN_ID}",
                    "priority": "HIGH",
                },
                token=self.admin_token,
                assertion="admin can publish broadcast announcement",
            )
            announcement_a = self.wait_for_user_message(user_a_token, announcement_title, "user a sees announcement", type_="ANNOUNCEMENT")
            announcement_b = self.wait_for_user_message(user_b_token, announcement_title, "user b sees announcement", type_="ANNOUNCEMENT")
            for label, message in (("user a", announcement_a), ("user b", announcement_b)):
                if message.get("receiverId") is not None or message.get("type") != "ANNOUNCEMENT":
                    raise HarnessFailure(f"{label} announcement: expected broadcast ANNOUNCEMENT, got {message}")
                if message.get("priority") != "HIGH" or message.get("status") != "UNREAD":
                    raise HarnessFailure(f"{label} announcement: priority/status mismatch")
            self.created_ids["announcementMessage"] = announcement_a.get("id")

            self.call_success(
                "user a marks announcement read",
                "POST",
                "/notification/mark-read",
                body={"messageId": announcement_a["id"]},
                token=user_a_token,
                assertion="marking broadcast announcement read creates user-specific read record",
            )
            announcement_a_read = self.wait_for_user_message(user_a_token, announcement_title, "user a announcement readback", type_="ANNOUNCEMENT")
            announcement_b_unread = self.wait_for_user_message(user_b_token, announcement_title, "user b announcement remains unread", type_="ANNOUNCEMENT")
            if announcement_a_read.get("status") not in {"READ", "read"}:
                raise HarnessFailure("user a announcement readback: status should be read through read record")
            if announcement_b_unread.get("status") != "UNREAD":
                raise HarnessFailure("user b announcement remains unread: read record leaked across users")

            unread_b_before_all = self.unread_count(user_b_token, "user b unread before mark all")
            self.call_success(
                "user b marks all notifications read",
                "POST",
                "/notification/mark-all-read",
                token=user_b_token,
                assertion="mark-all-read clears unread personal and announcement messages for current user",
            )
            unread_b_after_all = self.unread_count(user_b_token, "user b unread after mark all")
            if unread_b_after_all >= unread_b_before_all and unread_b_before_all > 0:
                raise HarnessFailure("user b unread after mark all: unread count should decrease")
            if self.wait_for_user_message(user_b_token, announcement_title, "user b announcement after mark all", type_="ANNOUNCEMENT").get("status") not in {"READ", "read"}:
                raise HarnessFailure("user b announcement after mark all: status should be read")

            self.call_success(
                "user a deletes system message",
                "POST",
                "/notification/delete",
                body={"messageId": system_a["id"]},
                token=user_a_token,
                assertion="user can delete own personal/system notification",
            )
            deleted_filtered = self.user_messages(user_a_token, "user a deleted system not in active list", title=system_title, type_="SYSTEM")
            if find_by_title(deleted_filtered, system_title):
                raise HarnessFailure("user a deleted system not in active list: deleted message still visible")

            stats_after = self.admin_statistics("admin statistics after notification writes")
            if stats_after.get("todayTotal", 0) < stats_before.get("todayTotal", 0) + 4:
                raise HarnessFailure("admin statistics after writes: todayTotal did not include created notifications")
            if stats_after.get("personalCount", 0) < stats_before.get("personalCount", 0) + 2:
                raise HarnessFailure("admin statistics after writes: personalCount did not include personal messages")
            if stats_after.get("systemCount", 0) < stats_before.get("systemCount", 0) + 1:
                raise HarnessFailure("admin statistics after writes: systemCount did not include system message")
            if stats_after.get("announcementCount", 0) < stats_before.get("announcementCount", 0) + 1:
                raise HarnessFailure("admin statistics after writes: announcementCount did not include announcement")

            template_code = f"CORRECT_{RUN_ID[-8:]}"
            self.call_success(
                "admin creates notification template",
                "POST",
                "/admin/notification/templates",
                body={
                    "code": template_code,
                    "name": f"Correct Template {RUN_ID[-6:]}",
                    "titleTemplate": "Hello {name}",
                    "contentTemplate": "Content {value}",
                    "isEnabled": True,
                },
                token=self.admin_token,
                assertion="admin can create a reusable notification template",
            )
            templates = require_list(
                self.call_success(
                    "admin template list after create",
                    "GET",
                    "/admin/notification/templates",
                    token=self.admin_token,
                    assertion="created enabled template is visible in template list",
                ),
                "admin template list after create",
            )
            template = find_by(templates, "code", template_code)
            if not template or not isinstance(template.get("id"), int):
                raise HarnessFailure("admin template list after create: created template missing")
            template_id = template["id"]
            self.created_ids["templateId"] = template_id

            self.call_success(
                "admin updates notification template",
                "PUT",
                f"/admin/notification/templates/{template_id}",
                body={
                    "code": template_code,
                    "name": f"Correct Template Updated {RUN_ID[-5:]}",
                    "titleTemplate": "Updated {name}",
                    "contentTemplate": "Updated content {value}",
                    "isEnabled": True,
                },
                token=self.admin_token,
                assertion="admin can update an existing template",
            )
            templates = require_list(
                self.call_success(
                    "admin template list after update",
                    "GET",
                    "/admin/notification/templates",
                    token=self.admin_token,
                    assertion="template update is visible in list readback",
                ),
                "admin template list after update",
            )
            updated_template = find_by(templates, "code", template_code)
            if not updated_template or updated_template.get("titleTemplate") != "Updated {name}":
                raise HarnessFailure("admin template list after update: titleTemplate did not persist")

            self.call_expected_error(
                "duplicate template code rejected",
                "POST",
                "/admin/notification/templates",
                body={
                    "code": template_code,
                    "name": "Duplicate Template",
                    "titleTemplate": "Dup",
                    "contentTemplate": "Dup",
                    "isEnabled": True,
                },
                token=self.admin_token,
                message_part="创建模板失败",
                assertion="duplicate template code must be rejected as business error",
            )

            self.call_expected_error(
                "batch send rejects empty receiver list",
                "POST",
                "/admin/notification/batch-send",
                body={"receiverIds": [], "title": "Invalid", "content": "Invalid", "type": "PERSONAL"},
                token=self.admin_token,
                message_part="接收者列表不能为空",
                assertion="batch send validates non-empty receiver list",
            )

            self.call_expected_error(
                "batch send rejects missing user",
                "POST",
                "/admin/notification/batch-send",
                body={"receiverIds": [999999999], "title": "Missing User", "content": "Missing", "type": "PERSONAL"},
                token=self.admin_token,
                message_part="用户不存在",
                assertion="batch send must not create notifications for nonexistent users",
            )

            self.cleanup()
        except Exception:
            failure = traceback.format_exc()
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
        if not self.admin_token:
            return
        for key in ("personalMessageA", "personalMessageB", "systemMessageA", "announcementMessage"):
            message_id = self.created_ids.get(key)
            if message_id:
                self.cleanup_delete_notification(key, message_id)
        template_id = self.created_ids.get("templateId")
        if template_id:
            try:
                self.call_success(
                    "cleanup delete notification template",
                    "DELETE",
                    f"/admin/notification/templates/{template_id}",
                    token=self.admin_token,
                    assertion="cleanup removes template created by harness",
                )
            except Exception:
                pass

    def cleanup_delete_notification(self, key: str, message_id: int) -> None:
        path = f"/admin/notification/delete/{message_id}"
        http = http_request(
            self.base_url,
            "POST",
            path,
            token=self.admin_token,
        )
        self.record(
            f"cleanup delete notification {key}",
            "POST",
            path,
            http,
            assertion="cleanup removes notification created by harness",
            status="PASS",
        )
        row = self.results[-1]
        if http.ok and result_code(http.json_body) == 200:
            return

        # Some scenarios intentionally delete a notification in the business flow
        # before cleanup. Treat that as an expected cleanup no-op, not a service
        # correctness failure.
        if http.ok and result_code(http.json_body) in {602, 404}:
            row["expected"] = "CLEANUP_NOOP"
            row["status"] = "PASS_EXPECTED_ERROR"
            row["assertion"] = "cleanup skips notifications already removed by the verified business flow"
            return

        row["status"] = "FAIL"

    def sanitized_context(self) -> dict[str, Any]:
        context = dict(self.context)
        for key, value in (("primaryUser", self.primary_user), ("secondaryUser", self.secondary_user)):
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
            "service": "notification-controller",
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
    parser = argparse.ArgumentParser(description="Run notification service correctness checks")
    parser.add_argument("--base-url", default="http://127.0.0.1:9999/api")
    parser.add_argument("--out-dir", required=True)
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    harness = NotificationCorrectnessHarness(args.base_url, args.out_dir)
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
