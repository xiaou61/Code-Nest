#!/usr/bin/env python3
"""Service-level correctness harness for xiaou-moyu APIs.

This is stricter than HTTP smoke coverage:
- HTTP 2xx alone is not success.
- success paths require business code 200 and state read-back assertions.
- expected validation/business errors are counted separately.
- external hot-topic data availability is treated as degraded data, but the
  local category contract is still asserted.

Tokens and captcha values are used in memory only and are not written to disk.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import time
import traceback
import urllib.parse
from decimal import Decimal
from typing import Any

from team_correctness_harness import (
    TEST_PASSWORD,
    HarnessFailure,
    assert_business_error,
    assert_success_http,
    generate_captcha,
    http_request,
    now_iso,
    require_dict,
    require_list,
    response_data,
    result_code,
    result_message,
    truncate,
    write_json,
)


RUN_ID = dt.datetime.now().strftime("%m%d%H%M%S")


def page_records(value: Any, step: str) -> list[Any]:
    page = require_dict(value, step)
    return require_list(page.get("records"), f"{step} records")


def find_by(items: list[Any], key: str, expected: Any) -> dict[str, Any] | None:
    for item in items:
        if isinstance(item, dict) and item.get(key) == expected:
            return item
    return None


def find_by_title(items: list[Any], title: str) -> dict[str, Any] | None:
    return find_by(items, "title", title)


def find_event_by_name(items: list[Any], event_name: str) -> dict[str, Any] | None:
    return find_by(items, "eventName", event_name)


def number_value(value: Any, step: str) -> Decimal:
    if value is None:
        raise HarnessFailure(f"{step}: expected numeric value, got null")
    try:
        return Decimal(str(value))
    except Exception as exc:
        raise HarnessFailure(f"{step}: expected numeric value, got {value!r}") from exc


class MoyuCorrectnessHarness:
    def __init__(self, base_url: str, out_dir: str):
        self.base_url = base_url.rstrip("/")
        self.out_dir = out_dir
        self.results: list[dict[str, Any]] = []
        self.context: dict[str, Any] = {"runId": RUN_ID, "baseUrl": self.base_url}
        self.admin_token: str | None = None
        self.user: dict[str, Any] = {}
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
        timeout: float = 10.0,
    ) -> Any:
        http = http_request(self.base_url, method, path, query=query, body=body, token=token, timeout=timeout)
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
                assertion="seed admin can log in for moyu management",
            ),
            "admin login",
        )
        token = login.get("accessToken")
        if not isinstance(token, str) or not token:
            raise HarnessFailure("admin login: missing accessToken")
        self.admin_token = token

    def register_and_login_user(self) -> dict[str, Any]:
        username = f"moyu{RUN_ID[-8:]}"
        key, code = generate_captcha(self.base_url)
        registered = require_dict(
            self.call_success(
                "register moyu user",
                "POST",
                "/user/auth/register",
                body={
                    "username": username,
                    "password": TEST_PASSWORD,
                    "confirmPassword": TEST_PASSWORD,
                    "nickname": f"Moyu Probe {RUN_ID[-4:]}",
                    "email": f"{username}@example.com",
                    "phone": "172" + RUN_ID[-8:],
                    "captcha": code,
                    "captchaKey": key,
                },
                assertion="new isolated user can be registered for moyu flows",
            ),
            "register moyu user",
        )
        user_id = registered.get("id")
        if not isinstance(user_id, int):
            raise HarnessFailure("register moyu user: missing numeric user id")

        key, code = generate_captcha(self.base_url)
        login = require_dict(
            self.call_success(
                "login moyu user",
                "POST",
                "/user/auth/login",
                body={
                    "username": username,
                    "password": TEST_PASSWORD,
                    "captcha": code,
                    "captchaKey": key,
                    "rememberMe": False,
                },
                assertion="newly registered user can obtain access token",
            ),
            "login moyu user",
        )
        token = login.get("accessToken")
        if not isinstance(token, str) or not token:
            raise HarnessFailure("login moyu user: missing accessToken")
        return {"id": user_id, "username": username, "token": token}

    def bug_body(self, title: str, *, status: int = 1, difficulty: int = 2) -> dict[str, Any]:
        return {
            "title": title,
            "phenomenon": f"Probe phenomenon {RUN_ID}",
            "causeAnalysis": f"Probe cause {RUN_ID}",
            "solution": f"Probe solution {RUN_ID}",
            "techTags": "api,correctness,moyu",
            "difficultyLevel": difficulty,
            "status": status,
            "sortOrder": 999,
        }

    def daily_body(self, title: str, *, content: str, status: int = 1, difficulty: int = 2) -> dict[str, Any]:
        return {
            "contentType": 3,
            "title": title,
            "content": content,
            "author": "api-correctness",
            "programmingLanguage": "Java",
            "tags": ["api", "correctness", "moyu"],
            "difficultyLevel": difficulty,
            "sourceUrl": "https://example.com/code-nest/moyu",
            "status": status,
        }

    def event_body(self, name: str, *, date: str, status: int = 1, is_major: int = 1) -> dict[str, Any]:
        return {
            "eventDate": date,
            "eventName": name,
            "eventType": 2,
            "description": f"Probe calendar event {RUN_ID}",
            "blessingText": f"Keep shipping {RUN_ID}",
            "relatedLinks": ["https://example.com/code-nest/calendar"],
            "icon": "calendar",
            "color": "brand",
            "isMajor": is_major,
            "sortOrder": 1,
            "status": status,
        }

    def run_bug_store_flow(self) -> None:
        assert self.admin_token and self.user.get("token")

        self.call_expected_error(
            "bug create rejects blank title",
            "POST",
            "/admin/moyu/bug-store",
            body=self.bug_body("", status=1),
            token=self.admin_token,
            message_part="Bug标题不能为空",
            assertion="validation error is reported as a business/validation failure",
        )

        title = f"API Moyu Bug {RUN_ID[-6:]}"
        bug_id = self.call_success(
            "create bug",
            "POST",
            "/admin/moyu/bug-store",
            body=self.bug_body(title),
            token=self.admin_token,
            assertion="admin bug creation returns generated id",
        )
        if not isinstance(bug_id, int):
            raise HarnessFailure("create bug: data should be numeric bug id")
        self.created_ids["bugId"] = bug_id

        detail = require_dict(
            self.call_success(
                "read created bug",
                "GET",
                f"/admin/moyu/bug-store/{bug_id}",
                token=self.admin_token,
                assertion="created bug detail echoes requested fields",
            ),
            "read created bug",
        )
        if detail.get("id") != bug_id or detail.get("title") != title or detail.get("status") != 1:
            raise HarnessFailure(f"read created bug: id/title/status mismatch {detail}")
        if detail.get("difficultyLevel") != 2 or "api" not in str(detail.get("techTags")):
            raise HarnessFailure("read created bug: difficulty/tags were not persisted")

        records = page_records(
            self.call_success(
                "list created bug",
                "POST",
                "/admin/moyu/bug-store/list",
                body={"current": 1, "size": 10, "title": title, "status": 1},
                token=self.admin_token,
                assertion="bug list can find the created bug by title/status",
            ),
            "list created bug",
        )
        if find_by(records, "id", bug_id) is None:
            raise HarnessFailure("list created bug: created bug not found")

        updated_title = f"API Moyu Bug U {RUN_ID[-5:]}"
        self.call_success(
            "update bug",
            "PUT",
            f"/admin/moyu/bug-store/{bug_id}",
            body=self.bug_body(updated_title, difficulty=3),
            token=self.admin_token,
            assertion="admin can update bug title and difficulty",
        )
        detail = require_dict(
            self.call_success(
                "read updated bug",
                "GET",
                f"/admin/moyu/bug-store/{bug_id}",
                token=self.admin_token,
                assertion="bug detail reflects update",
            ),
            "read updated bug",
        )
        if detail.get("title") != updated_title or detail.get("difficultyLevel") != 3:
            raise HarnessFailure("read updated bug: update was not persisted")

        random_bug = require_dict(
            self.call_success(
                "user random bug",
                "POST",
                "/moyu/bug-store/random",
                token=self.user["token"],
                assertion="authenticated user receives a structured enabled bug",
            ),
            "user random bug",
        )
        if not isinstance(random_bug.get("id"), int) or not random_bug.get("title"):
            raise HarnessFailure(f"user random bug: missing id/title {random_bug}")
        if not isinstance(random_bug.get("techTags"), list):
            raise HarnessFailure("user random bug: techTags should be a JSON list")

        self.call_success(
            "delete bug",
            "DELETE",
            f"/admin/moyu/bug-store/{bug_id}",
            token=self.admin_token,
            assertion="admin can delete created bug",
        )
        self.call_expected_error(
            "read deleted bug fails",
            "GET",
            f"/admin/moyu/bug-store/{bug_id}",
            token=self.admin_token,
            message_part="Bug不存在",
            assertion="deleted bug is no longer readable",
        )
        self.created_ids.pop("bugId", None)

    def run_daily_content_flow(self) -> None:
        assert self.admin_token and self.user.get("token")

        self.call_expected_error(
            "daily create rejects invalid type",
            "POST",
            "/admin/moyu/daily-content",
            body={**self.daily_body("bad", content="bad"), "contentType": 9},
            token=self.admin_token,
            message_part="内容类型值无效",
            assertion="daily content validates contentType",
        )

        title = f"API Moyu Daily {RUN_ID[-6:]}"
        content = f"public static int probe{RUN_ID[-4:]}() {{ return {RUN_ID[-2:]}; }}"
        self.call_success(
            "create daily content",
            "POST",
            "/admin/moyu/daily-content",
            body=self.daily_body(title, content=content),
            token=self.admin_token,
            assertion="admin can create daily content",
        )
        admin_items = require_list(
            self.call_success(
                "admin list created daily content",
                "GET",
                "/admin/moyu/daily-content/type/3",
                query={"limit": 20},
                token=self.admin_token,
                assertion="admin list can find newly created daily content",
            ),
            "admin list created daily content",
        )
        created = find_by_title(admin_items, title)
        if created is None or not isinstance(created.get("id"), int):
            raise HarnessFailure("admin list created daily content: created row not found")
        content_id = created["id"]
        self.created_ids["dailyContentId"] = content_id

        detail = require_dict(
            self.call_success(
                "read created daily content",
                "GET",
                f"/admin/moyu/daily-content/{content_id}",
                token=self.admin_token,
                assertion="created daily content detail echoes core fields",
            ),
            "read created daily content",
        )
        if detail.get("title") != title or detail.get("content") != content or detail.get("status") != 1:
            raise HarnessFailure("read created daily content: field mismatch")

        user_items = require_list(
            self.call_success(
                "user list daily content by type",
                "GET",
                "/moyu/daily-content/type/3",
                query={"limit": 20},
                token=self.user["token"],
                assertion="user content list includes enabled created content",
            ),
            "user list daily content by type",
        )
        user_item = find_by(user_items, "id", content_id)
        if user_item is None:
            raise HarnessFailure("user list daily content by type: created content not visible to user")
        if user_item.get("contentTypeName") != "代码片段" or user_item.get("difficultyLevelName") != "中级":
            raise HarnessFailure("user list daily content by type: type/difficulty names are wrong")
        initial_views = int(user_item.get("viewCount") or 0)
        initial_likes = int(user_item.get("likeCount") or 0)

        self.call_success(
            "view daily content",
            "POST",
            f"/moyu/daily-content/{content_id}/view",
            token=self.user["token"],
            assertion="view endpoint increments view count",
        )
        self.call_success(
            "like daily content",
            "POST",
            f"/moyu/daily-content/{content_id}/like",
            token=self.user["token"],
            assertion="like endpoint increments like count",
        )
        detail = require_dict(
            self.call_success(
                "read daily content counters",
                "GET",
                f"/admin/moyu/daily-content/{content_id}",
                token=self.admin_token,
                assertion="view/like counters are persisted",
            ),
            "read daily content counters",
        )
        if int(detail.get("viewCount") or 0) < initial_views + 1:
            raise HarnessFailure("read daily content counters: view count did not increment")
        if int(detail.get("likeCount") or 0) < initial_likes + 1:
            raise HarnessFailure("read daily content counters: like count did not increment")

        self.call_success(
            "collect daily content",
            "POST",
            f"/moyu/daily-content/{content_id}/toggle-collection",
            token=self.user["token"],
            assertion="user can collect daily content",
        )
        collections = require_list(
            self.call_success(
                "list daily content collections",
                "GET",
                "/moyu/daily-content/collections",
                token=self.user["token"],
                assertion="collected daily content appears in user collection list",
            ),
            "list daily content collections",
        )
        if find_by(collections, "id", content_id) is None:
            raise HarnessFailure("list daily content collections: collected content not found")

        updated_title = f"API Moyu Daily U {RUN_ID[-5:]}"
        self.call_success(
            "update daily content",
            "PUT",
            f"/admin/moyu/daily-content/{content_id}",
            body=self.daily_body(updated_title, content=content + " // updated", difficulty=3),
            token=self.admin_token,
            assertion="admin can update daily content",
        )
        detail = require_dict(
            self.call_success(
                "read updated daily content",
                "GET",
                f"/admin/moyu/daily-content/{content_id}",
                token=self.admin_token,
                assertion="daily content detail reflects update",
            ),
            "read updated daily content",
        )
        if detail.get("title") != updated_title or detail.get("difficultyLevel") != 3:
            raise HarnessFailure("read updated daily content: update was not persisted")

        self.call_success(
            "disable daily content",
            "POST",
            f"/admin/moyu/daily-content/{content_id}/status",
            query={"status": 0},
            token=self.admin_token,
            assertion="admin can disable daily content",
        )
        user_items = require_list(
            self.call_success(
                "user list after daily disable",
                "GET",
                "/moyu/daily-content/type/3",
                query={"limit": 30},
                token=self.user["token"],
                assertion="disabled content is hidden from user type list",
            ),
            "user list after daily disable",
        )
        if find_by(user_items, "id", content_id) is not None:
            raise HarnessFailure("user list after daily disable: disabled content is still visible")
        self.call_success(
            "enable daily content",
            "POST",
            f"/admin/moyu/daily-content/{content_id}/status",
            query={"status": 1},
            token=self.admin_token,
            assertion="admin can re-enable daily content for cleanup",
        )
        self.call_success(
            "uncollect daily content",
            "POST",
            f"/moyu/daily-content/{content_id}/toggle-collection",
            token=self.user["token"],
            assertion="second collection toggle removes daily content collection",
        )
        collections = require_list(
            self.call_success(
                "list daily content collections after uncollect",
                "GET",
                "/moyu/daily-content/collections",
                token=self.user["token"],
                assertion="uncollected daily content no longer appears in collections",
            ),
            "list daily content collections after uncollect",
        )
        if find_by(collections, "id", content_id) is not None:
            raise HarnessFailure("list daily content collections after uncollect: content still collected")

        stats = require_dict(
            self.call_success(
                "daily content statistics",
                "GET",
                "/admin/moyu/daily-content/statistics",
                token=self.admin_token,
                assertion="daily content statistics expose aggregate fields",
            ),
            "daily content statistics",
        )
        for key in ("totalContents", "codeSnippets", "totalViews", "totalLikes"):
            if key not in stats:
                raise HarnessFailure(f"daily content statistics: missing {key}")

        self.call_success(
            "delete daily content",
            "DELETE",
            f"/admin/moyu/daily-content/{content_id}",
            token=self.admin_token,
            assertion="admin can delete created daily content",
        )
        self.call_expected_error(
            "read deleted daily content fails",
            "GET",
            f"/admin/moyu/daily-content/{content_id}",
            token=self.admin_token,
            message_part="内容不存在",
            assertion="deleted daily content is no longer readable",
        )
        self.created_ids.pop("dailyContentId", None)

    def run_calendar_flow(self) -> None:
        assert self.admin_token and self.user.get("token")

        self.call_expected_error(
            "calendar event rejects blank name",
            "POST",
            "/admin/moyu/developer-calendar/events",
            body={**self.event_body("", date="2026-10-24"), "eventName": ""},
            token=self.admin_token,
            message_part="事件名称不能为空",
            assertion="calendar event validates eventName",
        )

        event_name = f"API Moyu Calendar {RUN_ID[-6:]}"
        event_date = "2026-10-24"
        event_mmdd = "10-24"
        self.call_success(
            "create calendar event",
            "POST",
            "/admin/moyu/developer-calendar/events",
            body=self.event_body(event_name, date=event_date),
            token=self.admin_token,
            assertion="admin can create developer calendar event",
        )
        events = require_list(
            self.call_success(
                "admin list calendar events by type",
                "GET",
                "/admin/moyu/developer-calendar/events/type/2",
                token=self.admin_token,
                assertion="admin type list can find created event",
            ),
            "admin list calendar events by type",
        )
        created = find_event_by_name(events, event_name)
        if created is None or not isinstance(created.get("id"), int):
            raise HarnessFailure("admin list calendar events by type: created event not found")
        event_id = created["id"]
        self.created_ids["calendarEventId"] = event_id

        detail = require_dict(
            self.call_success(
                "read created calendar event",
                "GET",
                f"/admin/moyu/developer-calendar/events/{event_id}",
                token=self.admin_token,
                assertion="created event detail echoes core fields",
            ),
            "read created calendar event",
        )
        if detail.get("eventName") != event_name or detail.get("eventDate") != event_mmdd or detail.get("status") != 1:
            raise HarnessFailure("read created calendar event: field mismatch")

        month = require_dict(
            self.call_success(
                "user month calendar",
                "GET",
                "/moyu/developer-calendar/month/2026/10",
                token=self.user["token"],
                assertion="month calendar contains created enabled event",
            ),
            "user month calendar",
        )
        by_date = require_dict(month.get("eventsByDate"), "user month calendar eventsByDate")
        day_events = require_list(by_date.get("24"), "user month calendar day events")
        if find_by(day_events, "id", event_id) is None:
            raise HarnessFailure("user month calendar: created event not found on day 24")
        if "24" not in require_list(month.get("majorEventDates"), "user month calendar major dates"):
            raise HarnessFailure("user month calendar: major date marker missing")

        date_events = require_list(
            self.call_success(
                "user list calendar events by date",
                "GET",
                f"/moyu/developer-calendar/events/{event_date}",
                token=self.user["token"],
                assertion="date event list contains created event",
            ),
            "user list calendar events by date",
        )
        if find_by(date_events, "id", event_id) is None:
            raise HarnessFailure("user list calendar events by date: created event not found")

        self.call_expected_error(
            "calendar invalid date rejected",
            "GET",
            "/moyu/developer-calendar/events/not-a-date",
            token=self.user["token"],
            message_part="日期格式不正确",
            assertion="calendar date endpoint validates yyyy-MM-dd",
        )

        self.call_success(
            "collect calendar event",
            "POST",
            f"/moyu/developer-calendar/events/{event_id}/toggle-collection",
            token=self.user["token"],
            assertion="user can collect calendar event",
        )
        collections = require_list(
            self.call_success(
                "list calendar event collections",
                "GET",
                "/moyu/developer-calendar/collections/events",
                token=self.user["token"],
                assertion="collected event appears in event collections",
            ),
            "list calendar event collections",
        )
        if find_by(collections, "id", event_id) is None:
            raise HarnessFailure("list calendar event collections: collected event not found")

        pref_body = {
            "eventReminder": 0,
            "dailyContentPush": 1,
            "preferredLanguages": ["Java", "Python"],
            "preferredContentTypes": [2, 3],
            "difficultyPreference": 3,
            "notificationTime": "09:30:00",
        }
        self.call_success(
            "save calendar preference",
            "POST",
            "/moyu/developer-calendar/preference",
            body=pref_body,
            token=self.user["token"],
            assertion="user can save calendar preference",
        )
        pref = require_dict(
            self.call_success(
                "read calendar preference",
                "GET",
                "/moyu/developer-calendar/preference",
                token=self.user["token"],
                assertion="preference read-back reflects saved settings",
            ),
            "read calendar preference",
        )
        if pref.get("eventReminder") != 0 or pref.get("dailyContentPush") != 1 or pref.get("difficultyPreference") != 3:
            raise HarnessFailure("read calendar preference: core settings mismatch")
        if "Java" not in str(pref.get("preferredLanguages")) or "3" not in str(pref.get("preferredContentTypes")):
            raise HarnessFailure("read calendar preference: list settings mismatch")

        updated_name = f"API Moyu Calendar U {RUN_ID[-5:]}"
        self.call_success(
            "update calendar event",
            "PUT",
            f"/admin/moyu/developer-calendar/events/{event_id}",
            body=self.event_body(updated_name, date=event_date, is_major=0),
            token=self.admin_token,
            assertion="admin can update calendar event",
        )
        detail = require_dict(
            self.call_success(
                "read updated calendar event",
                "GET",
                f"/admin/moyu/developer-calendar/events/{event_id}",
                token=self.admin_token,
                assertion="event detail reflects update",
            ),
            "read updated calendar event",
        )
        if detail.get("eventName") != updated_name or detail.get("isMajor") != 0:
            raise HarnessFailure("read updated calendar event: update was not persisted")

        self.call_success(
            "disable calendar event",
            "POST",
            f"/admin/moyu/developer-calendar/events/{event_id}/status",
            query={"status": 0},
            token=self.admin_token,
            assertion="admin can disable event",
        )
        date_events = require_list(
            self.call_success(
                "user date events after disable",
                "GET",
                f"/moyu/developer-calendar/events/{event_date}",
                token=self.user["token"],
                assertion="disabled event is hidden from user date list",
            ),
            "user date events after disable",
        )
        if find_by(date_events, "id", event_id) is not None:
            raise HarnessFailure("user date events after disable: disabled event is still visible")
        self.call_success(
            "enable calendar event",
            "POST",
            f"/admin/moyu/developer-calendar/events/{event_id}/status",
            query={"status": 1},
            token=self.admin_token,
            assertion="admin can re-enable event for cleanup",
        )
        self.call_success(
            "uncollect calendar event",
            "POST",
            f"/moyu/developer-calendar/events/{event_id}/toggle-collection",
            token=self.user["token"],
            assertion="second event collection toggle removes collection",
        )
        collections = require_list(
            self.call_success(
                "list calendar event collections after uncollect",
                "GET",
                "/moyu/developer-calendar/collections/events",
                token=self.user["token"],
                assertion="uncollected event no longer appears in collections",
            ),
            "list calendar event collections after uncollect",
        )
        if find_by(collections, "id", event_id) is not None:
            raise HarnessFailure("list calendar event collections after uncollect: event still collected")

        stats = require_dict(
            self.call_success(
                "calendar event statistics",
                "GET",
                "/admin/moyu/developer-calendar/events/statistics",
                token=self.admin_token,
                assertion="calendar statistics expose aggregate fields",
            ),
            "calendar event statistics",
        )
        for key in ("totalEvents", "techMemorials", "majorEvents"):
            if key not in stats:
                raise HarnessFailure(f"calendar event statistics: missing {key}")

        self.call_success(
            "delete calendar event",
            "DELETE",
            f"/admin/moyu/developer-calendar/events/{event_id}",
            token=self.admin_token,
            assertion="admin can delete created event",
        )
        self.call_expected_error(
            "read deleted calendar event fails",
            "GET",
            f"/admin/moyu/developer-calendar/events/{event_id}",
            token=self.admin_token,
            message_part="事件不存在",
            assertion="deleted event is no longer readable",
        )
        self.created_ids.pop("calendarEventId", None)

    def run_salary_flow(self) -> None:
        assert self.user.get("token")
        token = self.user["token"]

        self.call_expected_error(
            "work start without salary config fails",
            "POST",
            "/moyu/salary-calculator/work-time",
            body={"action": "START", "remark": "no config"},
            token=token,
            message_part="请先配置薪资信息",
            assertion="work-time action requires salary config",
        )

        self.call_expected_error(
            "salary config rejects low salary",
            "POST",
            "/moyu/salary-calculator/config",
            body={"monthlySalary": 999, "workDaysPerMonth": 22, "workHoursPerDay": 8},
            token=token,
            message_part="月薪不能低于1000元",
            assertion="salary config validates monthly salary floor",
        )

        config_body = {"monthlySalary": 22000, "workDaysPerMonth": 22, "workHoursPerDay": 8}
        self.call_success(
            "save salary config",
            "POST",
            "/moyu/salary-calculator/config",
            body=config_body,
            token=token,
            assertion="user can save salary config",
        )
        config = require_dict(
            self.call_success(
                "read salary config",
                "GET",
                "/moyu/salary-calculator/config",
                token=token,
                assertion="salary config read-back reflects saved values",
            ),
            "read salary config",
        )
        if number_value(config.get("monthlySalary"), "read salary config monthlySalary") != Decimal("22000"):
            raise HarnessFailure("read salary config: monthly salary mismatch")
        if config.get("workDaysPerMonth") != 22 or number_value(config.get("workHoursPerDay"), "read salary config hours") != Decimal("8"):
            raise HarnessFailure("read salary config: day/hour settings mismatch")

        data = require_dict(
            self.call_success(
                "read salary calculator data",
                "GET",
                "/moyu/salary-calculator/data",
                token=token,
                assertion="salary calculator derives hourly rate",
            ),
            "read salary calculator data",
        )
        expected_hourly = Decimal("125.00")
        actual_hourly = number_value(data.get("hourlyRate"), "read salary calculator data hourlyRate")
        if actual_hourly != expected_hourly:
            raise HarnessFailure(f"read salary calculator data: expected hourlyRate {expected_hourly}, got {actual_hourly}")

        started = require_dict(
            self.call_success(
                "start work",
                "POST",
                "/moyu/salary-calculator/work-time",
                body={"action": "START", "remark": f"probe {RUN_ID}"},
                token=token,
                assertion="START moves work status to in-progress",
            ),
            "start work",
        )
        if started.get("workStatus") != 1 or not started.get("todayStartTime"):
            raise HarnessFailure("start work: workStatus/startTime not updated")

        paused = require_dict(
            self.call_success(
                "pause work",
                "POST",
                "/moyu/salary-calculator/work-time",
                body={"action": "PAUSE"},
                token=token,
                assertion="PAUSE moves work status to paused",
            ),
            "pause work",
        )
        if paused.get("workStatus") != 2:
            raise HarnessFailure("pause work: workStatus should be 2")

        resumed = require_dict(
            self.call_success(
                "resume work",
                "POST",
                "/moyu/salary-calculator/work-time",
                body={"action": "RESUME"},
                token=token,
                assertion="RESUME moves work status back to in-progress",
            ),
            "resume work",
        )
        if resumed.get("workStatus") != 1:
            raise HarnessFailure("resume work: workStatus should be 1")

        ended = require_dict(
            self.call_success(
                "end work",
                "POST",
                "/moyu/salary-calculator/work-time",
                body={"action": "END"},
                token=token,
                assertion="END completes today's work record",
            ),
            "end work",
        )
        if ended.get("workStatus") != 3:
            raise HarnessFailure("end work: workStatus should be 3")
        if number_value(ended.get("todayWorkHours"), "end work todayWorkHours") < Decimal("0"):
            raise HarnessFailure("end work: todayWorkHours should not be negative")

        self.call_expected_error(
            "unsupported work action rejected",
            "POST",
            "/moyu/salary-calculator/work-time",
            body={"action": "FLY"},
            token=token,
            message_part="不支持的操作类型",
            assertion="work-time action validates action enum behavior",
        )

        self.call_success(
            "delete salary config",
            "DELETE",
            "/moyu/salary-calculator/config",
            token=token,
            assertion="user can delete salary config",
        )
        config = self.call_success(
            "read salary config after delete",
            "GET",
            "/moyu/salary-calculator/config",
            token=token,
            assertion="deleted salary config reads back as null",
        )
        if config is not None:
            raise HarnessFailure("read salary config after delete: expected null config")

    def run_hot_topic_flow(self) -> None:
        categories = require_dict(
            self.call_success(
                "hot topic categories",
                "GET",
                "/moyu/hot-topic/categories",
                assertion="hot topic categories return local structured platform catalog",
            ),
            "hot topic categories",
        )
        all_types = require_dict(categories.get("allTypes"), "hot topic categories allTypes")
        category_list = require_list(categories.get("categories"), "hot topic categories list")
        if categories.get("total") != len(all_types):
            raise HarnessFailure("hot topic categories: total does not match allTypes size")
        for platform in ("weibo", "zhihu"):
            if platform not in all_types:
                raise HarnessFailure(f"hot topic categories: missing platform {platform}")
        if not category_list or not all(isinstance(item, dict) and item.get("apis") for item in category_list):
            raise HarnessFailure("hot topic categories: categories must include api mappings")

        data = self.call_success(
            "hot topic weibo data",
            "GET",
            "/moyu/hot-topic/data/weibo",
            assertion="specific platform data endpoint returns success or null degraded payload",
            timeout=12,
        )
        if data is not None:
            platform_data = require_dict(data, "hot topic weibo data")
            if not platform_data.get("name") and not platform_data.get("title"):
                raise HarnessFailure("hot topic weibo data: returned object lacks name/title")
            if platform_data.get("data") is not None and not isinstance(platform_data.get("data"), list):
                raise HarnessFailure("hot topic weibo data: data field should be a list when present")

        all_data = require_dict(
            self.call_success(
                "hot topic all data",
                "GET",
                "/moyu/hot-topic/data/all",
                assertion="aggregate hot-topic endpoint returns a platform map even under external degradation",
                timeout=15,
            ),
            "hot topic all data",
        )
        if not isinstance(all_data, dict):
            raise HarnessFailure("hot topic all data: expected map")

        self.call_success(
            "hot topic refresh",
            "POST",
            "/moyu/hot-topic/refresh",
            assertion="manual refresh triggers without blocking API success",
            timeout=8,
        )

    def cleanup(self) -> None:
        admin_token = self.admin_token
        bug_id = self.created_ids.get("bugId")
        daily_id = self.created_ids.get("dailyContentId")
        event_id = self.created_ids.get("calendarEventId")
        if bug_id and admin_token:
            try:
                self.call_success(
                    "cleanup delete bug",
                    "DELETE",
                    f"/admin/moyu/bug-store/{bug_id}",
                    token=admin_token,
                    assertion="cleanup removes bug created by harness",
                )
            except Exception:
                pass
        if daily_id and admin_token:
            try:
                self.call_success(
                    "cleanup delete daily content",
                    "DELETE",
                    f"/admin/moyu/daily-content/{daily_id}",
                    token=admin_token,
                    assertion="cleanup removes daily content created by harness",
                )
            except Exception:
                pass
        if event_id and admin_token:
            try:
                self.call_success(
                    "cleanup delete calendar event",
                    "DELETE",
                    f"/admin/moyu/developer-calendar/events/{event_id}",
                    token=admin_token,
                    assertion="cleanup removes calendar event created by harness",
                )
            except Exception:
                pass

    def run(self) -> dict[str, Any]:
        failure: str | None = None
        try:
            self.admin_login()
            self.user = self.register_and_login_user()
            self.context["userId"] = self.user["id"]

            self.run_bug_store_flow()
            self.run_daily_content_flow()
            self.run_calendar_flow()
            self.run_salary_flow()
            self.run_hot_topic_flow()
        except Exception as exc:
            failure = f"{type(exc).__name__}: {truncate(exc, 900)}"
            raise
        finally:
            try:
                self.cleanup()
            finally:
                write_json(os.path.join(self.out_dir, "steps.json"), self.results)
                write_json(os.path.join(self.out_dir, "context.json"), self.sanitized_context())
                write_json(os.path.join(self.out_dir, "summary.json"), self.summary(failure))
        return self.summary(failure)

    def sanitized_context(self) -> dict[str, Any]:
        context = dict(self.context)
        if self.user:
            context["user"] = {k: v for k, v in self.user.items() if k != "token"}
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
            "service": "xiaou-moyu",
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
    parser = argparse.ArgumentParser(description="Run xiaou-moyu service correctness checks")
    parser.add_argument("--base-url", default="http://127.0.0.1:9999/api")
    parser.add_argument("--out-dir", required=True)
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    harness = MoyuCorrectnessHarness(args.base_url, args.out_dir)
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
