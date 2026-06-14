#!/usr/bin/env python3
"""Service-level correctness harness for points and lottery APIs.

The checks are stricter than HTTP smoke coverage:
- user/admin success paths assert balance, detail, record, and statistics readbacks.
- expected auth, validation, and business errors are counted separately.
- lottery checks assert accounting consistency using the actual returned prize.
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


class PointsLotteryCorrectnessHarness:
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
                assertion="seed admin can log in for points and lottery management",
            ),
            "admin login",
        )
        token = login.get("accessToken")
        if not isinstance(token, str) or not token:
            raise HarnessFailure("admin login: missing accessToken")
        self.admin_token = token

    def register_and_login_user(self, suffix: str) -> dict[str, Any]:
        username = f"pts{suffix}{RUN_ID[-8:]}"
        phone_prefix = "139" if suffix == "a" else "158"
        phone = phone_prefix + RUN_ID[-8:]

        key, code = generate_captcha(self.base_url)
        registered = require_dict(
            self.call_success(
                f"register points user {suffix}",
                "POST",
                "/user/auth/register",
                body={
                    "username": username,
                    "password": TEST_PASSWORD,
                    "confirmPassword": TEST_PASSWORD,
                    "nickname": f"Points User {suffix.upper()} {RUN_ID[-4:]}",
                    "email": f"{username}@example.com",
                    "phone": phone,
                    "captcha": code,
                    "captchaKey": key,
                },
                assertion="new isolated user can be registered for points flow",
            ),
            f"register points user {suffix}",
        )
        user_id = registered.get("id")
        if not isinstance(user_id, int):
            raise HarnessFailure(f"register points user {suffix}: missing numeric user id")

        key, code = generate_captcha(self.base_url)
        login = require_dict(
            self.call_success(
                f"login points user {suffix}",
                "POST",
                "/user/auth/login",
                body={
                    "username": username,
                    "password": TEST_PASSWORD,
                    "captcha": code,
                    "captchaKey": key,
                    "rememberMe": False,
                },
                assertion="registered user can obtain access token",
            ),
            f"login points user {suffix}",
        )
        token = login.get("accessToken")
        if not isinstance(token, str) or not token:
            raise HarnessFailure(f"login points user {suffix}: missing accessToken")
        return {"id": user_id, "username": username, "phone": phone, "token": token}

    def assert_balance(self, token: str, expected: int, step: str) -> dict[str, Any]:
        balance = require_dict(
            self.call_success(
                step,
                "GET",
                "/user/points/balance",
                token=token,
                assertion=f"points balance readback equals {expected}",
            ),
            step,
        )
        if balance.get("totalPoints") != expected:
            raise HarnessFailure(f"{step}: expected totalPoints {expected}, got {balance.get('totalPoints')!r}")
        return balance

    def wait_for_admin_monitor_draw_count(self, before_count: int, token: str) -> dict[str, Any]:
        last_monitor: dict[str, Any] | None = None
        for _ in range(8):
            monitor = require_dict(
                self.call_success(
                    "admin lottery realtime monitor",
                    "GET",
                    "/admin/lottery/monitor/realtime",
                    token=token,
                    assertion="realtime monitor exposes lottery overview and prize status after draw",
                ),
                "admin lottery realtime monitor",
            )
            last_monitor = monitor
            overview = require_dict(monitor.get("todayOverview"), "realtime monitor todayOverview")
            count = overview.get("totalDrawCount")
            if isinstance(count, int) and count >= before_count + 1:
                return monitor
            time.sleep(0.35)
        raise HarnessFailure("realtime monitor: totalDrawCount did not advance after draw")

    def create_or_find_test_prize(self) -> int:
        if not self.admin_token:
            raise HarnessFailure("admin token missing")
        name = f"API Correct Prize {RUN_ID[-6:]}"
        self.call_success(
            "create lottery prize fixture",
            "POST",
            "/admin/lottery/prize/save",
            body={
                "prizeName": name,
                "prizeLevel": 6,
                "prizePoints": 120,
                "baseProbability": "1.00000000",
                "currentProbability": "1.00000000",
                "targetReturnRate": "1.2000",
                "maxReturnRate": "2.0000",
                "minReturnRate": "0.0000",
                "dailyStock": -1,
                "totalStock": -1,
                "displayOrder": 999,
                "prizeIcon": "",
                "prizeDesc": "service correctness fixture",
                "adjustStrategy": "FIXED",
                "isActive": 1,
            },
            token=self.admin_token,
            assertion="admin can create a lottery prize fixture",
        )
        prizes = require_list(
            self.call_success(
                "admin list prize configs after fixture",
                "GET",
                "/admin/lottery/prize/list",
                token=self.admin_token,
                assertion="created lottery prize is visible in admin config list",
            ),
            "admin list prize configs after fixture",
        )
        created = find_by(prizes, "prizeName", name)
        if not created or not isinstance(created.get("id"), int):
            raise HarnessFailure("lottery prize fixture: created prize missing from admin list")
        prize_id = created["id"]
        self.created_ids["lotteryPrizeId"] = prize_id
        return prize_id

    def run(self) -> dict[str, Any]:
        failure: str | None = None
        try:
            self.admin_login()
            self.primary_user = self.register_and_login_user("a")
            self.secondary_user = self.register_and_login_user("b")
            if not self.admin_token:
                raise HarnessFailure("admin token missing after login")

            user_id = self.primary_user["id"]
            user_token = self.primary_user["token"]
            secondary_id = self.secondary_user["id"]

            self.context["primaryUserId"] = user_id
            self.context["secondaryUserId"] = secondary_id

            self.call_expected_error(
                "points balance requires user auth",
                "GET",
                "/user/points/balance",
                message_part="Token",
                assertion="anonymous user cannot read points balance",
            )

            self.assert_balance(user_token, 0, "initial points balance")

            checkin = require_dict(
                self.call_success(
                    "daily points checkin",
                    "POST",
                    "/user/points/checkin",
                    token=user_token,
                    assertion="first checkin grants expected base points and updates account",
                ),
                "daily points checkin",
            )
            if checkin.get("pointsEarned") != 50 or checkin.get("continuousDays") != 1:
                raise HarnessFailure("daily points checkin: expected 50 points and continuousDays=1 for new user")
            if checkin.get("totalBalance") != 50:
                raise HarnessFailure("daily points checkin: totalBalance should be 50")
            balance = self.assert_balance(user_token, 50, "balance after checkin")
            if balance.get("todayCheckedIn") is not True or balance.get("todayPoints") != 0:
                raise HarnessFailure("balance after checkin: todayCheckedIn/todayPoints mismatch")

            self.call_expected_error(
                "duplicate checkin returns business error",
                "POST",
                "/user/points/checkin",
                token=user_token,
                message_part="今日已打卡",
                assertion="same user cannot check in twice in one day",
            )

            user_details = page_records(
                self.call_success(
                    "user detail list after checkin",
                    "POST",
                    "/user/points/detail",
                    body={"pageNum": 1, "pageSize": 10},
                    token=user_token,
                    assertion="user points detail includes checkin record",
                ),
                "user detail list after checkin",
            )
            checkin_detail = next(
                (
                    item
                    for item in user_details
                    if isinstance(item, dict)
                    and item.get("pointsType") == 2
                    and item.get("pointsChange") == 50
                    and item.get("balanceAfter") == 50
                ),
                None,
            )
            if not checkin_detail:
                raise HarnessFailure("user detail list: checkin detail missing")

            calendar = require_dict(
                self.call_success(
                    "checkin calendar readback",
                    "POST",
                    "/user/points/checkin-calendar",
                    body={},
                    token=user_token,
                    assertion="checkin calendar marks today's checkin",
                ),
                "checkin calendar readback",
            )
            today = dt.datetime.now().day
            if today not in require_list(calendar.get("checkinDays"), "checkin calendar checkinDays"):
                raise HarnessFailure("checkin calendar: today should be in checkinDays")
            if calendar.get("totalCheckinDays") != 1 or calendar.get("continuousDays") != 1:
                raise HarnessFailure("checkin calendar: total/continuous days mismatch")

            checkin_stats = require_dict(
                self.call_success(
                    "checkin statistics readback",
                    "POST",
                    "/user/points/checkin-statistics",
                    body={"months": 1},
                    token=user_token,
                    assertion="checkin statistics reflect first checkin",
                ),
                "checkin statistics readback",
            )
            if checkin_stats.get("totalCheckinDays") < 1 or checkin_stats.get("totalPointsFromCheckin") < 50:
                raise HarnessFailure("checkin statistics: totals should include first checkin")

            grant_reason = f"correctness grant {RUN_ID[-6:]}"
            grant = require_dict(
                self.call_success(
                    "admin grant points",
                    "POST",
                    "/admin/points/grant",
                    body={"userId": user_id, "points": 250, "reason": grant_reason},
                    token=self.admin_token,
                    assertion="admin grant updates target user's balance and returns detail id",
                ),
                "admin grant points",
            )
            if not isinstance(grant.get("detailId"), int) or grant.get("userBalance") != 300:
                raise HarnessFailure("admin grant points: expected detailId and userBalance=300")
            self.assert_balance(user_token, 300, "balance after admin grant")

            batch_reason = f"batch grant {RUN_ID[-6:]}"
            batch = require_dict(
                self.call_success(
                    "admin batch grant points",
                    "POST",
                    "/admin/points/batch-grant",
                    body={"userIds": [user_id, secondary_id], "points": 20, "reason": batch_reason},
                    token=self.admin_token,
                    assertion="batch grant updates both users and reports per-user success",
                ),
                "admin batch grant points",
            )
            if batch.get("successCount") != 2 or batch.get("failCount") != 0 or batch.get("totalPointsGranted") != 40:
                raise HarnessFailure("admin batch grant: success/fail/total mismatch")
            self.assert_balance(user_token, 320, "balance after batch grant primary")

            admin_details = page_records(
                self.call_success(
                    "admin detail list for user",
                    "POST",
                    "/admin/points/detail-list",
                    body={"userId": user_id, "pageNum": 1, "pageSize": 10},
                    token=self.admin_token,
                    assertion="admin detail list includes grant and checkin records for target user",
                ),
                "admin detail list for user",
            )
            if not any(isinstance(row, dict) and row.get("description") == grant_reason and row.get("pointsChange") == 250 for row in admin_details):
                raise HarnessFailure("admin detail list: grant detail missing")
            if not any(isinstance(row, dict) and row.get("description") == batch_reason and row.get("pointsChange") == 20 for row in admin_details):
                raise HarnessFailure("admin detail list: batch grant detail missing")

            user_list = page_records(
                self.call_success(
                    "admin user points list",
                    "POST",
                    "/admin/points/user-list",
                    body={"pageNum": 1, "pageSize": 20, "minPoints": 300, "orderBy": "points", "orderDirection": "desc"},
                    token=self.admin_token,
                    assertion="admin points ranking includes target user with updated balance",
                ),
                "admin user points list",
            )
            user_row = find_by(user_list, "userId", user_id)
            if not user_row or user_row.get("totalPoints") != 320:
                raise HarnessFailure("admin user points list: target user balance missing")

            user_info = require_dict(
                self.call_success(
                    "admin user points info",
                    "GET",
                    f"/admin/points/user-info/{user_id}",
                    token=self.admin_token,
                    assertion="admin user points info reads updated balance and checkin status",
                ),
                "admin user points info",
            )
            if user_info.get("userId") != user_id or user_info.get("totalPoints") != 320 or user_info.get("hasCheckedToday") is not True:
                raise HarnessFailure("admin user points info: userId/totalPoints/hasCheckedToday mismatch")

            points_stats = require_dict(
                self.call_success(
                    "admin points statistics",
                    "GET",
                    "/admin/points/statistics",
                    token=self.admin_token,
                    assertion="admin points statistics include checkin and grant totals",
                ),
                "admin points statistics",
            )
            if points_stats.get("checkinPointsSum", 0) < 50 or points_stats.get("adminGrantPointsSum", 0) < 270:
                raise HarnessFailure("admin points statistics: expected checkin/admin grant totals to include this run")

            self.call_expected_error(
                "admin grant validates points minimum",
                "POST",
                "/admin/points/grant",
                body={"userId": user_id, "points": 0, "reason": "invalid"},
                token=self.admin_token,
                message_part="积分数量最小为1",
                assertion="admin grant rejects zero points",
            )

            self.call_expected_error(
                "admin grant rejects missing user",
                "POST",
                "/admin/points/grant",
                body={"userId": 999999999, "points": 1, "reason": "missing user probe"},
                token=self.admin_token,
                message_part="用户不存在",
                assertion="admin grant must not create points account for nonexistent user",
            )

            prize_id = self.create_or_find_test_prize()
            user_prizes = require_list(
                self.call_success(
                    "user lottery prize list",
                    "GET",
                    "/user/lottery/prizes",
                    token=user_token,
                    assertion="user prize list exposes active configured prizes",
                ),
                "user lottery prize list",
            )
            if not find_by(user_prizes, "prizeId", prize_id):
                raise HarnessFailure("user lottery prize list: created fixture prize missing")

            remaining_before = self.call_success(
                "lottery remaining count before draw",
                "GET",
                "/user/lottery/remaining-count",
                token=user_token,
                assertion="new user has available lottery draws before draw",
            )
            if not isinstance(remaining_before, int) or remaining_before <= 0:
                raise HarnessFailure("lottery remaining before draw: expected positive count")

            user_lottery_stats_before = require_dict(
                self.call_success(
                    "user lottery statistics before draw",
                    "GET",
                    "/user/lottery/statistics",
                    token=user_token,
                    assertion="user lottery statistics initializes draw counters",
                ),
                "user lottery statistics before draw",
            )
            before_draw_count = user_lottery_stats_before.get("totalDrawCount")
            if not isinstance(before_draw_count, int):
                raise HarnessFailure("user lottery statistics before draw: missing totalDrawCount")

            monitor_before = require_dict(
                self.call_success(
                    "admin lottery monitor before draw",
                    "GET",
                    "/admin/lottery/monitor/realtime",
                    token=self.admin_token,
                    assertion="admin lottery monitor is readable before draw",
                ),
                "admin lottery monitor before draw",
            )
            overview_before = require_dict(monitor_before.get("todayOverview"), "monitor before overview")
            admin_draw_count_before = overview_before.get("totalDrawCount")
            if not isinstance(admin_draw_count_before, int):
                raise HarnessFailure("monitor before draw: missing totalDrawCount")

            balance_before_draw_data = require_dict(
                self.call_success(
                    "balance before lottery draw",
                    "GET",
                    "/user/points/balance",
                    token=user_token,
                    assertion="balance before draw is captured for accounting",
                ),
                "balance before lottery draw",
            )
            balance_before_draw = balance_before_draw_data.get("totalPoints")
            if not isinstance(balance_before_draw, int) or balance_before_draw < 100:
                raise HarnessFailure("balance before lottery draw: expected at least draw cost")

            draw = require_dict(
                self.call_success(
                    "user lottery draw",
                    "POST",
                    "/user/lottery/draw",
                    body={"strategyType": "ALIAS_METHOD"},
                    token=user_token,
                    assertion="lottery draw deducts cost, awards returned prize, and writes draw record",
                ),
                "user lottery draw",
            )
            record_id = draw.get("recordId")
            prize_points = draw.get("prizePoints")
            if not isinstance(record_id, int) or not isinstance(draw.get("prizeId"), int):
                raise HarnessFailure("lottery draw: missing recordId/prizeId")
            if not isinstance(prize_points, int):
                raise HarnessFailure("lottery draw: missing prizePoints")
            self.created_ids["lotteryRecordId"] = record_id

            expected_balance_after_draw = balance_before_draw - 100 + prize_points
            self.assert_balance(user_token, expected_balance_after_draw, "balance after lottery draw")

            remaining_after = self.call_success(
                "lottery remaining count after draw",
                "GET",
                "/user/lottery/remaining-count",
                token=user_token,
                assertion="remaining draw count decreases after successful draw",
            )
            if remaining_after != max(0, remaining_before - 1):
                raise HarnessFailure("lottery remaining count: expected decrement by one")

            draw_records = page_records(
                self.call_success(
                    "user lottery records after draw",
                    "POST",
                    "/user/lottery/records",
                    body={"page": 1, "size": 10},
                    token=user_token,
                    assertion="user draw records include new draw with returned prize",
                ),
                "user lottery records after draw",
            )
            record = find_by(draw_records, "recordId", record_id)
            if not record or record.get("userId") != user_id or record.get("prizePoints") != prize_points:
                raise HarnessFailure("user lottery records: created draw record missing or mismatched")

            lottery_stats_after = require_dict(
                self.call_success(
                    "user lottery statistics after draw",
                    "GET",
                    "/user/lottery/statistics",
                    token=user_token,
                    assertion="user lottery statistics increments after draw",
                ),
                "user lottery statistics after draw",
            )
            if lottery_stats_after.get("totalDrawCount") != before_draw_count + 1:
                raise HarnessFailure("user lottery statistics: totalDrawCount should increment by one")

            points_after_draw = page_records(
                self.call_success(
                    "user points details after lottery draw",
                    "POST",
                    "/user/points/detail",
                    body={"pageNum": 1, "pageSize": 20},
                    token=user_token,
                    assertion="points details include lottery cost and reward entries",
                ),
                "user points details after lottery draw",
            )
            if not any(isinstance(row, dict) and row.get("pointsType") == 3 and row.get("pointsChange") == -100 for row in points_after_draw):
                raise HarnessFailure("points details after draw: lottery cost entry missing")
            if prize_points > 0 and not any(isinstance(row, dict) and row.get("pointsType") == 4 and row.get("pointsChange") == prize_points for row in points_after_draw):
                raise HarnessFailure("points details after draw: lottery reward entry missing")

            monitor_after = self.wait_for_admin_monitor_draw_count(admin_draw_count_before, self.admin_token)
            prize_status = require_list(monitor_after.get("prizeStatusList"), "monitor after prizeStatusList")
            if not find_by(prize_status, "prizeId", draw.get("prizeId")):
                raise HarnessFailure("monitor after draw: drawn prize missing from prize status list")

            admin_records = page_records(
                self.call_success(
                    "admin lottery records after draw",
                    "POST",
                    "/admin/lottery/records",
                    body={"userId": user_id, "page": 1, "size": 10},
                    token=self.admin_token,
                    assertion="admin lottery records include user draw",
                ),
                "admin lottery records after draw",
            )
            if not find_by(admin_records, "recordId", record_id):
                raise HarnessFailure("admin lottery records: created draw record missing")

            self.call_expected_error(
                "second lottery draw hits cooldown",
                "POST",
                "/user/lottery/draw",
                body={"strategyType": "ALIAS_METHOD"},
                token=user_token,
                message_part="等待",
                assertion="immediate second draw should be blocked by cooldown",
            )

            self.call_success(
                "admin reset lottery limit",
                "POST",
                "/admin/lottery/user/reset-limit",
                query={"userId": user_id},
                token=self.admin_token,
                assertion="admin can reset lottery limit record created by draw",
            )
            reset_stats = require_dict(
                self.call_success(
                    "user lottery statistics after reset",
                    "GET",
                    "/user/lottery/statistics",
                    token=user_token,
                    assertion="user lottery statistics reflects reset of daily counters",
                ),
                "user lottery statistics after reset",
            )
            if reset_stats.get("todayDrawCount") != 0 or reset_stats.get("todayWinCount") != 0:
                raise HarnessFailure("lottery statistics after reset: daily counters should be zero")

            self.call_success(
                "admin blacklist lottery user",
                "POST",
                "/admin/lottery/user/blacklist",
                query={"userId": user_id, "isBlacklist": "true"},
                token=self.admin_token,
                assertion="admin can blacklist a user with an existing lottery limit record",
            )
            self.call_expected_error(
                "blacklisted user cannot draw",
                "POST",
                "/user/lottery/draw",
                body={"strategyType": "ALIAS_METHOD"},
                token=user_token,
                message_part="限制",
                assertion="lottery draw is blocked for blacklisted user",
            )
            self.call_success(
                "admin remove lottery blacklist",
                "POST",
                "/admin/lottery/user/blacklist",
                query={"userId": user_id, "isBlacklist": "false"},
                token=self.admin_token,
                assertion="admin can remove lottery blacklist after check",
            )

            self.call_expected_error(
                "lottery invalid strategy returns business error",
                "POST",
                "/user/lottery/draw",
                body={"strategyType": "UNKNOWN_STRATEGY"},
                token=self.secondary_user["token"],
                message_part="积分不足",
                assertion="lottery draw first rejects users without enough points",
            )

        except Exception:
            failure = traceback.format_exc()
            raise
        finally:
            summary = self.summary(failure)
            write_json(os.path.join(self.out_dir, "context.json"), self.sanitized_context())
            write_json(os.path.join(self.out_dir, "results.json"), self.results)
            write_json(os.path.join(self.out_dir, "summary.json"), summary)
        return summary

    def sanitized_context(self) -> dict[str, Any]:
        context = dict(self.context)
        if self.primary_user:
            context["primaryUser"] = {k: v for k, v in self.primary_user.items() if k != "token"}
        if self.secondary_user:
            context["secondaryUser"] = {k: v for k, v in self.secondary_user.items() if k != "token"}
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
            "service": "points-lottery-controller",
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
    parser = argparse.ArgumentParser(description="Run points/lottery service correctness checks")
    parser.add_argument("--base-url", default="http://127.0.0.1:9999/api")
    parser.add_argument("--out-dir", required=True)
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    harness = PointsLotteryCorrectnessHarness(args.base_url, args.out_dir)
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
