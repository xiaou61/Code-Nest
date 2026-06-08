#!/usr/bin/env python3
"""Service-level correctness harness for blog APIs.

This is stricter than HTTP smoke coverage:
- blog opening, config updates, draft/publish flows, admin operations, and cleanup
  must be read back through the API.
- public/user/admin article lists must prove field and state consistency.
- expected auth, validation, and business errors are counted separately.
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


def find_by_title(items: list[Any], title: str) -> dict[str, Any] | None:
    return find_by(items, "title", title)


class BlogCorrectnessHarness:
    def __init__(self, base_url: str, out_dir: str):
        self.base_url = base_url.rstrip("/")
        self.out_dir = out_dir
        self.results: list[dict[str, Any]] = []
        self.context: dict[str, Any] = {"runId": RUN_ID, "baseUrl": self.base_url}
        self.admin_token: str | None = None
        self.author: dict[str, Any] = {}
        self.reader: dict[str, Any] = {}
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
                assertion="seed admin can log in for blog management",
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

    def register_and_login_user(self, suffix: str) -> dict[str, Any]:
        username = f"blog{suffix}{RUN_ID[-8:]}"
        phone_prefix = "172" if suffix == "a" else "173"
        phone = phone_prefix + RUN_ID[-8:]

        key, code = generate_captcha(self.base_url)
        registered = require_dict(
            self.call_success(
                f"register blog user {suffix}",
                "POST",
                "/user/auth/register",
                body={
                    "username": username,
                    "password": TEST_PASSWORD,
                    "confirmPassword": TEST_PASSWORD,
                    "nickname": f"Blog User {suffix.upper()} {RUN_ID[-4:]}",
                    "email": f"{username}@example.com",
                    "phone": phone,
                    "captcha": code,
                    "captchaKey": key,
                },
                assertion="new isolated user can be registered for blog flow",
            ),
            f"register blog user {suffix}",
        )
        user_id = registered.get("id")
        if not isinstance(user_id, int):
            raise HarnessFailure(f"register blog user {suffix}: missing numeric user id")

        key, code = generate_captcha(self.base_url)
        login = require_dict(
            self.call_success(
                f"login blog user {suffix}",
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
            f"login blog user {suffix}",
        )
        token = login.get("accessToken")
        if not isinstance(token, str) or not token:
            raise HarnessFailure(f"login blog user {suffix}: missing accessToken")
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

    def grant_points(self, user_id: int, points: int, reason: str) -> dict[str, Any]:
        if not self.admin_token:
            raise HarnessFailure("grant points: admin token missing")
        grant = require_dict(
            self.call_success(
                f"admin grants {points} points",
                "POST",
                "/admin/points/grant",
                body={"userId": user_id, "points": points, "reason": reason},
                token=self.admin_token,
                assertion="admin grant readback returns detail id and updated user balance",
            ),
            f"admin grants {points} points",
        )
        if not isinstance(grant.get("detailId"), int) or grant.get("userBalance") is None:
            raise HarnessFailure("admin grant points: missing detailId or userBalance")
        return grant

    def ensure_blog_category(self) -> dict[str, Any]:
        category_name = f"Blog Correctness {RUN_ID[-6:]}"
        self.call_success(
            "admin creates blog category fixture",
            "POST",
            "/admin/blog/category/create",
            body={
                "categoryName": category_name,
                "categoryIcon": "",
                "categoryDescription": "blog correctness fixture",
                "sortOrder": 900,
            },
            token=self.admin_token,
            assertion="admin can create a category fixture for blog article flows",
        )

        categories = require_list(
            self.call_success(
                "user reads blog categories after fixture",
                "GET",
                "/user/blog/categories",
                token=self.author.get("token"),
                assertion="created category is visible through user category API",
            ),
            "user reads blog categories after fixture",
        )
        category = find_by(categories, "categoryName", category_name)
        if not category or not isinstance(category.get("id"), int):
            raise HarnessFailure("blog category fixture: created category missing from user list")
        if category.get("status") != 1:
            raise HarnessFailure("blog category fixture: created category should be enabled")
        self.created_ids["categoryId"] = category["id"]
        self.created_ids["categoryName"] = category_name
        return category

    def category_by_id(self, category_id: int, step: str) -> dict[str, Any]:
        page = require_dict(
            self.call_success(
                step,
                "GET",
                "/admin/blog/category/list",
                query={"pageNum": 1, "pageSize": 200},
                token=self.admin_token,
                assertion="admin category list supports readback by id",
            ),
            step,
        )
        for row in page_records(page, step):
            if isinstance(row, dict) and row.get("id") == category_id:
                return row
        raise HarnessFailure(f"{step}: category {category_id} not found")

    def open_blog_and_update_config(self) -> dict[str, Any]:
        token = self.author["token"]
        user_id = self.author["id"]

        self.assert_balance(token, 0, "initial author points balance")
        grant = self.grant_points(user_id, 120, f"blog correctness grant {RUN_ID[-6:]}")
        if grant.get("userBalance") != 120:
            raise HarnessFailure(f"admin grant for blog: expected userBalance=120, got {grant.get('userBalance')!r}")
        self.assert_balance(token, 120, "author balance after grant")

        closed = require_dict(
            self.call_success(
                "check blog status before open",
                "GET",
                "/user/blog/check-status",
                token=token,
                assertion="unopened author reports isOpened=false and open cost",
            ),
            "check blog status before open",
        )
        if closed.get("isOpened") is not False or closed.get("requiredPoints") != 50 or closed.get("canOpen") is not True:
            raise HarnessFailure("check blog status before open: expected canOpen unopened state")

        opened = require_dict(
            self.call_success(
                "open blog",
                "POST",
                "/user/blog/open",
                token=token,
                assertion="opening blog deducts 50 points and returns blog id",
            ),
            "open blog",
        )
        blog_id = opened.get("blogId")
        if not isinstance(blog_id, int) or opened.get("userId") != user_id or opened.get("pointsRemaining") != 70:
            raise HarnessFailure(f"open blog: id/user/remaining mismatch: {opened}")
        self.created_ids["blogId"] = blog_id
        self.assert_balance(token, 70, "author balance after opening blog")

        opened_status = require_dict(
            self.call_success(
                "check blog status after open",
                "GET",
                "/user/blog/check-status",
                token=token,
                assertion="opened status readback returns same blog id",
            ),
            "check blog status after open",
        )
        if opened_status.get("isOpened") is not True or opened_status.get("blogId") != blog_id:
            raise HarnessFailure("check blog status after open: opened flag or blogId mismatch")

        blog_name = f"Correct Blog {RUN_ID[-6:]}"
        self.call_success(
            "update blog config",
            "POST",
            "/user/blog/config/update",
            body={
                "blogName": blog_name,
                "blogDescription": "service correctness blog config",
                "blogNotice": f"notice {RUN_ID[-4:]}",
                "personalTags": ["java", "testing"],
                "socialLinks": {"github": "https://example.com/code-nest"},
                "isPublic": 1,
            },
            token=token,
            assertion="user can update blog configuration",
        )
        config = require_dict(
            self.call_success(
                "read blog config after update",
                "GET",
                f"/user/blog/config/{user_id}",
                token=token,
                assertion="blog config update is readable by user id",
            ),
            "read blog config after update",
        )
        if config.get("userId") != user_id or config.get("blogName") != blog_name:
            raise HarnessFailure("blog config readback: userId or blogName mismatch")
        if config.get("personalTags") != ["java", "testing"]:
            raise HarnessFailure("blog config readback: personalTags did not persist")
        if not isinstance(config.get("socialLinks"), dict) or config["socialLinks"].get("github") != "https://example.com/code-nest":
            raise HarnessFailure("blog config readback: socialLinks did not persist")
        return config

    def article_body(self, category_id: int, title: str, *, tag: str, summary: str | None = None) -> dict[str, Any]:
        # Keep the article body free of short common substrings that may collide with
        # the configured sensitive-word library during publish validation.
        content = ("0123456789 " * 6) + RUN_ID
        return {
            "title": title,
            "summary": summary or f"Summary for {title}",
            "content": content,
            "categoryId": category_id,
            "tags": [tag],
            "isOriginal": 1,
        }

    def create_draft_and_publish(self, category_id: int) -> dict[str, Any]:
        token = self.author["token"]
        user_id = self.author["id"]
        title = f"Blog Correctness Article {RUN_ID[-6:]}"
        tag = f"correct-{RUN_ID[-5:]}"

        draft_id = self.call_success(
            "create blog draft",
            "POST",
            "/user/blog/article/create",
            body=self.article_body(category_id, title, tag=tag),
            token=token,
            assertion="creating a draft returns a numeric article id",
        )
        if not isinstance(draft_id, int):
            raise HarnessFailure("create blog draft: expected numeric article id")
        self.created_ids["articleId"] = draft_id
        self.created_ids["articleTitle"] = title
        self.created_ids["articleTag"] = tag

        draft_detail = require_dict(
            self.call_success(
                "read blog draft detail by owner",
                "GET",
                f"/user/blog/article/{draft_id}",
                token=token,
                assertion="draft detail is readable by owner before publish",
            ),
            "read blog draft detail by owner",
        )
        if draft_detail.get("id") != draft_id or draft_detail.get("userId") != user_id or draft_detail.get("title") != title:
            raise HarnessFailure("draft detail: id/user/title mismatch")
        if draft_detail.get("canEdit") is not True or draft_detail.get("canDelete") is not True:
            raise HarnessFailure("draft detail: owner should be able to edit and delete")

        self.call_expected_error(
            "reader cannot read another user's draft",
            "GET",
            f"/user/blog/article/{draft_id}",
            token=self.reader["token"],
            message_part="文章不存在或已下架",
            assertion="drafts are not visible to non-authors",
        )

        drafts = page_records(
            self.call_success(
                "my draft list includes created draft",
                "POST",
                "/user/blog/article/draft-list",
                query={"pageNum": 1, "pageSize": 10},
                token=token,
                assertion="draft list includes the created draft",
            ),
            "my draft list includes created draft",
        )
        if not find_by(drafts, "id", draft_id):
            raise HarnessFailure("my draft list: created draft missing")

        published = require_dict(
            self.call_success(
                "publish blog draft",
                "POST",
                "/user/blog/article/publish",
                body={**self.article_body(category_id, title, tag=tag), "id": draft_id},
                token=token,
                assertion="publishing a draft returns same article id and remaining points",
            ),
            "publish blog draft",
        )
        if published.get("articleId") != draft_id or published.get("pointsRemaining") != 50:
            raise HarnessFailure(f"publish blog draft: expected articleId={draft_id}, pointsRemaining=50, got {published}")
        self.assert_balance(token, 50, "author balance after publish")

        detail = require_dict(
            self.call_success(
                "read published article detail",
                "GET",
                f"/user/blog/article/{draft_id}",
                token=token,
                assertion="published detail contains title/category/tags and owner permissions",
            ),
            "read published article detail",
        )
        if detail.get("id") != draft_id or detail.get("categoryId") != category_id:
            raise HarnessFailure("published detail: id/category mismatch")
        if detail.get("tags") != [tag] or detail.get("canEdit") is not True:
            raise HarnessFailure("published detail: tags or canEdit mismatch")

        public_detail = require_dict(
            self.call_success(
                "reader reads published article detail",
                "GET",
                f"/user/blog/article/{draft_id}",
                token=self.reader["token"],
                assertion="published detail is visible to a different user",
            ),
            "reader reads published article detail",
        )
        if public_detail.get("id") != draft_id or public_detail.get("canEdit") is not False:
            raise HarnessFailure("reader published detail: id/canEdit mismatch")

        config_after_publish = require_dict(
            self.call_success(
                "blog config total articles after publish",
                "GET",
                f"/user/blog/config/{user_id}",
                token=token,
                assertion="blog config totalArticles increments after publishing",
            ),
            "blog config total articles after publish",
        )
        if config_after_publish.get("totalArticles") != 1:
            raise HarnessFailure("blog config after publish: expected totalArticles=1")

        category_after_publish = self.category_by_id(category_id, "category count after publish")
        if category_after_publish.get("articleCount") != 1:
            raise HarnessFailure(f"category count after publish: expected articleCount=1, got {category_after_publish.get('articleCount')!r}")

        hot_tags = require_list(
            self.call_success(
                "hot tags include published article tag",
                "GET",
                "/user/blog/tags/hot",
                query={"limit": 20},
                token=token,
                assertion="tag use count is updated after publish",
            ),
            "hot tags include published article tag",
        )
        tag_row = find_by(hot_tags, "tagName", tag)
        if not tag_row or tag_row.get("useCount") != 1:
            raise HarnessFailure("hot tags after publish: created tag missing or useCount mismatch")

        return {"id": draft_id, "title": title, "tag": tag}

    def assert_article_in_public_lists(self, article: dict[str, Any], category_id: int) -> None:
        token = self.author["token"]
        user_id = self.author["id"]
        article_id = article["id"]
        title = article["title"]
        tag = article["tag"]

        public_records = page_records(
            self.call_success(
                "public user article list includes published article",
                "POST",
                "/user/blog/article/list",
                body={"userId": user_id, "pageNum": 1, "pageSize": 10},
                token=token,
                assertion="public user article list returns the published article",
            ),
            "public user article list includes published article",
        )
        public_row = find_by(public_records, "id", article_id)
        if not public_row or public_row.get("title") != title or public_row.get("categoryId") != category_id:
            raise HarnessFailure("public article list: published article missing or fields mismatch")

        my_records = page_records(
            self.call_success(
                "my published article list includes article",
                "POST",
                "/user/blog/article/my-list",
                body={"pageNum": 1, "pageSize": 10},
                token=token,
                assertion="my-list returns the published article for the author",
            ),
            "my published article list includes article",
        )
        if not find_by(my_records, "id", article_id):
            raise HarnessFailure("my published article list: article missing")

        category_records = page_records(
            self.call_success(
                "public by-category list includes article",
                "POST",
                "/user/blog/article/by-category",
                query={"categoryId": category_id, "pageNum": 1, "pageSize": 10},
                token=token,
                assertion="category listing returns published articles by category",
            ),
            "public by-category list includes article",
        )
        if not find_by(category_records, "id", article_id):
            raise HarnessFailure("public by-category list: article missing")

        tag_records = page_records(
            self.call_success(
                "public article list filters by tag",
                "POST",
                "/user/blog/article/list",
                body={"userId": user_id, "tags": tag, "pageNum": 1, "pageSize": 10},
                token=token,
                assertion="tag filter returns the published article",
            ),
            "public article list filters by tag",
        )
        if not find_by(tag_records, "id", article_id):
            raise HarnessFailure("public tag-filtered list: article missing")

        admin_records = page_records(
            self.call_success(
                "admin article list filters by keyword",
                "POST",
                "/admin/blog/article/list",
                body={"keyword": title[-6:], "pageNum": 1, "pageSize": 20},
                token=self.admin_token,
                assertion="admin list keyword search returns the article",
            ),
            "admin article list filters by keyword",
        )
        admin_row = find_by(admin_records, "id", article_id)
        if not admin_row or admin_row.get("title") != title:
            raise HarnessFailure("admin article list: keyword search did not return article")

    def admin_status_flow_and_delete(self, article: dict[str, Any], category_id: int) -> None:
        token = self.author["token"]
        article_id = article["id"]
        title = article["title"]
        user_id = self.author["id"]

        self.call_success(
            "admin tops blog article",
            "POST",
            "/admin/blog/article/top",
            query={"id": article_id, "duration": 1},
            token=self.admin_token,
            assertion="admin can set a published article as top",
        )
        topped_records = page_records(
            self.call_success(
                "admin list reads topped article",
                "POST",
                "/admin/blog/article/list",
                body={"userId": user_id, "pageNum": 1, "pageSize": 20},
                token=self.admin_token,
                assertion="admin list readback shows isTop=1",
            ),
            "admin list reads topped article",
        )
        topped = find_by(topped_records, "id", article_id)
        if not topped or topped.get("isTop") != 1:
            raise HarnessFailure("admin list after top: isTop did not persist")

        self.call_success(
            "admin cancels blog article top",
            "POST",
            "/admin/blog/article/cancel-top",
            query={"id": article_id},
            token=self.admin_token,
            assertion="admin can cancel top state",
        )
        untopped_records = page_records(
            self.call_success(
                "admin list reads untopped article",
                "POST",
                "/admin/blog/article/list",
                body={"userId": user_id, "pageNum": 1, "pageSize": 20},
                token=self.admin_token,
                assertion="admin list readback shows isTop=0",
            ),
            "admin list reads untopped article",
        )
        untopped = find_by(untopped_records, "id", article_id)
        if not untopped or untopped.get("isTop") != 0:
            raise HarnessFailure("admin list after cancel top: isTop did not clear")

        self.call_success(
            "admin takes blog article offline",
            "POST",
            "/admin/blog/article/update-status",
            query={"id": article_id, "status": 2},
            token=self.admin_token,
            assertion="admin can update article status to offline",
        )
        offline_records = page_records(
            self.call_success(
                "admin list filters offline article",
                "POST",
                "/admin/blog/article/list",
                body={"userId": user_id, "status": 2, "pageNum": 1, "pageSize": 20},
                token=self.admin_token,
                assertion="admin status filter returns offline article",
            ),
            "admin list filters offline article",
        )
        if not find_by(offline_records, "id", article_id):
            raise HarnessFailure("admin offline list: article missing")

        public_after_offline = page_records(
            self.call_success(
                "public list hides offline article",
                "POST",
                "/user/blog/article/list",
                body={"userId": user_id, "pageNum": 1, "pageSize": 10},
                token=token,
                assertion="public list should not expose offline articles",
            ),
            "public list hides offline article",
        )
        if find_by(public_after_offline, "id", article_id):
            raise HarnessFailure("public list after offline: offline article is still visible")

        self.call_expected_error(
            "reader cannot read offline article",
            "GET",
            f"/user/blog/article/{article_id}",
            token=self.reader["token"],
            message_part="文章不存在或已下架",
            assertion="offline article detail is hidden from non-authors",
        )

        self.call_success(
            "admin republishes blog article",
            "POST",
            "/admin/blog/article/update-status",
            query={"id": article_id, "status": 1},
            token=self.admin_token,
            assertion="admin can restore article status to published",
        )
        restored = page_records(
            self.call_success(
                "public list shows republished article",
                "POST",
                "/user/blog/article/list",
                body={"userId": user_id, "pageNum": 1, "pageSize": 10},
                token=token,
                assertion="public list shows article after status is restored",
            ),
            "public list shows republished article",
        )
        if not find_by(restored, "id", article_id):
            raise HarnessFailure("public list after republish: article missing")

        self.call_success(
            "admin deletes blog article",
            "DELETE",
            f"/admin/blog/article/{article_id}",
            token=self.admin_token,
            assertion="admin can soft-delete article",
        )

        deleted_admin_records = page_records(
            self.call_success(
                "admin list filters deleted article",
                "POST",
                "/admin/blog/article/list",
                body={"userId": user_id, "status": 3, "pageNum": 1, "pageSize": 20},
                token=self.admin_token,
                assertion="admin deleted status filter returns deleted article",
            ),
            "admin list filters deleted article",
        )
        if not find_by(deleted_admin_records, "id", article_id):
            raise HarnessFailure("admin deleted list: deleted article missing")

        public_after_delete = page_records(
            self.call_success(
                "public list hides deleted article",
                "POST",
                "/user/blog/article/list",
                body={"userId": user_id, "pageNum": 1, "pageSize": 10},
                token=token,
                assertion="public list should not expose deleted articles",
            ),
            "public list hides deleted article",
        )
        if find_by(public_after_delete, "id", article_id):
            raise HarnessFailure("public list after delete: deleted article is still visible")

        config_after_delete = require_dict(
            self.call_success(
                "blog config total articles after delete",
                "GET",
                f"/user/blog/config/{user_id}",
                token=token,
                assertion="blog totalArticles decrements after deleting a published article",
            ),
            "blog config total articles after delete",
        )
        if config_after_delete.get("totalArticles") != 0:
            raise HarnessFailure("blog config after delete: expected totalArticles=0")

        category_after_delete = self.category_by_id(category_id, "category count after delete")
        if category_after_delete.get("articleCount") != 0:
            raise HarnessFailure(
                f"category count after delete: expected articleCount=0, got {category_after_delete.get('articleCount')!r}"
            )

        self.call_expected_error(
            "reader cannot read deleted article",
            "GET",
            f"/user/blog/article/{article_id}",
            token=self.reader["token"],
            message_part="文章不存在或已下架",
            assertion="deleted article detail is hidden from non-authors",
        )

    def expected_error_checks(self, category_id: int) -> None:
        token = self.author["token"]
        self.call_expected_error(
            "anonymous blog status requires auth",
            "GET",
            "/user/blog/check-status",
            message_part="Token",
            assertion="anonymous user cannot read private blog status",
        )
        self.call_expected_error(
            "duplicate blog open rejected",
            "POST",
            "/user/blog/open",
            token=token,
            message_part="已开通博客",
            assertion="opening an already opened blog is rejected",
        )
        self.call_expected_error(
            "short article content rejected",
            "POST",
            "/user/blog/article/publish",
            body={
                "title": "Too short",
                "content": "short",
                "categoryId": category_id,
                "tags": ["short"],
                "isOriginal": 1,
            },
            token=token,
            message_part="文章内容不能少于",
            assertion="article publish validates minimum content length",
        )
        self.call_expected_error(
            "admin category duplicate rejected",
            "POST",
            "/admin/blog/category/create",
            body={
                "categoryName": self.created_ids["categoryName"],
                "categoryIcon": "",
                "categoryDescription": "duplicate category",
                "sortOrder": 901,
            },
            token=self.admin_token,
            message_part="分类名称已存在",
            assertion="duplicate blog category names are rejected",
        )

    def cleanup(self) -> None:
        category_id = self.created_ids.get("categoryId")
        if category_id and self.admin_token:
            http = http_request(self.base_url, "DELETE", f"/admin/blog/category/{category_id}", token=self.admin_token)
            self.record(
                "cleanup delete blog category",
                "DELETE",
                f"/admin/blog/category/{category_id}",
                http,
                "cleanup removes unused category fixture",
                "PASS" if http.ok and result_code(http.json_body) == 200 else "PASS_EXPECTED_ERROR",
                expected="SUCCESS" if http.ok and result_code(http.json_body) == 200 else "CLEANUP_NOOP",
            )

    def run(self) -> dict[str, Any]:
        failure: str | None = None
        try:
            self.admin_login()
            self.author = self.register_and_login_user("a")
            self.reader = self.register_and_login_user("b")
            self.context["authorUserId"] = self.author["id"]
            self.context["readerUserId"] = self.reader["id"]

            self.call_expected_error(
                "reader config before open rejected",
                "GET",
                f"/user/blog/config/{self.reader['id']}",
                token=self.reader["token"],
                message_part="未开通博客",
                assertion="unopened user has no public blog config",
            )

            category = self.ensure_blog_category()
            category_id = category["id"]
            self.open_blog_and_update_config()
            article = self.create_draft_and_publish(category_id)
            self.assert_article_in_public_lists(article, category_id)
            self.expected_error_checks(category_id)
            self.admin_status_flow_and_delete(article, category_id)
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

    def sanitized_context(self) -> dict[str, Any]:
        context = dict(self.context)
        for key, value in (("author", self.author), ("reader", self.reader)):
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
            "service": "blog-controller",
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
    parser = argparse.ArgumentParser(description="Run blog service correctness checks")
    parser.add_argument("--base-url", default="http://127.0.0.1:9999/api")
    parser.add_argument("--out-dir", required=True)
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    harness = BlogCorrectnessHarness(args.base_url, args.out_dir)
    try:
        summary = harness.run()
    except Exception:
        summary = harness.summary(traceback.format_exc())
    print(json.dumps(summary, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
