#!/usr/bin/env python3
"""Service-level correctness harness for sensitive-word APIs.

This harness checks business correctness, not just HTTP reachability:
- success paths assert business code, response shape, and read-back state.
- expected validation/auth/business errors are counted separately.
- sensitive-word engine refresh is verified by public check endpoints.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import traceback
from typing import Any

from team_correctness_harness import (
    SUCCESS_CODE,
    HarnessFailure,
    assert_success_http,
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
    total = page.get("total")
    if not isinstance(total, int):
        raise HarnessFailure(f"{step}: expected numeric page.total")
    return records


def find_by_id(items: list[Any], item_id: int) -> dict[str, Any] | None:
    for item in items:
        if isinstance(item, dict) and item.get("id") == item_id:
            return item
    return None


def find_by_field(items: list[Any], field: str, value: Any) -> dict[str, Any] | None:
    for item in items:
        if isinstance(item, dict) and item.get(field) == value:
            return item
    return None


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


class SensitiveCorrectnessHarness:
    def __init__(self, base_url: str, out_dir: str):
        self.base_url = base_url.rstrip("/")
        self.out_dir = out_dir
        self.results: list[dict[str, Any]] = []
        self.context: dict[str, Any] = {"runId": RUN_ID, "baseUrl": self.base_url}
        self.admin_token: str | None = None
        self.created_word_id: int | None = None
        self.created_whitelist_id: int | None = None
        self.created_homophone_id: int | None = None
        self.created_similar_id: int | None = None
        self.created_source_id: int | None = None

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
        body: Any = None,
        query: dict[str, Any] | None = None,
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
        body: Any = None,
        query: dict[str, Any] | None = None,
        token: str | None = None,
        message_part: str | None = None,
        allowed_codes: set[int] | None = None,
        assertion: str = "expected business error",
    ) -> None:
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
        except Exception:
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
            raise

    @staticmethod
    def path_with_query(path: str, query: dict[str, Any] | None) -> str:
        if not query:
            return path
        from urllib.parse import urlencode

        return path + "?" + urlencode(query, doseq=True)

    def admin_login(self) -> None:
        data = self.call_success(
            "admin login",
            "POST",
            "/auth/login",
            body={"username": "admin", "password": "123456", "rememberMe": False},
            assertion="seed admin can authenticate before admin-only sensitive APIs",
        )
        login = require_dict(data, "admin login")
        token = login.get("accessToken")
        if not isinstance(token, str) or not token:
            raise HarnessFailure("admin login: missing accessToken")
        self.admin_token = token

    def assert_check_response(
        self,
        data: Any,
        step: str,
        *,
        hit: bool,
        expected_word: str | None = None,
        allowed: bool | None = None,
    ) -> dict[str, Any]:
        item = require_dict(data, step)
        if item.get("hit") is not hit:
            raise HarnessFailure(f"{step}: expected hit={hit}, got {item.get('hit')!r}")
        hit_words = require_list(item.get("hitWords"), f"{step} hitWords")
        if expected_word and expected_word not in hit_words:
            raise HarnessFailure(f"{step}: expected hitWords to contain {expected_word!r}, got {hit_words!r}")
        if not hit and hit_words:
            raise HarnessFailure(f"{step}: expected empty hitWords for clean text, got {hit_words!r}")
        if allowed is not None and item.get("allowed") is not allowed:
            raise HarnessFailure(f"{step}: expected allowed={allowed}, got {item.get('allowed')!r}")
        if not isinstance(item.get("processedText"), str):
            raise HarnessFailure(f"{step}: processedText missing")
        if not isinstance(item.get("riskLevel"), int):
            raise HarnessFailure(f"{step}: riskLevel missing")
        if not isinstance(item.get("action"), str):
            raise HarnessFailure(f"{step}: action missing")
        return item

    def list_words(self, word: str | None = None, page_size: int = 20) -> list[Any]:
        body: dict[str, Any] = {"pageNum": 1, "pageSize": page_size}
        if word:
            body["word"] = word
        data = self.call_success(
            "list sensitive words" if word is None else f"list sensitive words by {word}",
            "POST",
            "/admin/sensitive/words/list",
            body=body,
            token=self.admin_token,
            assertion="admin word list returns page records and total",
        )
        return page_records(data, "list sensitive words")

    def exercise_word_library(self) -> str:
        token = self.admin_token
        word = f"敏测词{RUN_ID}"
        updated_word = f"敏测改{RUN_ID}"
        self.context["sensitiveWord"] = {"word": word, "updatedWord": updated_word}

        self.call_success(
            "add sensitive word",
            "POST",
            "/admin/sensitive/words",
            token=token,
            body={
                "word": f"  {word}  ",
                "categoryId": 3,
                "level": 1,
                "action": 1,
                "status": 1,
                "remark": "api correctness",
            },
            assertion="admin can add a normalized enabled sensitive word",
        )

        records = self.list_words(word)
        created = find_by_field(records, "word", word)
        if not created:
            raise HarnessFailure("add sensitive word: added word not found by list read-back")
        self.created_word_id = int(created["id"])
        if created.get("level") != 1 or created.get("status") != 1:
            raise HarnessFailure(f"add sensitive word: wrong level/status in read-back: {created}")

        detail = self.call_success(
            "get sensitive word detail",
            "POST",
            f"/admin/sensitive/words/{self.created_word_id}",
            token=token,
            assertion="detail read-back returns the created word",
        )
        detail_obj = require_dict(detail, "get sensitive word detail")
        if detail_obj.get("word") != word or detail_obj.get("categoryId") != 3:
            raise HarnessFailure(f"get sensitive word detail: unexpected data {detail_obj}")

        self.call_expected_error(
            "duplicate sensitive word rejected",
            "POST",
            "/admin/sensitive/words",
            token=token,
            body={"word": word, "categoryId": 3, "level": 1, "action": 1, "status": 1},
            message_part="已存在",
            allowed_codes={409, 600, 603},
            assertion="duplicate word is rejected as a business error",
        )

        self.call_success(
            "refresh sensitive word library after add",
            "POST",
            "/admin/sensitive/refresh",
            token=token,
            assertion="manual refresh succeeds after adding word",
        )
        hit = self.call_success(
            "public check hits created word",
            "POST",
            "/sensitive/check",
            body={"text": f"这里包含{word}用于检测", "module": "community", "businessId": 1, "userId": 1},
            assertion="public check response reports hit, hitWords, action and processed text",
        )
        self.assert_check_response(hit, "public check hits created word", hit=True, expected_word=word, allowed=True)

        clean = self.call_success(
            "public check clean text",
            "POST",
            "/sensitive/check",
            body={"text": f"普通文本{RUN_ID}", "module": "community"},
            assertion="clean text reports no hit and remains allowed",
        )
        self.assert_check_response(clean, "public check clean text", hit=False, allowed=True)

        batch = self.call_success(
            "public batch check mixed texts",
            "POST",
            "/sensitive/check/batch",
            body=[
                {"text": f"批量命中{word}", "module": "community"},
                {"text": f"批量干净{RUN_ID}", "module": "community"},
            ],
            assertion="batch check preserves per-item hit semantics",
        )
        items = require_list(batch, "public batch check mixed texts")
        if len(items) != 2:
            raise HarnessFailure(f"public batch check mixed texts: expected 2 items, got {len(items)}")
        self.assert_check_response(items[0], "batch item hit", hit=True, expected_word=word)
        self.assert_check_response(items[1], "batch item clean", hit=False, allowed=True)

        self.call_success(
            "update sensitive word",
            "POST",
            "/admin/sensitive/words/update",
            token=token,
            body={"id": self.created_word_id, "word": updated_word, "categoryId": 3, "level": 3, "action": 2, "status": 1},
            assertion="admin can update word text, level, action and status",
        )
        detail = self.call_success(
            "get updated sensitive word detail",
            "POST",
            f"/admin/sensitive/words/{self.created_word_id}",
            token=token,
            assertion="detail reflects updated word fields",
        )
        detail_obj = require_dict(detail, "get updated sensitive word detail")
        for key, value in {"word": updated_word, "level": 3, "action": 2, "status": 1}.items():
            if detail_obj.get(key) != value:
                raise HarnessFailure(f"get updated sensitive word detail: expected {key}={value!r}, got {detail_obj.get(key)!r}")

        self.call_success(
            "refresh sensitive word library after update",
            "POST",
            "/admin/sensitive/refresh",
            token=token,
            assertion="manual refresh succeeds after updating word",
        )
        old_word_check = self.call_success(
            "old sensitive word no longer hits",
            "POST",
            "/sensitive/check",
            body={"text": f"旧词{word}", "module": "community"},
            assertion="after word update old word is no longer present in engine",
        )
        self.assert_check_response(old_word_check, "old sensitive word no longer hits", hit=False, allowed=True)

        updated_check = self.call_success(
            "updated sensitive word hits",
            "POST",
            "/sensitive/check",
            body={"text": f"新词{updated_word}", "module": "community"},
            assertion="after word update new word is loaded into engine",
        )
        self.assert_check_response(updated_check, "updated sensitive word hits", hit=True, expected_word=updated_word, allowed=False)

        self.call_expected_error(
            "update missing sensitive word rejected",
            "POST",
            "/admin/sensitive/words/update",
            token=token,
            body={"id": 999999999, "word": f"missing{RUN_ID}"},
            message_part="失败",
            allowed_codes={602, 600},
            assertion="updating a missing word is an expected business error",
        )
        self.call_expected_error(
            "get missing sensitive word rejected",
            "POST",
            "/admin/sensitive/words/999999999",
            token=token,
            message_part="不存在",
            allowed_codes={602},
            assertion="missing word detail returns data-not-exist semantics",
        )

        return updated_word

    def exercise_whitelist(self, sensitive_word: str) -> None:
        token = self.admin_token
        whitelist_word = sensitive_word
        self.call_success(
            "add global whitelist",
            "POST",
            "/sensitive/whitelist/add",
            token=token,
            body={
                "word": whitelist_word,
                "category": "api-test",
                "reason": "correctness whitelist",
                "scope": "global",
                "status": 1,
                "creatorId": 1,
            },
            assertion="admin can add a global whitelist entry",
        )
        records = page_records(
            self.call_success(
                "list whitelist by word",
                "POST",
                "/sensitive/whitelist/list",
                token=token,
                body={"pageNum": 1, "pageSize": 20, "word": whitelist_word},
                assertion="whitelist list supports word filter and page shape",
            ),
            "list whitelist by word",
        )
        created = find_by_field(records, "word", whitelist_word)
        if not created:
            raise HarnessFailure("add global whitelist: entry not found by list read-back")
        self.created_whitelist_id = int(created["id"])
        if created.get("scope") != "global" or created.get("status") != 1:
            raise HarnessFailure(f"add global whitelist: unexpected read-back {created}")

        detail = require_dict(
            self.call_success(
                "get whitelist detail",
                "GET",
                f"/sensitive/whitelist/{self.created_whitelist_id}",
                token=token,
                assertion="whitelist detail returns the created entry",
            ),
            "get whitelist detail",
        )
        if detail.get("word") != whitelist_word:
            raise HarnessFailure(f"get whitelist detail: unexpected detail {detail}")

        self.call_success(
            "refresh whitelist cache",
            "POST",
            "/sensitive/whitelist/refresh",
            token=token,
            assertion="whitelist refresh succeeds before checking filtering",
        )
        filtered = self.call_success(
            "whitelisted word is filtered from check",
            "POST",
            "/sensitive/check",
            body={"text": f"白名单过滤{sensitive_word}", "module": "community"},
            assertion="global whitelist suppresses a sensitive hit",
        )
        self.assert_check_response(filtered, "whitelisted word is filtered from check", hit=False, allowed=True)

        self.call_success(
            "update whitelist disabled",
            "POST",
            "/sensitive/whitelist/update",
            token=token,
            body={"id": self.created_whitelist_id, "status": 0, "reason": "disable for regression"},
            assertion="whitelist can be disabled and read back",
        )
        detail = require_dict(
            self.call_success(
                "get disabled whitelist detail",
                "GET",
                f"/sensitive/whitelist/{self.created_whitelist_id}",
                token=token,
                assertion="whitelist detail reflects disabled status",
            ),
            "get disabled whitelist detail",
        )
        if detail.get("status") != 0:
            raise HarnessFailure(f"get disabled whitelist detail: expected status 0, got {detail.get('status')!r}")
        self.call_success(
            "refresh disabled whitelist cache",
            "POST",
            "/sensitive/whitelist/refresh",
            token=token,
            assertion="whitelist refresh succeeds after disabling entry",
        )
        hit = self.call_success(
            "disabled whitelist no longer filters",
            "POST",
            "/sensitive/check",
            body={"text": f"白名单禁用后{sensitive_word}", "module": "community"},
            assertion="disabled whitelist entry no longer suppresses hit",
        )
        self.assert_check_response(hit, "disabled whitelist no longer filters", hit=True, expected_word=sensitive_word)

        self.call_expected_error(
            "update missing whitelist rejected",
            "POST",
            "/sensitive/whitelist/update",
            token=token,
            body={"id": 999999999, "word": f"missing{RUN_ID}"},
            message_part="不存在",
            allowed_codes={602, 600},
            assertion="updating missing whitelist returns business error",
        )

    def exercise_variant_mappings(self) -> None:
        token = self.admin_token
        original_homo = f"原{RUN_ID[-4:]}"
        homo_chars = f"源{RUN_ID[-4:]},元{RUN_ID[-4:]}"
        original_similar = f"形{RUN_ID[-4:]}"
        similar_chars = f"彤{RUN_ID[-4:]},衍{RUN_ID[-4:]}"

        self.call_success(
            "add homophone mapping",
            "POST",
            "/sensitive/homophone/add",
            token=token,
            body={"originalChar": original_homo, "homophoneChars": homo_chars, "status": 1},
            assertion="admin can add homophone mapping",
        )
        homo_records = page_records(
            self.call_success(
                "list homophone mapping",
                "POST",
                "/sensitive/homophone/list",
                token=token,
                body={"pageNum": 1, "pageSize": 20, "originalChar": original_homo},
                assertion="homophone list supports originalChar filter",
            ),
            "list homophone mapping",
        )
        homo = find_by_field(homo_records, "originalChar", original_homo)
        if not homo:
            raise HarnessFailure("add homophone mapping: mapping not found by list")
        self.created_homophone_id = int(homo["id"])
        if homo.get("homophoneChars") != homo_chars or homo.get("status") != 1:
            raise HarnessFailure(f"add homophone mapping: unexpected read-back {homo}")

        self.call_success(
            "update homophone mapping",
            "POST",
            "/sensitive/homophone/update",
            token=token,
            body={"id": self.created_homophone_id, "homophoneChars": homo_chars + ",圆", "status": 0},
            assertion="homophone mapping update persists chars and status",
        )
        homo_detail = require_dict(
            self.call_success(
                "get homophone detail",
                "GET",
                f"/sensitive/homophone/{self.created_homophone_id}",
                token=token,
                assertion="homophone detail reflects update",
            ),
            "get homophone detail",
        )
        if homo_detail.get("status") != 0 or "圆" not in str(homo_detail.get("homophoneChars")):
            raise HarnessFailure(f"get homophone detail: unexpected detail {homo_detail}")
        self.call_expected_error(
            "update missing homophone rejected",
            "POST",
            "/sensitive/homophone/update",
            token=token,
            body={"id": 999999999, "homophoneChars": "x"},
            message_part="不存在",
            allowed_codes={602, 600},
            assertion="updating missing homophone returns business error",
        )

        self.call_success(
            "add similar-char mapping",
            "POST",
            "/sensitive/similar-char/add",
            token=token,
            body={"originalChar": original_similar, "similarChars": similar_chars, "status": 1},
            assertion="admin can add similar-char mapping",
        )
        similar_records = page_records(
            self.call_success(
                "list similar-char mapping",
                "POST",
                "/sensitive/similar-char/list",
                token=token,
                body={"pageNum": 1, "pageSize": 20, "originalChar": original_similar},
                assertion="similar-char list supports originalChar filter",
            ),
            "list similar-char mapping",
        )
        similar = find_by_field(similar_records, "originalChar", original_similar)
        if not similar:
            raise HarnessFailure("add similar-char mapping: mapping not found by list")
        self.created_similar_id = int(similar["id"])
        if similar.get("similarChars") != similar_chars or similar.get("status") != 1:
            raise HarnessFailure(f"add similar-char mapping: unexpected read-back {similar}")

        self.call_success(
            "update similar-char mapping",
            "POST",
            "/sensitive/similar-char/update",
            token=token,
            body={"id": self.created_similar_id, "similarChars": similar_chars + ",彡", "status": 0},
            assertion="similar-char mapping update persists chars and status",
        )
        similar_detail = require_dict(
            self.call_success(
                "get similar-char detail",
                "GET",
                f"/sensitive/similar-char/{self.created_similar_id}",
                token=token,
                assertion="similar-char detail reflects update",
            ),
            "get similar-char detail",
        )
        if similar_detail.get("status") != 0 or "彡" not in str(similar_detail.get("similarChars")):
            raise HarnessFailure(f"get similar-char detail: unexpected detail {similar_detail}")
        self.call_expected_error(
            "batch add empty similar-char rejected",
            "POST",
            "/sensitive/similar-char/batch-add",
            token=token,
            body=[],
            message_part="列表不能为空",
            allowed_codes={601},
            assertion="empty similar-char batch is a validation business error",
        )

    def exercise_strategy_statistics_version_source(self) -> None:
        token = self.admin_token
        strategies = page_records(
            self.call_success(
                "list sensitive strategies",
                "POST",
                "/sensitive/strategy/list",
                token=token,
                body={"pageNum": 1, "pageSize": 20},
                assertion="strategy list returns configured policy records",
            ),
            "list sensitive strategies",
        )
        if not strategies:
            raise HarnessFailure("list sensitive strategies: expected seeded strategies")
        strategy = require_dict(strategies[0], "first strategy")
        strategy_id = strategy.get("id")
        module = strategy.get("module")
        level = strategy.get("level")
        if not isinstance(strategy_id, int) or not isinstance(module, str) or not isinstance(level, int):
            raise HarnessFailure(f"list sensitive strategies: missing id/module/level in {strategy}")

        detail = require_dict(
            self.call_success(
                "get sensitive strategy detail",
                "GET",
                f"/sensitive/strategy/{strategy_id}",
                token=token,
                assertion="strategy detail read-back returns selected strategy",
            ),
            "get sensitive strategy detail",
        )
        original_description = detail.get("description")
        original_status = detail.get("status")
        updated_description = f"api correctness {RUN_ID}"
        self.call_success(
            "update sensitive strategy description",
            "POST",
            "/sensitive/strategy/update",
            token=token,
            body={"id": strategy_id, "description": updated_description, "status": original_status},
            assertion="strategy update persists non-destructive fields",
        )
        updated = require_dict(
            self.call_success(
                "get updated sensitive strategy detail",
                "GET",
                f"/sensitive/strategy/{strategy_id}",
                token=token,
                assertion="strategy detail reflects update",
            ),
            "get updated sensitive strategy detail",
        )
        if updated.get("description") != updated_description:
            raise HarnessFailure(f"get updated sensitive strategy detail: expected description update, got {updated}")
        public_strategy = require_dict(
            self.call_success(
                "public get strategy by module level",
                "GET",
                "/sensitive/strategy/get",
                query={"module": module, "level": level},
                assertion="public strategy lookup returns module/level policy",
            ),
            "public get strategy by module level",
        )
        if public_strategy.get("id") != strategy_id:
            raise HarnessFailure(f"public get strategy by module level: expected id {strategy_id}, got {public_strategy}")
        self.call_success(
            "restore sensitive strategy description",
            "POST",
            "/sensitive/strategy/update",
            token=token,
            body={"id": strategy_id, "description": original_description, "status": original_status},
            assertion="strategy original description is restored after test",
        )
        self.call_expected_error(
            "update missing strategy rejected",
            "POST",
            "/sensitive/strategy/update",
            token=token,
            body={"id": 999999999, "description": "missing"},
            message_part="不存在",
            allowed_codes={602, 600},
            assertion="updating missing strategy returns business error",
        )

        overview = require_dict(
            self.call_success(
                "sensitive statistics overview",
                "POST",
                "/sensitive/statistics/overview",
                token=token,
                body={"module": "community"},
                assertion="statistics overview returns numeric counters",
            ),
            "sensitive statistics overview",
        )
        numeric_seen = False
        for key, value in overview.items():
            if isinstance(value, int):
                numeric_seen = True
                if value < 0:
                    raise HarnessFailure(f"sensitive statistics overview: negative counter {key}={value}")
        if not numeric_seen:
            raise HarnessFailure(f"sensitive statistics overview: expected at least one numeric counter, got {overview}")

        for name, path in (
            ("sensitive statistics trend", "/sensitive/statistics/trend"),
            ("sensitive statistics hot words", "/sensitive/statistics/hot-words"),
            ("sensitive statistics category distribution", "/sensitive/statistics/category-distribution"),
            ("sensitive statistics module distribution", "/sensitive/statistics/module-distribution"),
        ):
            data = self.call_success(
                name,
                "POST",
                path,
                token=token,
                body={"module": "community"},
                assertion=f"{name} returns a list shape",
            )
            require_list(data, name)

        export_data = require_dict(
            self.call_success(
                "sensitive statistics export",
                "POST",
                "/sensitive/statistics/export",
                token=token,
                body={"module": "community"},
                assertion="statistics export returns file metadata/content",
            ),
            "sensitive statistics export",
        )
        if not export_data:
            raise HarnessFailure("sensitive statistics export: expected non-empty export payload")

        latest_version = self.call_success(
            "get latest sensitive version",
            "GET",
            "/sensitive/version/latest",
            assertion="latest version endpoint returns a string value",
        )
        if latest_version is not None and not isinstance(latest_version, str):
            raise HarnessFailure(f"get latest sensitive version: expected string/null, got {type(latest_version).__name__}")
        versions = page_records(
            self.call_success(
                "list sensitive versions",
                "POST",
                "/sensitive/version/list",
                token=token,
                body={"pageNum": 1, "pageSize": 10},
                assertion="version list returns page shape after word mutations",
            ),
            "list sensitive versions",
        )
        if versions:
            version_id = versions[0].get("id")
            if isinstance(version_id, int):
                self.call_success(
                    "get sensitive version detail",
                    "GET",
                    f"/sensitive/version/{version_id}",
                    token=token,
                    assertion="version detail returns the selected version",
                )
        self.call_expected_error(
            "get missing sensitive version rejected",
            "GET",
            "/sensitive/version/999999999",
            token=token,
            message_part="不存在",
            allowed_codes={602},
            assertion="missing version detail returns data-not-exist semantics",
        )

        source_name = f"api-source-{RUN_ID}"
        self.call_success(
            "add sensitive source",
            "POST",
            "/sensitive/source/add",
            token=token,
            body={
                "sourceName": source_name,
                "sourceType": "manual",
                "apiUrl": "http://127.0.0.1/unused",
                "syncInterval": 0,
                "status": 0,
            },
            assertion="admin can add a disabled source without external sync",
        )
        sources = page_records(
            self.call_success(
                "list sensitive source by name",
                "POST",
                "/sensitive/source/list",
                token=token,
                body={"pageNum": 1, "pageSize": 20, "sourceName": source_name},
                assertion="source list supports name filter",
            ),
            "list sensitive source by name",
        )
        source = find_by_field(sources, "sourceName", source_name)
        if not source:
            raise HarnessFailure("add sensitive source: source not found by list read-back")
        self.created_source_id = int(source["id"])
        source_detail = require_dict(
            self.call_success(
                "get sensitive source detail",
                "GET",
                f"/sensitive/source/{self.created_source_id}",
                token=token,
                assertion="source detail returns created source",
            ),
            "get sensitive source detail",
        )
        if source_detail.get("sourceName") != source_name or source_detail.get("status") != 0:
            raise HarnessFailure(f"get sensitive source detail: unexpected detail {source_detail}")
        self.call_expected_error(
            "update missing sensitive source rejected",
            "POST",
            "/sensitive/source/update",
            token=token,
            body={"id": 999999999, "sourceName": "missing"},
            message_part="不存在",
            allowed_codes={602, 600},
            assertion="updating missing source returns business error",
        )

    def cleanup(self) -> None:
        token = self.admin_token
        if not token:
            return
        cleanup_steps = [
            ("delete sensitive source", "POST", f"/sensitive/source/delete/{self.created_source_id}", self.created_source_id),
            ("delete similar-char mapping", "POST", f"/sensitive/similar-char/delete/{self.created_similar_id}", self.created_similar_id),
            ("delete homophone mapping", "POST", f"/sensitive/homophone/delete/{self.created_homophone_id}", self.created_homophone_id),
            ("delete whitelist", "POST", f"/sensitive/whitelist/delete/{self.created_whitelist_id}", self.created_whitelist_id),
            ("delete sensitive word", "POST", f"/admin/sensitive/words/delete/{self.created_word_id}", self.created_word_id),
        ]
        for name, method, path, item_id in cleanup_steps:
            if item_id is None:
                continue
            self.call_success(name, method, path, token=token, assertion=f"{name} succeeds during cleanup")
        self.call_success(
            "refresh sensitive word library after cleanup",
            "POST",
            "/admin/sensitive/refresh",
            token=token,
            assertion="word library refresh succeeds after cleanup",
        )
        self.call_success(
            "refresh whitelist after cleanup",
            "POST",
            "/sensitive/whitelist/refresh",
            token=token,
            assertion="whitelist refresh succeeds after cleanup",
        )

    def run(self) -> None:
        failure: str | None = None
        try:
            self.admin_login()
            self.call_expected_error(
                "admin endpoint rejects missing token",
                "POST",
                "/admin/sensitive/words/list",
                body={"pageNum": 1, "pageSize": 1},
                message_part="Token",
                allowed_codes={701, 702, 703},
                assertion="admin sensitive APIs require admin authentication",
            )
            sensitive_word = self.exercise_word_library()
            self.exercise_whitelist(sensitive_word)
            self.exercise_variant_mappings()
            self.exercise_strategy_statistics_version_source()
            self.cleanup()
        except Exception as exc:
            failure = "".join(traceback.format_exception_only(type(exc), exc)).strip()
            if not isinstance(exc, HarnessFailure):
                failure += "\n" + traceback.format_exc()
        finally:
            summary = {
                "runId": RUN_ID,
                "generatedAt": now_iso(),
                "baseUrl": self.base_url,
                "service": "xiaou-sensitive",
                "status": "FAIL" if failure else "PASS",
                "failure": failure,
                "stepCount": len(self.results),
                "failureCount": sum(1 for item in self.results if item["status"] == "FAIL"),
                "byStatus": {},
                "failedSteps": [item for item in self.results if item["status"] == "FAIL"],
            }
            for item in self.results:
                summary["byStatus"][item["status"]] = summary["byStatus"].get(item["status"], 0) + 1
            write_json(os.path.join(self.out_dir, "steps.json"), self.results)
            write_json(os.path.join(self.out_dir, "context.json"), self.context)
            write_json(os.path.join(self.out_dir, "summary.json"), summary)
            print(json.dumps(summary, ensure_ascii=False))
            if failure:
                raise SystemExit(1)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:9999/api")
    parser.add_argument("--out-dir", required=True)
    args = parser.parse_args()
    SensitiveCorrectnessHarness(args.base_url, args.out_dir).run()


if __name__ == "__main__":
    main()
