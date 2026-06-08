#!/usr/bin/env python3
"""Service-level correctness harness for learning asset APIs.

This verifies the learning asset state machine end to end:
- user fixture creation through auth, points, and blog draft APIs
- conversion from a blog draft into learning asset candidates
- candidate edit, discard, confirm, publish, and admin review transitions
- list/detail/statistics readbacks
- expected auth, validation, and business errors counted separately

Secrets, captchas, and tokens are used in memory only and are not written.
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


def by_asset_type(candidates: list[Any], asset_type: str) -> list[dict[str, Any]]:
    return [
        item
        for item in candidates
        if isinstance(item, dict) and item.get("assetType") == asset_type
    ]


class LearningAssetCorrectnessHarness:
    def __init__(self, base_url: str, out_dir: str):
        self.base_url = base_url.rstrip("/")
        self.out_dir = out_dir
        self.results: list[dict[str, Any]] = []
        self.context: dict[str, Any] = {"runId": RUN_ID, "baseUrl": self.base_url}
        self.user: dict[str, Any] = {}
        self.admin_token: str | None = None
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
        login_data = self.call_success(
            "admin login",
            "POST",
            "/auth/login",
            body={"username": "admin", "password": "123456", "rememberMe": False},
            assertion="seed admin can log in for review and fixture operations",
        )
        login = require_dict(login_data, "admin login")
        token = login.get("accessToken")
        user_info = login.get("userInfo")
        if not isinstance(token, str) or not token:
            raise HarnessFailure("admin login: missing accessToken")
        if not isinstance(user_info, dict) or user_info.get("username") != "admin":
            raise HarnessFailure("admin login: userInfo should identify admin")
        self.admin_token = token

    def register_and_login_user(self) -> None:
        username = f"asset{RUN_ID[-8:]}"
        email = f"{username}@example.com"
        phone = "138" + RUN_ID[-8:]

        key, code = generate_captcha(self.base_url)
        register_data = self.call_success(
            "register asset user",
            "POST",
            "/user/auth/register",
            body={
                "username": username,
                "password": TEST_PASSWORD,
                "confirmPassword": TEST_PASSWORD,
                "nickname": f"Asset User {RUN_ID[-4:]}",
                "email": email,
                "phone": phone,
                "captcha": code,
                "captchaKey": key,
            },
            assertion="new isolated user can be registered for learning asset flow",
        )
        registered = require_dict(register_data, "register asset user")
        user_id = registered.get("id")
        if not isinstance(user_id, int):
            raise HarnessFailure("register asset user: missing numeric user id")

        key, code = generate_captcha(self.base_url)
        login_data = self.call_success(
            "login asset user",
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
        )
        login = require_dict(login_data, "login asset user")
        token = login.get("accessToken")
        if not isinstance(token, str) or not token:
            raise HarnessFailure("login asset user: missing accessToken")
        self.user = {"id": user_id, "username": username, "email": email, "phone": phone, "token": token}
        self.context["userId"] = user_id

    def ensure_blog_category(self) -> int:
        token = self.user["token"]
        categories_data = self.call_success(
            "get blog categories",
            "GET",
            "/user/blog/categories",
            token=token,
            assertion="blog category list is readable before creating a source draft",
        )
        categories = require_list(categories_data, "get blog categories")
        for category in categories:
            if isinstance(category, dict) and isinstance(category.get("id"), int) and category.get("status", 1) == 1:
                self.context["categoryId"] = category["id"]
                return category["id"]

        if not self.admin_token:
            raise HarnessFailure("blog category fixture: admin token missing")
        name = f"API Asset Category {RUN_ID[-6:]}"
        self.call_success(
            "create blog category fixture",
            "POST",
            "/admin/blog/category/create",
            body={
                "categoryName": name,
                "categoryIcon": "",
                "categoryDescription": "learning asset correctness fixture",
                "sortOrder": 999,
            },
            token=self.admin_token,
            assertion="admin can create a blog category fixture when none exists",
        )
        categories_data = self.call_success(
            "get blog categories after fixture",
            "GET",
            "/user/blog/categories",
            token=token,
            assertion="created category is visible to user blog APIs",
        )
        for category in require_list(categories_data, "get blog categories after fixture"):
            if isinstance(category, dict) and category.get("categoryName") == name and isinstance(category.get("id"), int):
                self.context["categoryId"] = category["id"]
                return category["id"]
        raise HarnessFailure("blog category fixture: created category not found")

    def create_blog_source(self, category_id: int) -> int:
        token = self.user["token"]

        initial_balance = require_dict(
            self.call_success(
                "get initial points balance",
                "GET",
                "/user/points/balance",
                token=token,
                assertion="points balance exists or is initialized for the new user",
            ),
            "get initial points balance",
        )
        initial_points = initial_balance.get("totalPoints")
        if not isinstance(initial_points, int) or initial_points != 0:
            raise HarnessFailure(f"initial points balance: expected 0 for isolated user, got {initial_points!r}")

        checkin = require_dict(
            self.call_success(
                "daily points checkin",
                "POST",
                "/user/points/checkin",
                token=token,
                assertion="first daily checkin grants points and updates balance",
            ),
            "daily points checkin",
        )
        earned = checkin.get("pointsEarned")
        total_balance = checkin.get("totalBalance")
        if not isinstance(earned, int) or earned <= 0:
            raise HarnessFailure("daily points checkin: pointsEarned should be positive")
        if total_balance != earned:
            raise HarnessFailure("daily points checkin: totalBalance should equal earned points for new user")

        balance_after_checkin = require_dict(
            self.call_success(
                "read points balance after checkin",
                "GET",
                "/user/points/balance",
                token=token,
                assertion="points balance readback reflects checkin reward",
            ),
            "read points balance after checkin",
        )
        if balance_after_checkin.get("totalPoints") != earned or balance_after_checkin.get("todayCheckedIn") is not True:
            raise HarnessFailure("points balance after checkin: reward or todayCheckedIn mismatch")

        opened = require_dict(
            self.call_success(
                "open blog with checkin points",
                "POST",
                "/user/blog/open",
                token=token,
                assertion="blog opening deducts points and returns blog id",
            ),
            "open blog with checkin points",
        )
        blog_id = opened.get("blogId")
        remaining = opened.get("pointsRemaining")
        if not isinstance(blog_id, int):
            raise HarnessFailure("open blog: missing numeric blogId")
        if not isinstance(remaining, int) or remaining != earned - 50:
            raise HarnessFailure(f"open blog: expected pointsRemaining {earned - 50}, got {remaining!r}")
        self.created_ids["blogId"] = blog_id

        status = require_dict(
            self.call_success(
                "check blog opened status",
                "GET",
                "/user/blog/check-status",
                token=token,
                assertion="blog status readback reports opened blog id",
            ),
            "check blog opened status",
        )
        if status.get("isOpened") is not True or status.get("blogId") != blog_id:
            raise HarnessFailure("blog check-status: opened status or blogId mismatch")

        title = f"Learning Asset Source {RUN_ID[-6:]}"
        content = (
            "学习资产转化测试内容，覆盖闪卡、知识节点和练习计划三个方向。"
            "文章说明了如何把一篇技术博客拆成可复习、可实践、可审核的学习单元。"
            "这里包含状态流转、候选编辑、发布审核和统计读回等关键验证点。"
        )
        article_id = self.call_success(
            "create blog draft source",
            "POST",
            "/user/blog/article/create",
            body={
                "title": title,
                "summary": "学习资产转化 correctness 源文章摘要",
                "content": content,
                "categoryId": category_id,
                "tags": ["api", "learning-asset", "correctness"],
                "isOriginal": 1,
            },
            token=token,
            assertion="user can create a blog draft used as learning asset source",
        )
        if not isinstance(article_id, int):
            raise HarnessFailure("create blog draft source: expected numeric article id")
        self.created_ids["articleId"] = article_id

        detail = require_dict(
            self.call_success(
                "read blog draft source",
                "GET",
                f"/user/blog/article/{article_id}",
                token=token,
                assertion="draft article detail is readable by owner and echoes source fields",
            ),
            "read blog draft source",
        )
        if detail.get("id") != article_id or detail.get("title") != title or detail.get("categoryId") != category_id:
            raise HarnessFailure("blog draft detail: id/title/category mismatch")
        if detail.get("canEdit") is not True:
            raise HarnessFailure("blog draft detail: owner should be able to edit")
        self.context["sourceTitle"] = title
        return article_id

    def run(self) -> dict[str, Any]:
        failure: str | None = None
        try:
            self.admin_login()
            self.register_and_login_user()
            if not self.user.get("token") or not self.admin_token:
                raise HarnessFailure("required tokens missing after login")
            user_token = self.user["token"]

            self.call_expected_error(
                "learning asset records require user auth",
                "POST",
                "/user/learning-assets/records/list",
                body={"pageNum": 1, "pageSize": 5},
                message_part="Token",
                assertion="anonymous user cannot list learning asset records",
            )

            category_id = self.ensure_blog_category()
            article_id = self.create_blog_source(category_id)

            convert_data = require_dict(
                self.call_success(
                    "convert blog draft to learning assets",
                    "POST",
                    "/user/learning-assets/convert",
                    body={
                        "sourceType": "blog",
                        "sourceId": article_id,
                        "transformMode": "study",
                        "targetTypes": ["flashcard", "practice_plan", "knowledge_node"],
                        "useExistingSummary": True,
                        "extraTags": ["service-correctness"],
                    },
                    token=user_token,
                    assertion="conversion creates a pending record with draft candidates",
                ),
                "convert blog draft to learning assets",
            )
            record_id = convert_data.get("recordId")
            candidates = require_list(convert_data.get("candidates"), "convert candidates")
            if not isinstance(record_id, int):
                raise HarnessFailure("convert: missing numeric recordId")
            if convert_data.get("status") != "PENDING_CONFIRM":
                raise HarnessFailure(f"convert: expected PENDING_CONFIRM, got {convert_data.get('status')!r}")
            if convert_data.get("sourceType") != "blog" or convert_data.get("sourceId") != article_id:
                raise HarnessFailure("convert: sourceType/sourceId mismatch")
            if convert_data.get("totalCandidates") != len(candidates) or len(candidates) < 4:
                raise HarnessFailure("convert: candidate count should match detail and include generated candidates")
            for candidate in candidates:
                if not isinstance(candidate, dict) or candidate.get("status") != "DRAFT":
                    raise HarnessFailure("convert: all generated candidates should start as DRAFT")
            flashcards = by_asset_type(candidates, "flashcard")
            plans = by_asset_type(candidates, "practice_plan")
            knowledge_nodes = by_asset_type(candidates, "knowledge_node")
            if len(flashcards) < 1 or len(plans) < 1 or len(knowledge_nodes) != 1:
                raise HarnessFailure("convert: expected flashcard, practice_plan, and one knowledge_node candidate")
            self.created_ids["recordId"] = record_id
            self.created_ids["candidateIds"] = [candidate.get("id") for candidate in candidates if isinstance(candidate, dict)]

            self.call_expected_error(
                "publish rejects draft candidate ids",
                "POST",
                f"/user/learning-assets/records/{record_id}/publish",
                body={"candidateIds": [flashcards[0]["id"]]},
                token=user_token,
                message_part="仅支持发布已确认候选项",
                assertion="publish cannot bypass candidate confirmation",
            )

            updated_title = f"Edited Flashcard {RUN_ID[-6:]}"
            updated_content = json.dumps(
                {
                    "frontContent": "学习资产编辑后的问题",
                    "backContent": "编辑后的答案会被读回并发布为闪卡",
                    "contentType": 1,
                },
                ensure_ascii=False,
            )
            self.call_success(
                "edit draft candidate",
                "PUT",
                f"/user/learning-assets/candidates/{flashcards[0]['id']}",
                body={
                    "title": updated_title,
                    "contentJson": updated_content,
                    "tags": "api,edited",
                    "difficulty": "medium",
                },
                token=user_token,
                assertion="draft candidate editable fields can be updated",
            )
            detail = require_dict(
                self.call_success(
                    "read record after candidate edit",
                    "GET",
                    f"/user/learning-assets/records/{record_id}",
                    token=user_token,
                    assertion="record detail reflects candidate edit",
                ),
                "read record after candidate edit",
            )
            edited = find_by(require_list(detail.get("candidates"), "detail candidates after edit"), "id", flashcards[0]["id"])
            if not edited:
                raise HarnessFailure("candidate edit readback: edited candidate missing")
            if edited.get("title") != updated_title or edited.get("contentJson") != updated_content:
                raise HarnessFailure("candidate edit readback: title/contentJson mismatch")
            if edited.get("tags") != "api,edited" or edited.get("difficulty") != "medium":
                raise HarnessFailure("candidate edit readback: tags/difficulty mismatch")

            discard_id = plans[-1]["id"]
            discarded_detail = require_dict(
                self.call_success(
                    "discard one draft candidate",
                    "POST",
                    f"/user/learning-assets/candidates/{discard_id}/discard",
                    token=user_token,
                    assertion="draft candidate can be discarded and read back",
                ),
                "discard one draft candidate",
            )
            discarded = find_by(require_list(discarded_detail.get("candidates"), "discard candidates"), "id", discard_id)
            if not discarded or discarded.get("status") != "DISCARDED":
                raise HarnessFailure("discard candidate: readback status should be DISCARDED")

            selected_ids = [flashcards[0]["id"], plans[0]["id"], knowledge_nodes[0]["id"]]
            confirmed = require_dict(
                self.call_success(
                    "confirm selected candidates",
                    "POST",
                    f"/user/learning-assets/records/{record_id}/confirm",
                    body={"candidateIds": selected_ids},
                    token=user_token,
                    assertion="confirm marks chosen candidates SELECTED and others DISCARDED",
                ),
                "confirm selected candidates",
            )
            confirmed_candidates = require_list(confirmed.get("candidates"), "confirmed candidates")
            for candidate_id in selected_ids:
                candidate = find_by(confirmed_candidates, "id", candidate_id)
                if not candidate or candidate.get("status") != "SELECTED":
                    raise HarnessFailure(f"confirm selected candidates: candidate {candidate_id} not SELECTED")
            if find_by(confirmed_candidates, "id", discard_id).get("status") != "DISCARDED":
                raise HarnessFailure("confirm selected candidates: discarded candidate changed unexpectedly")

            listed = page_records(
                self.call_success(
                    "list records after confirm",
                    "POST",
                    "/user/learning-assets/records/list",
                    body={"pageNum": 1, "pageSize": 5, "sourceType": "blog"},
                    token=user_token,
                    assertion="record list includes the new blog transform summary",
                ),
                "list records after confirm",
            )
            summary = find_by(listed, "recordId", record_id)
            if not summary or summary.get("sourceType") != "blog" or summary.get("sourceId") != article_id:
                raise HarnessFailure("record list: created transform summary missing or mismatched")

            publish = require_dict(
                self.call_success(
                    "publish confirmed candidates",
                    "POST",
                    f"/user/learning-assets/records/{record_id}/publish",
                    body={},
                    token=user_token,
                    assertion="publish creates direct assets and sends review-only candidates to admin queue",
                ),
                "publish confirmed candidates",
            )
            if publish.get("recordId") != record_id:
                raise HarnessFailure("publish: recordId mismatch")
            if publish.get("publishedCount") != 2 or publish.get("reviewingCount") != 1:
                raise HarnessFailure("publish: expected 2 direct published and 1 reviewing candidate")
            if flashcards[0]["id"] not in require_list(publish.get("publishedCandidateIds"), "publish published ids"):
                raise HarnessFailure("publish: edited flashcard candidate id missing from published ids")
            if plans[0]["id"] not in require_list(publish.get("publishedCandidateIds"), "publish published ids"):
                raise HarnessFailure("publish: practice plan candidate id missing from published ids")
            if knowledge_nodes[0]["id"] not in require_list(publish.get("reviewingCandidateIds"), "publish reviewing ids"):
                raise HarnessFailure("publish: knowledge node candidate id missing from reviewing ids")
            if not isinstance(publish.get("flashcardDeckId"), int):
                raise HarnessFailure("publish: flashcardDeckId should be present for direct flashcard publish")
            if not require_list(publish.get("planIds"), "publish plan ids"):
                raise HarnessFailure("publish: planIds should include created practice plan")

            detail = require_dict(
                self.call_success(
                    "read record after publish",
                    "GET",
                    f"/user/learning-assets/records/{record_id}",
                    token=user_token,
                    assertion="record detail reflects direct publish and review submission states",
                ),
                "read record after publish",
            )
            if detail.get("status") != "PARTIAL_PUBLISHED" or detail.get("publishedCandidates") != 2:
                raise HarnessFailure("record after publish: expected PARTIAL_PUBLISHED with two published candidates")
            detail_candidates = require_list(detail.get("candidates"), "detail candidates after publish")
            if find_by(detail_candidates, "id", flashcards[0]["id"]).get("status") != "PUBLISHED":
                raise HarnessFailure("record after publish: flashcard should be PUBLISHED")
            if find_by(detail_candidates, "id", plans[0]["id"]).get("status") != "PUBLISHED":
                raise HarnessFailure("record after publish: practice plan should be PUBLISHED")
            reviewing = find_by(detail_candidates, "id", knowledge_nodes[0]["id"])
            if not reviewing or reviewing.get("status") != "REVIEWING" or reviewing.get("reviewNote") != "已提交审核":
                raise HarnessFailure("record after publish: knowledge node should be REVIEWING with submit note")

            review_rows = page_records(
                self.call_success(
                    "admin list reviewing knowledge candidates",
                    "POST",
                    "/admin/learning-assets/candidates/list",
                    body={"assetType": "knowledge_node", "pageNum": 1, "pageSize": 10},
                    token=self.admin_token,
                    assertion="admin review list includes submitted knowledge node candidate",
                ),
                "admin list reviewing knowledge candidates",
            )
            review_row = find_by(review_rows, "candidateId", knowledge_nodes[0]["id"])
            if not review_row or review_row.get("recordId") != record_id or review_row.get("status") != "REVIEWING":
                raise HarnessFailure("admin review list: submitted knowledge candidate missing or mismatched")

            review_detail = require_dict(
                self.call_success(
                    "admin read reviewing candidate detail",
                    "GET",
                    f"/admin/learning-assets/candidates/{knowledge_nodes[0]['id']}",
                    token=self.admin_token,
                    assertion="admin review detail echoes source and candidate fields",
                ),
                "admin read reviewing candidate detail",
            )
            if review_detail.get("candidateId") != knowledge_nodes[0]["id"] or review_detail.get("sourceId") != article_id:
                raise HarnessFailure("admin review detail: candidate/source mismatch")
            if review_detail.get("assetType") != "knowledge_node" or review_detail.get("sourceType") != "blog":
                raise HarnessFailure("admin review detail: assetType/sourceType mismatch")

            self.call_expected_error(
                "admin reject requires note",
                "POST",
                f"/admin/learning-assets/candidates/{knowledge_nodes[0]['id']}/reject",
                body={"note": ""},
                token=self.admin_token,
                message_part="驳回原因不能为空",
                assertion="admin reject validates non-empty note",
            )

            reject_note = f"correctness reject note {RUN_ID[-6:]}"
            self.call_success(
                "admin reject reviewing candidate",
                "POST",
                f"/admin/learning-assets/candidates/{knowledge_nodes[0]['id']}/reject",
                body={"note": reject_note},
                token=self.admin_token,
                assertion="admin can reject a reviewing candidate with note",
            )
            detail = require_dict(
                self.call_success(
                    "read record after admin reject",
                    "GET",
                    f"/user/learning-assets/records/{record_id}",
                    token=user_token,
                    assertion="user record detail reflects admin reject note and final partial status",
                ),
                "read record after admin reject",
            )
            if detail.get("status") != "PARTIAL_PUBLISHED" or detail.get("publishedCandidates") != 2:
                raise HarnessFailure("record after reject: expected PARTIAL_PUBLISHED with two published candidates")
            rejected = find_by(require_list(detail.get("candidates"), "detail candidates after reject"), "id", knowledge_nodes[0]["id"])
            if not rejected or rejected.get("status") != "REJECTED" or rejected.get("reviewNote") != reject_note:
                raise HarnessFailure("record after reject: knowledge candidate should be REJECTED with note")

            stats = require_dict(
                self.call_success(
                    "admin get learning asset statistics",
                    "GET",
                    "/admin/learning-assets/statistics",
                    token=self.admin_token,
                    assertion="statistics reflect at least the transform and candidates created in this run",
                ),
                "admin get learning asset statistics",
            )
            overview = require_dict(stats.get("overview"), "learning asset statistics overview")
            if overview.get("totalTransforms", 0) < 1 or overview.get("totalCandidates", 0) < len(candidates):
                raise HarnessFailure("statistics overview: transform/candidate counts too small")
            source_stats = require_list(stats.get("sourceStats"), "learning asset sourceStats")
            asset_stats = require_list(stats.get("assetStats"), "learning asset assetStats")
            if not find_by(source_stats, "sourceType", "blog"):
                raise HarnessFailure("statistics sourceStats: blog source stat missing")
            for asset_type in ("flashcard", "practice_plan", "knowledge_node"):
                if not find_by(asset_stats, "assetType", asset_type):
                    raise HarnessFailure(f"statistics assetStats: {asset_type} stat missing")

        except Exception as e:
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
            "service": "learning-asset-controller",
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
    parser = argparse.ArgumentParser(description="Run learning-asset service correctness checks")
    parser.add_argument("--base-url", default="http://127.0.0.1:9999/api")
    parser.add_argument("--out-dir", required=True)
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    harness = LearningAssetCorrectnessHarness(args.base_url, args.out_dir)
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
