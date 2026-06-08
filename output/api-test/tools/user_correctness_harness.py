#!/usr/bin/env python3
"""Service-level correctness harness for user/auth/admin-user APIs.

This harness is stricter than HTTP smoke coverage:
- HTTP 200 alone is not success; business code and response shape are asserted.
- success-path writes are read back through the service API.
- expected auth, validation, and business errors are counted separately.
- tokens, passwords, and captcha values are used only in memory and are never
  written to output files.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import traceback
import urllib.parse
from typing import Any

from team_correctness_harness import (
    SUCCESS_CODE,
    TEST_PASSWORD,
    HarnessFailure,
    assert_success_http,
    captcha_code_from_logs,
    captcha_code_from_redis,
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
NEW_PASSWORD = "ApiNew123456"
RESET_PASSWORD = "ApiReset123456"
CROSS_PASSWORD = "ApiCross123456"
SENSITIVE_QUERY_KEYS = {"password", "oldPassword", "newPassword", "captcha", "captchaKey"}


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


def assert_expected_error(
    http: Any,
    step: str,
    *,
    message_part: str | None = None,
    allowed_codes: set[int] | None = None,
) -> None:
    if not http.ok:
        raise HarnessFailure(f"{step}: transport error during expected business error: {http.error}")
    if http.status is None or http.status >= 500:
        raise HarnessFailure(f"{step}: expected business error, got HTTP status={http.status}")
    code = result_code(http.json_body)
    message = result_message(http.json_body, http.text)
    if code == SUCCESS_CODE:
        raise HarnessFailure(f"{step}: expected business error but got success")
    if allowed_codes and code not in allowed_codes:
        raise HarnessFailure(f"{step}: expected code in {sorted(allowed_codes)}, got {code}, message={message}")
    if message_part and message_part not in message:
        raise HarnessFailure(f"{step}: expected message containing {message_part!r}, got {message!r}")


def assert_user_fields(user: dict[str, Any], expected: dict[str, Any], step: str) -> None:
    user_id = user.get("id")
    if not isinstance(user_id, int):
        raise HarnessFailure(f"{step}: missing numeric id")
    if "password" in user:
        raise HarnessFailure(f"{step}: response must not contain password field")
    for key, value in expected.items():
        if user.get(key) != value:
            raise HarnessFailure(f"{step}: expected {key}={value!r}, got {user.get(key)!r}")
    status = user.get("status")
    if status not in (0, 1):
        raise HarnessFailure(f"{step}: expected active/disabled status, got {status!r}")


def find_by_id(items: list[Any], user_id: int) -> dict[str, Any] | None:
    for item in items:
        if isinstance(item, dict) and item.get("id") == user_id:
            return item
    return None


class UserCorrectnessHarness:
    def __init__(self, base_url: str, out_dir: str):
        self.base_url = base_url.rstrip("/")
        self.out_dir = out_dir
        self.results: list[dict[str, Any]] = []
        self.context: dict[str, Any] = {"runId": RUN_ID, "baseUrl": self.base_url}
        self.admin_token: str | None = None
        self.primary: dict[str, Any] = {}
        self.secondary: dict[str, Any] = {}
        self.admin_created: dict[str, Any] = {}
        self.batch_created: dict[str, Any] = {}
        self.soft_failures: list[str] = []

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
        message_part: str | None = None,
        allowed_codes: set[int] | None = None,
        query: dict[str, Any] | None = None,
        body: Any = None,
        token: str | None = None,
        assertion: str = "expected business error",
        fail_fast: bool = True,
    ) -> bool:
        http = http_request(self.base_url, method, path, query=query, body=body, token=token)
        try:
            assert_expected_error(http, f"{method} {path}", message_part=message_part, allowed_codes=allowed_codes)
            self.record(
                name,
                method,
                self.path_with_query(path, query),
                http,
                assertion,
                "PASS_EXPECTED_ERROR",
                expected="BUSINESS_ERROR",
                details={"expectedMessagePart": message_part, "allowedCodes": sorted(allowed_codes or [])},
            )
            return True
        except Exception as exc:
            self.record(
                name,
                method,
                self.path_with_query(path, query),
                http,
                assertion,
                "FAIL",
                expected="BUSINESS_ERROR",
                details={"expectedMessagePart": message_part, "allowedCodes": sorted(allowed_codes or [])},
            )
            if fail_fast:
                raise
            self.soft_failures.append(f"{name}: {exc}")
            return False

    @staticmethod
    def path_with_query(path: str, query: dict[str, Any] | None) -> str:
        if not query:
            return path
        safe_query = {
            key: ("[REDACTED]" if key in SENSITIVE_QUERY_KEYS else value)
            for key, value in query.items()
        }
        return path + "?" + urllib.parse.urlencode(safe_query, doseq=True)

    def generate_captcha_recorded(self, name: str) -> tuple[str, str]:
        http = http_request(self.base_url, "GET", "/captcha/generate", timeout=8)
        try:
            assert_success_http(http, "GET /captcha/generate")
            data = require_dict(response_data(http.json_body), name)
            key = data.get("captchaKey")
            image = data.get("captchaImage")
            expires = data.get("expiresIn")
            if not isinstance(key, str) or not key:
                raise HarnessFailure(f"{name}: captchaKey missing")
            if not isinstance(image, str) or not image.startswith("data:image/png;base64,"):
                raise HarnessFailure(f"{name}: captchaImage should be a png data URL")
            if not isinstance(expires, int) or expires <= 0:
                raise HarnessFailure(f"{name}: expiresIn should be positive integer")
            code = captcha_code_from_logs(key) or captcha_code_from_redis(key)
            if not code:
                raise HarnessFailure(f"{name}: captcha code could not be acquired from logs or Redis")
            self.record(name, "GET", "/captcha/generate", http, "captcha response has key, image, and expiry", "PASS")
            return key, code
        except Exception:
            self.record(name, "GET", "/captcha/generate", http, "captcha response has key, image, and expiry", "FAIL")
            raise

    def admin_login(self) -> None:
        login = require_dict(
            self.call_success(
                "admin login",
                "POST",
                "/auth/login",
                body={"username": "admin", "password": "123456", "rememberMe": False},
                assertion="seed admin can log in and obtain an access token",
            ),
            "admin login",
        )
        token = login.get("accessToken")
        user_info = login.get("userInfo")
        if not isinstance(token, str) or not token:
            raise HarnessFailure("admin login: missing accessToken")
        if not isinstance(user_info, dict) or user_info.get("username") != "admin":
            raise HarnessFailure("admin login: userInfo should identify admin")
        self.admin_token = token

    def user_payload(self, prefix: str, *, password: str = TEST_PASSWORD) -> dict[str, Any]:
        suffix = RUN_ID[-8:]
        username = f"{prefix}{suffix}"
        phone_prefix = {"apita": "170", "apitb": "171", "adminu": "172", "batchu": "173"}.get(prefix, "174")
        return {
            "username": username,
            "password": password,
            "confirmPassword": password,
            "nickname": f"{prefix.upper()} Nick {RUN_ID[-4:]}",
            "email": f"{username}@example.com",
            "phone": phone_prefix + suffix,
        }

    def register_user(self, prefix: str) -> dict[str, Any]:
        payload = self.user_payload(prefix)
        key, code = generate_captcha(self.base_url)
        registered = require_dict(
            self.call_success(
                f"register user {prefix}",
                "POST",
                "/user/auth/register",
                body={**payload, "captcha": code, "captchaKey": key},
                assertion="isolated user registration returns id and requested profile fields",
            ),
            f"register user {prefix}",
        )
        assert_user_fields(
            registered,
            {
                "username": payload["username"],
                "nickname": payload["nickname"],
                "email": payload["email"],
                "phone": payload["phone"],
                "status": 0,
            },
            f"register user {prefix}",
        )
        return {**payload, "id": registered["id"]}

    def login_user(self, user: dict[str, Any], password: str, name: str, *, login_name: str | None = None) -> str:
        key, code = generate_captcha(self.base_url)
        login = require_dict(
            self.call_success(
                name,
                "POST",
                "/user/auth/login",
                body={
                    "username": login_name or user["username"],
                    "password": password,
                    "captcha": code,
                    "captchaKey": key,
                    "rememberMe": False,
                },
                assertion="registered user can log in and receives expected token/user fields",
            ),
            name,
        )
        token = login.get("accessToken")
        user_info = login.get("userInfo")
        if not isinstance(token, str) or not token:
            raise HarnessFailure(f"{name}: missing accessToken")
        if login.get("tokenType") != "Bearer" or not isinstance(login.get("expiresIn"), int):
            raise HarnessFailure(f"{name}: tokenType/expiresIn are invalid")
        info = require_dict(user_info, f"{name} userInfo")
        for key in ("id", "username", "nickname", "email", "phone"):
            if info.get(key) != user.get(key):
                raise HarnessFailure(f"{name}: userInfo {key} mismatch, got {info.get(key)!r}")
        return token

    def assert_availability(self, user: dict[str, Any], expected: bool, label: str) -> None:
        checks = (
            ("check username availability", "/user/auth/check-username", "username"),
            ("check email availability", "/user/auth/check-email", "email"),
            ("check phone availability", "/user/auth/check-phone", "phone"),
        )
        for base_name, path, key in checks:
            data = require_dict(
                self.call_success(
                    f"{base_name} {label}",
                    "GET",
                    path,
                    query={key: user[key]},
                    assertion=f"{key} availability should be {expected} for {label}",
                ),
                f"{base_name} {label}",
            )
            if data.get("available") is not expected:
                raise HarnessFailure(f"{base_name} {label}: expected available={expected}, got {data}")

    def assert_profile(self, token: str, user: dict[str, Any], step: str) -> dict[str, Any]:
        profile = require_dict(
            self.call_success(
                step,
                "GET",
                "/user/profile",
                token=token,
                assertion="authenticated current-user profile readback matches persisted fields",
            ),
            step,
        )
        expected = {key: user[key] for key in ("username", "nickname", "email", "phone")}
        assert_user_fields(profile, expected, step)
        return profile

    def create_admin_user(self, prefix: str, *, status: int = 0) -> dict[str, Any]:
        if not self.admin_token:
            raise HarnessFailure("admin token missing")
        payload = self.user_payload(prefix)
        body = {
            "username": payload["username"],
            "password": payload["password"],
            "nickname": payload["nickname"],
            "realName": f"{prefix.upper()} Real {RUN_ID[-4:]}",
            "email": payload["email"],
            "phone": payload["phone"],
            "avatar": f"https://example.com/avatar/{payload['username']}.png",
            "gender": 1,
            "birthday": "1998-06-07",
            "status": status,
            "remark": f"api correctness {RUN_ID}",
        }
        created = require_dict(
            self.call_success(
                f"admin create user {prefix}",
                "POST",
                "/admin/user/create",
                body=body,
                token=self.admin_token,
                assertion="admin user creation returns id and requested user fields",
            ),
            f"admin create user {prefix}",
        )
        assert_user_fields(
            created,
            {
                "username": body["username"],
                "nickname": body["nickname"],
                "realName": body["realName"],
                "email": body["email"],
                "phone": body["phone"],
                "gender": body["gender"],
                "birthday": body["birthday"],
                "status": status,
            },
            f"admin create user {prefix}",
        )
        return {**body, "id": created["id"]}

    def run(self) -> dict[str, Any]:
        failure: str | None = None
        try:
            self.admin_login()

            key, code = self.generate_captcha_recorded("generate captcha")
            wrong = self.call_success(
                "verify captcha with wrong code",
                "POST",
                "/captcha/verify",
                query={"captchaKey": key, "captcha": "WRONG"},
                assertion="wrong captcha verification returns business success with data=false",
            )
            if wrong is not False:
                raise HarnessFailure(f"verify captcha with wrong code: expected false, got {wrong!r}")
            correct = self.call_success(
                "verify captcha with correct code",
                "POST",
                "/captcha/verify",
                query={"captchaKey": key, "captcha": code},
                assertion="correct captcha verification returns data=true and consumes the key",
            )
            if correct is not True:
                raise HarnessFailure(f"verify captcha with correct code: expected true, got {correct!r}")
            reused = self.call_success(
                "verify consumed captcha",
                "POST",
                "/captcha/verify",
                query={"captchaKey": key, "captcha": code},
                assertion="consumed captcha key cannot be reused",
            )
            if reused is not False:
                raise HarnessFailure(f"verify consumed captcha: expected false, got {reused!r}")

            self.primary = self.user_payload("apita")
            self.assert_availability(self.primary, True, "before registration")
            self.primary = self.register_user("apita")
            self.context["primaryUser"] = {k: v for k, v in self.primary.items() if k not in {"password", "confirmPassword"}}
            self.assert_availability(self.primary, False, "after registration")

            duplicate_key, duplicate_code = generate_captcha(self.base_url)
            self.call_expected_error(
                "duplicate username registration",
                "POST",
                "/user/auth/register",
                body={**self.primary, "captcha": duplicate_code, "captchaKey": duplicate_key},
                message_part="用户名已存在",
                allowed_codes={600},
                assertion="duplicate username is rejected as a business error",
            )
            self.call_expected_error(
                "invalid username availability validation",
                "GET",
                "/user/auth/check-username",
                query={"username": "ab"},
                message_part="用户名长度",
                allowed_codes={601},
                assertion="username availability validates minimum length",
            )

            wrong_key, _ = generate_captcha(self.base_url)
            self.call_expected_error(
                "login with wrong captcha",
                "POST",
                "/user/auth/login",
                body={
                    "username": self.primary["username"],
                    "password": TEST_PASSWORD,
                    "captcha": "WRONG",
                    "captchaKey": wrong_key,
                },
                message_part="验证码错误",
                allowed_codes={600},
                assertion="login rejects wrong captcha separately from password checks",
            )

            self.primary["token"] = self.login_user(self.primary, TEST_PASSWORD, "login registered user")
            self.primary["emailToken"] = self.login_user(
                self.primary,
                TEST_PASSWORD,
                "login registered user by email",
                login_name=self.primary["email"],
            )
            info = require_dict(
                self.call_success(
                    "get user auth info",
                    "GET",
                    "/user/auth/info",
                    token=self.primary["token"],
                    assertion="auth info returns the logged-in user",
                ),
                "get user auth info",
            )
            if info.get("id") != self.primary["id"] or info.get("username") != self.primary["username"]:
                raise HarnessFailure("get user auth info: wrong logged-in user returned")
            self.assert_profile(self.primary["token"], self.primary, "get current profile")

            refresh = require_dict(
                self.call_success(
                    "refresh user token",
                    "POST",
                    "/user/auth/refresh",
                    token=self.primary["token"],
                    assertion="refresh returns current bearer token metadata for logged-in user",
                ),
                "refresh user token",
            )
            if refresh.get("tokenType") != "Bearer" or not isinstance(refresh.get("accessToken"), str):
                raise HarnessFailure("refresh user token: missing bearer token data")

            updated_primary = {
                **self.primary,
                "nickname": f"Updated Nick {RUN_ID[-4:]}",
                "realName": f"Updated Real {RUN_ID[-4:]}",
                "email": f"apita-updated-{RUN_ID[-8:]}@example.com",
                "phone": "174" + RUN_ID[-8:],
                "avatar": f"https://example.com/avatar/apita-{RUN_ID[-4:]}.png",
                "gender": 2,
                "birthday": "1999-06-07",
            }
            updated = require_dict(
                self.call_success(
                    "update current profile",
                    "PUT",
                    "/user/profile",
                    body={
                        "nickname": updated_primary["nickname"],
                        "realName": updated_primary["realName"],
                        "email": updated_primary["email"],
                        "phone": updated_primary["phone"],
                        "avatar": updated_primary["avatar"],
                        "gender": updated_primary["gender"],
                        "birthday": updated_primary["birthday"],
                    },
                    token=self.primary["token"],
                    assertion="current-user profile update returns updated persisted fields",
                ),
                "update current profile",
            )
            assert_user_fields(
                updated,
                {
                    "username": self.primary["username"],
                    "nickname": updated_primary["nickname"],
                    "realName": updated_primary["realName"],
                    "email": updated_primary["email"],
                    "phone": updated_primary["phone"],
                    "gender": updated_primary["gender"],
                    "birthday": updated_primary["birthday"],
                    "status": 0,
                },
                "update current profile",
            )
            self.primary.update(updated_primary)
            self.assert_profile(self.primary["token"], self.primary, "read profile after update")

            self.secondary = self.register_user("apitb")
            self.secondary["token"] = self.login_user(self.secondary, TEST_PASSWORD, "login secondary user")
            self.context["secondaryUser"] = {k: v for k, v in self.secondary.items() if k not in {"password", "confirmPassword", "token"}}

            self.call_expected_error(
                "update profile to duplicate email",
                "PUT",
                "/user/profile",
                body={"email": self.secondary["email"]},
                token=self.primary["token"],
                message_part="邮箱已被其他用户使用",
                allowed_codes={600},
                assertion="current-user update rejects another active user's email",
            )
            key, code = generate_captcha(self.base_url)
            self.call_expected_error(
                "change password wrong captcha",
                "PUT",
                "/user/password",
                body={
                    "oldPassword": "WrongOld123",
                    "newPassword": NEW_PASSWORD,
                    "confirmPassword": NEW_PASSWORD,
                    "captcha": "WRONG",
                    "captchaKey": key,
                },
                token=self.primary["token"],
                message_part="验证码错误",
                allowed_codes={600},
                assertion="password change validates captcha before password checks",
            )
            key, code = generate_captcha(self.base_url)
            self.call_expected_error(
                "change password wrong old password with valid captcha",
                "PUT",
                "/user/password",
                body={
                    "oldPassword": "WrongOld123",
                    "newPassword": NEW_PASSWORD,
                    "confirmPassword": NEW_PASSWORD,
                    "captcha": code,
                    "captchaKey": key,
                },
                token=self.primary["token"],
                message_part="原密码错误",
                allowed_codes={600},
                assertion="password change rejects wrong current password",
            )
            key, code = generate_captcha(self.base_url)
            self.call_success(
                "change current user password",
                "PUT",
                "/user/password",
                body={
                    "oldPassword": TEST_PASSWORD,
                    "newPassword": NEW_PASSWORD,
                    "confirmPassword": NEW_PASSWORD,
                    "captcha": code,
                    "captchaKey": key,
                },
                token=self.primary["token"],
                assertion="current-user password change succeeds with valid old password and captcha",
            )
            self.call_expected_error(
                "old user token invalid after password change",
                "GET",
                "/user/profile",
                token=self.primary["token"],
                message_part="Token",
                allowed_codes={701, 702},
                assertion="password change logs out the old user token",
            )
            key, code = generate_captcha(self.base_url)
            self.call_expected_error(
                "login with old password after change",
                "POST",
                "/user/auth/login",
                body={
                    "username": self.primary["username"],
                    "password": TEST_PASSWORD,
                    "captcha": code,
                    "captchaKey": key,
                },
                message_part="用户名或密码错误",
                allowed_codes={600},
                assertion="old password no longer authenticates after password change",
            )
            self.primary["token"] = self.login_user(self.primary, NEW_PASSWORD, "login with new password after change")

            self.call_expected_error(
                "cross-user get by id is forbidden",
                "GET",
                f"/user/{self.secondary['id']}",
                token=self.primary["token"],
                message_part="权限",
                allowed_codes={403, 600, 703},
                assertion="ordinary users cannot read another user's full profile by id",
                fail_fast=False,
            )
            self.call_expected_error(
                "cross-user update by id is forbidden",
                "PUT",
                f"/user/{self.secondary['id']}",
                body={
                    "nickname": self.secondary["nickname"],
                    "email": self.secondary["email"],
                    "phone": self.secondary["phone"],
                },
                token=self.primary["token"],
                message_part="权限",
                allowed_codes={403, 600, 703},
                assertion="ordinary users cannot update another user's profile by id",
                fail_fast=False,
            )
            key, code = generate_captcha(self.base_url)
            password_blocked = self.call_expected_error(
                "cross-user password change by id is forbidden",
                "PUT",
                f"/user/{self.secondary['id']}/password",
                body={
                    "oldPassword": TEST_PASSWORD,
                    "newPassword": CROSS_PASSWORD,
                    "confirmPassword": CROSS_PASSWORD,
                    "captcha": code,
                    "captchaKey": key,
                },
                token=self.primary["token"],
                message_part="权限",
                allowed_codes={403, 600, 703},
                assertion="ordinary users cannot change another user's password by id",
                fail_fast=False,
            )
            if not password_blocked:
                self.secondary["password"] = CROSS_PASSWORD

            self.call_expected_error(
                "admin user list requires admin token",
                "GET",
                "/admin/user/list",
                message_part="Token",
                allowed_codes={701, 702},
                assertion="admin user list rejects anonymous access",
            )
            self.call_expected_error(
                "ordinary user token cannot access admin user list",
                "GET",
                "/admin/user/list",
                token=self.secondary["token"],
                message_part="Token",
                allowed_codes={701, 702, 703},
                assertion="ordinary user token is not accepted as admin authentication",
            )

            stats = require_dict(
                self.call_success(
                    "admin user statistics",
                    "GET",
                    "/admin/user/statistics",
                    token=self.admin_token,
                    assertion="admin user statistics returns all declared counters",
                ),
                "admin user statistics",
            )
            for key_name in ("totalUsers", "activeUsers", "disabledUsers", "deletedUsers"):
                if not isinstance(stats.get(key_name), int):
                    raise HarnessFailure(f"admin user statistics: {key_name} should be integer")

            self.admin_created = self.create_admin_user("adminu")
            self.context["adminCreatedUser"] = {k: v for k, v in self.admin_created.items() if k not in {"password", "confirmPassword"}}

            records = page_records(
                self.call_success(
                    "admin list created user",
                    "GET",
                    "/admin/user/list",
                    query={"username": self.admin_created["username"], "pageNum": 1, "pageSize": 10},
                    token=self.admin_token,
                    assertion="admin list filters by username and includes created user",
                ),
                "admin list created user",
            )
            if not find_by_id(records, self.admin_created["id"]):
                raise HarnessFailure("admin list created user: created user not found in filtered records")

            detail = require_dict(
                self.call_success(
                    "admin detail created user",
                    "GET",
                    f"/admin/user/{self.admin_created['id']}",
                    token=self.admin_token,
                    assertion="admin detail readback returns created user fields",
                ),
                "admin detail created user",
            )
            assert_user_fields(
                detail,
                {
                    "username": self.admin_created["username"],
                    "email": self.admin_created["email"],
                    "phone": self.admin_created["phone"],
                    "status": 0,
                },
                "admin detail created user",
            )

            admin_updated = {
                **self.admin_created,
                "nickname": f"Admin Updated {RUN_ID[-4:]}",
                "realName": f"Admin Updated Real {RUN_ID[-4:]}",
                "email": f"adminu-updated-{RUN_ID[-8:]}@example.com",
                "phone": "175" + RUN_ID[-8:],
                "gender": 2,
                "birthday": "1997-06-07",
            }
            update_detail = require_dict(
                self.call_success(
                    "admin update created user",
                    "PUT",
                    f"/admin/user/{self.admin_created['id']}",
                    body={
                        "nickname": admin_updated["nickname"],
                        "realName": admin_updated["realName"],
                        "email": admin_updated["email"],
                        "phone": admin_updated["phone"],
                        "gender": admin_updated["gender"],
                        "birthday": admin_updated["birthday"],
                    },
                    token=self.admin_token,
                    assertion="admin update returns updated user fields",
                ),
                "admin update created user",
            )
            assert_user_fields(
                update_detail,
                {
                    "username": self.admin_created["username"],
                    "nickname": admin_updated["nickname"],
                    "realName": admin_updated["realName"],
                    "email": admin_updated["email"],
                    "phone": admin_updated["phone"],
                    "gender": admin_updated["gender"],
                    "birthday": admin_updated["birthday"],
                    "status": 0,
                },
                "admin update created user",
            )
            self.admin_created.update(admin_updated)

            self.call_expected_error(
                "admin duplicate user create",
                "POST",
                "/admin/user/create",
                body=self.admin_created,
                token=self.admin_token,
                message_part="用户名已存在",
                allowed_codes={600},
                assertion="admin create rejects duplicate username",
            )

            self.call_success(
                "admin disable created user",
                "PUT",
                f"/admin/user/{self.admin_created['id']}/status",
                query={"status": 1},
                token=self.admin_token,
                assertion="admin can disable an active user",
            )
            disabled = require_dict(
                self.call_success(
                    "admin detail disabled user",
                    "GET",
                    f"/admin/user/{self.admin_created['id']}",
                    token=self.admin_token,
                    assertion="disabled status is persisted and readable",
                ),
                "admin detail disabled user",
            )
            if disabled.get("status") != 1:
                raise HarnessFailure(f"admin detail disabled user: expected status 1, got {disabled.get('status')!r}")
            key, code = generate_captcha(self.base_url)
            self.call_expected_error(
                "disabled user login rejected",
                "POST",
                "/user/auth/login",
                body={
                    "username": self.admin_created["username"],
                    "password": self.admin_created["password"],
                    "captcha": code,
                    "captchaKey": key,
                },
                message_part="账户已被禁用",
                allowed_codes={600},
                assertion="disabled users cannot log in",
            )
            self.call_success(
                "admin enable created user",
                "PUT",
                f"/admin/user/{self.admin_created['id']}/status",
                query={"status": 0},
                token=self.admin_token,
                assertion="admin can re-enable disabled user",
            )
            self.call_success(
                "admin reset created user password",
                "PUT",
                f"/admin/user/{self.admin_created['id']}/reset-password",
                query={"newPassword": RESET_PASSWORD},
                token=self.admin_token,
                assertion="admin password reset succeeds with supplied password",
            )
            key, code = generate_captcha(self.base_url)
            self.call_expected_error(
                "old admin-created user password rejected after reset",
                "POST",
                "/user/auth/login",
                body={
                    "username": self.admin_created["username"],
                    "password": self.admin_created["password"],
                    "captcha": code,
                    "captchaKey": key,
                },
                message_part="用户名或密码错误",
                allowed_codes={600},
                assertion="admin reset invalidates old user password",
            )
            self.login_user(self.admin_created, RESET_PASSWORD, "login admin-created user after reset")

            self.batch_created = self.create_admin_user("batchu")
            self.context["batchCreatedUser"] = {k: v for k, v in self.batch_created.items() if k not in {"password", "confirmPassword"}}
            self.call_success(
                "admin batch delete user",
                "DELETE",
                "/admin/user/batch",
                body=[self.batch_created["id"]],
                token=self.admin_token,
                assertion="admin batch delete logically deletes supplied user ids",
            )
            self.call_expected_error(
                "admin detail batch-deleted user",
                "GET",
                f"/admin/user/{self.batch_created['id']}",
                token=self.admin_token,
                message_part="用户不存在",
                allowed_codes={600},
                assertion="batch-deleted user is no longer readable",
            )

            self.call_success(
                "admin delete created user",
                "DELETE",
                f"/admin/user/{self.admin_created['id']}",
                token=self.admin_token,
                assertion="admin single delete logically deletes the created user",
            )
            self.call_expected_error(
                "admin detail deleted user",
                "GET",
                f"/admin/user/{self.admin_created['id']}",
                token=self.admin_token,
                message_part="用户不存在",
                allowed_codes={600},
                assertion="deleted user is no longer readable by admin detail",
            )
            key, code = generate_captcha(self.base_url)
            self.call_expected_error(
                "deleted user login rejected",
                "POST",
                "/user/auth/login",
                body={
                    "username": self.admin_created["username"],
                    "password": RESET_PASSWORD,
                    "captcha": code,
                    "captchaKey": key,
                },
                message_part="用户名或密码错误",
                allowed_codes={600},
                assertion="deleted users cannot log in",
            )

            self.call_success(
                "logout current user",
                "POST",
                "/user/auth/logout",
                token=self.primary["token"],
                assertion="current user can log out explicitly",
            )
            self.call_expected_error(
                "profile after logout rejected",
                "GET",
                "/user/profile",
                token=self.primary["token"],
                message_part="Token",
                allowed_codes={701, 702},
                assertion="logged out user token no longer authorizes profile access",
            )

            if self.soft_failures:
                failure = "; ".join(self.soft_failures)

        except Exception as exc:
            failure = str(exc)

        summary = self.summary(failure)
        write_json(os.path.join(self.out_dir, "steps.json"), self.results)
        write_json(os.path.join(self.out_dir, "context.json"), self.sanitized_context())
        write_json(os.path.join(self.out_dir, "summary.json"), summary)
        return summary

    def sanitized_context(self) -> dict[str, Any]:
        context = dict(self.context)
        for key, value in (
            ("primaryUser", self.primary),
            ("secondaryUser", self.secondary),
            ("adminCreatedUser", self.admin_created),
            ("batchCreatedUser", self.batch_created),
        ):
            if value:
                context[key] = {
                    k: v
                    for k, v in value.items()
                    if k not in {"token", "emailToken", "password", "confirmPassword"}
                }
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
            "service": "xiaou-user",
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
    parser = argparse.ArgumentParser(description="Run user/auth/admin-user service correctness checks")
    parser.add_argument("--base-url", default="http://127.0.0.1:9999/api")
    parser.add_argument("--out-dir", required=True)
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    harness = UserCorrectnessHarness(args.base_url, args.out_dir)
    try:
        summary = harness.run()
    except Exception:
        with open(os.path.join(args.out_dir, "error.txt"), "w", encoding="utf-8") as f:
            f.write(traceback.format_exc())
        summary = {"status": "FAIL", "error": "see error.txt"}
    print(json.dumps(summary, ensure_ascii=False), flush=True)
    return 0 if summary.get("status") == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
