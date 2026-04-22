from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from fastapi.testclient import TestClient

from app.config import ServiceSettings
from app.main import create_app


class LlamaIndexServiceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.data_file = Path(self.temp_dir.name) / "knowledge-base.json"
        settings = ServiceSettings(
            service_api_key="secret-key",
            data_file=self.data_file,
            max_top_k=10,
        )
        self.client = TestClient(create_app(settings))
        self.headers = {"Authorization": "Bearer secret-key"}

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_health_should_return_runtime_summary(self) -> None:
        response = self.client.get("/health")

        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["status"], "ok")
        self.assertTrue(payload["authEnabled"])
        self.assertEqual(payload["documentCount"], 0)
        self.assertEqual(payload["sceneCount"], 0)

    def test_retrieve_should_require_api_key_when_configured(self) -> None:
        response = self.client.post("/api/v1/retrieve", json={"query": "缓存一致性"})

        self.assertEqual(response.status_code, 401)
        self.assertIn("Bearer", response.headers["www-authenticate"])

    def test_should_import_documents_and_retrieve_by_scene_and_metadata(self) -> None:
        import_response = self.client.post(
            "/api/v1/admin/documents/import",
            headers=self.headers,
            json={
                "documents": [
                    {
                        "id": "doc-cache",
                        "scene": "mock_interview",
                        "text": "缓存一致性常见方案包括旁路缓存、延迟双删和消息最终一致性。",
                        "metadata": {"level": "senior", "source": "handbook"},
                    },
                    {
                        "id": "doc-sql",
                        "scene": "sql_optimize",
                        "text": "Using filesort 通常意味着排序未完全命中索引，需要结合过滤列设计联合索引。",
                        "metadata": {"level": "senior", "source": "mysql-manual"},
                    },
                ]
            },
        )

        self.assertEqual(import_response.status_code, 200)
        self.assertEqual(import_response.json()["importedCount"], 2)
        self.assertEqual(import_response.json()["totalCount"], 2)

        list_response = self.client.get("/api/v1/admin/documents", headers=self.headers)
        self.assertEqual(list_response.status_code, 200)
        self.assertEqual(list_response.json()["totalCount"], 2)

        retrieve_response = self.client.post(
            "/api/v1/retrieve",
            headers=self.headers,
            json={
                "query": "缓存一致性",
                "scene": "mock_interview",
                "topK": 3,
                "metadataFilters": {"level": "senior"},
            },
        )

        self.assertEqual(retrieve_response.status_code, 200)
        payload = retrieve_response.json()
        self.assertFalse(payload["fallback"])
        self.assertEqual(len(payload["nodes"]), 1)
        self.assertEqual(payload["nodes"][0]["id"], "doc-cache")
        self.assertEqual(payload["nodes"][0]["metadata"]["source"], "handbook")
        self.assertIn("缓存一致性", payload["nodes"][0]["matchedTerms"])
        self.assertIn("textTermScore", payload["nodes"][0]["scoreBreakdown"])
        self.assertEqual(payload["nodes"][0]["bestMatchField"], "text")
        self.assertIn("缓存一致性", payload["nodes"][0]["bestSnippet"])

    def test_import_should_support_replace_mode(self) -> None:
        self.client.post(
            "/api/v1/admin/documents/import",
            headers=self.headers,
            json={
                "documents": [
                    {"id": "doc-1", "text": "第一版文档", "scene": "community"},
                    {"id": "doc-2", "text": "第二版文档", "scene": "community"},
                ]
            },
        )

        replace_response = self.client.post(
            "/api/v1/admin/documents/import",
            headers=self.headers,
            json={
                "replace": True,
                "documents": [
                    {"id": "doc-3", "text": "替换后的文档", "scene": "job_battle"},
                ],
            },
        )

        self.assertEqual(replace_response.status_code, 200)
        self.assertEqual(replace_response.json()["totalCount"], 1)

        list_response = self.client.get("/api/v1/admin/documents", headers=self.headers)
        self.assertEqual(list_response.json()["totalCount"], 1)
        self.assertEqual(list_response.json()["documents"][0]["id"], "doc-3")

    def test_retrieve_should_prioritize_exact_phrase_and_expose_score_breakdown(self) -> None:
        self.client.post(
            "/api/v1/admin/documents/import",
            headers=self.headers,
            json={
                "replace": True,
                "documents": [
                    {
                        "id": "doc-phrase",
                        "scene": "sql_optimize",
                        "text": "Using filesort 通常意味着排序未命中索引，需要优先检查联合索引设计。",
                        "metadata": {"source": "mysql-manual", "tags": ["filesort", "index"]},
                    },
                    {
                        "id": "doc-generic",
                        "scene": "sql_optimize",
                        "text": "SQL 优化要关注索引、回表和扫描行数。",
                        "metadata": {"source": "generic"},
                    },
                ]
            },
        )

        retrieve_response = self.client.post(
            "/api/v1/retrieve",
            headers=self.headers,
            json={
                "query": "using filesort",
                "scene": "sql_optimize",
                "topK": 2,
            },
        )

        self.assertEqual(retrieve_response.status_code, 200)
        payload = retrieve_response.json()
        self.assertFalse(payload["fallback"])
        self.assertEqual(payload["nodes"][0]["id"], "doc-phrase")
        self.assertIn("exactTextMatch", payload["nodes"][0]["scoreBreakdown"])
        self.assertIn("metadataTagScore", payload["nodes"][0]["scoreBreakdown"])
        self.assertNotIn("using", payload["nodes"][0]["matchedTerms"])
        self.assertIn("filesort", payload["nodes"][0]["matchedTerms"])
        self.assertEqual(payload["nodes"][0]["bestMatchField"], "text")
        self.assertIn("Using filesort", payload["nodes"][0]["bestSnippet"])

    def test_retrieve_should_ignore_low_signal_english_stopwords(self) -> None:
        self.client.post(
            "/api/v1/admin/documents/import",
            headers=self.headers,
            json={
                "replace": True,
                "documents": [
                    {
                        "id": "doc-using-only",
                        "scene": "sql_optimize",
                        "text": "Using 只是一个英语连接词，本身不能作为 SQL 调优依据。",
                        "metadata": {"source": "noise"},
                    },
                    {
                        "id": "doc-filesort-core",
                        "scene": "sql_optimize",
                        "text": "filesort 说明排序阶段没有完全走索引，要重点检查排序列和过滤列的联合索引。",
                        "metadata": {"tags": ["filesort", "index"], "source": "mysql-manual"},
                    },
                ]
            },
        )

        retrieve_response = self.client.post(
            "/api/v1/retrieve",
            headers=self.headers,
            json={
                "query": "using filesort",
                "scene": "sql_optimize",
                "topK": 2,
            },
        )

        self.assertEqual(retrieve_response.status_code, 200)
        payload = retrieve_response.json()
        self.assertEqual(payload["nodes"][0]["id"], "doc-filesort-core")
        self.assertEqual(payload["nodes"][0]["bestMatchField"], "text")
        self.assertNotIn("using", payload["nodes"][0]["matchedTerms"])

    def test_should_export_full_documents_by_scene(self) -> None:
        self.client.post(
            "/api/v1/admin/documents/import",
            headers=self.headers,
            json={
                "replace": True,
                "documents": [
                    {
                        "id": "doc-export-1",
                        "scene": "community",
                        "text": "社区发帖总结需要兼顾观点提炼和风险表达。",
                        "metadata": {"source": "ops-manual"},
                    },
                    {
                        "id": "doc-export-2",
                        "scene": "mock_interview",
                        "text": "模拟面试追问时要优先围绕候选人的项目细节展开。",
                        "metadata": {"source": "interview-playbook"},
                    },
                ]
            },
        )

        export_response = self.client.get(
            "/api/v1/admin/documents/export",
            headers=self.headers,
            params={"scene": "community"},
        )

        self.assertEqual(export_response.status_code, 200)
        payload = export_response.json()
        self.assertEqual(payload["totalCount"], 1)
        self.assertEqual(payload["documents"][0]["id"], "doc-export-1")
        self.assertEqual(payload["documents"][0]["text"], "社区发帖总结需要兼顾观点提炼和风险表达。")
        self.assertEqual(payload["documents"][0]["metadata"]["source"], "ops-manual")

    def test_should_delete_document_by_id(self) -> None:
        self.client.post(
            "/api/v1/admin/documents/import",
            headers=self.headers,
            json={
                "replace": True,
                "documents": [
                    {"id": "doc-delete-1", "scene": "community", "text": "待删除文档"},
                    {"id": "doc-delete-2", "scene": "community", "text": "保留文档"},
                ]
            },
        )

        delete_response = self.client.delete(
            "/api/v1/admin/documents",
            headers=self.headers,
            params={"documentId": "doc-delete-1"},
        )

        self.assertEqual(delete_response.status_code, 200)
        payload = delete_response.json()
        self.assertEqual(payload["documentId"], "doc-delete-1")
        self.assertEqual(payload["deletedCount"], 1)
        self.assertEqual(payload["totalCount"], 1)

        list_response = self.client.get("/api/v1/admin/documents", headers=self.headers)
        self.assertEqual(list_response.status_code, 200)
        self.assertEqual(list_response.json()["totalCount"], 1)
        self.assertEqual(list_response.json()["documents"][0]["id"], "doc-delete-2")

    def test_should_return_not_found_when_deleting_missing_document(self) -> None:
        delete_response = self.client.delete(
            "/api/v1/admin/documents",
            headers=self.headers,
            params={"documentId": "missing-doc"},
        )

        self.assertEqual(delete_response.status_code, 404)
        self.assertEqual(delete_response.json()["detail"], "未找到文档: missing-doc")

    def test_should_batch_delete_documents_and_report_missing_ids(self) -> None:
        self.client.post(
            "/api/v1/admin/documents/import",
            headers=self.headers,
            json={
                "replace": True,
                "documents": [
                    {"id": "doc-batch-1", "scene": "community", "text": "批量删除文档 1"},
                    {"id": "doc-batch-2", "scene": "community", "text": "批量删除文档 2"},
                    {"id": "doc-batch-3", "scene": "community", "text": "保留文档"},
                ]
            },
        )

        batch_delete_response = self.client.post(
            "/api/v1/admin/documents/batch-delete",
            headers=self.headers,
            json={"documentIds": ["doc-batch-1", "missing-doc", "doc-batch-2", "doc-batch-1"]},
        )

        self.assertEqual(batch_delete_response.status_code, 200)
        payload = batch_delete_response.json()
        self.assertEqual(payload["requestedCount"], 3)
        self.assertEqual(payload["deletedCount"], 2)
        self.assertEqual(payload["deletedDocumentIds"], ["doc-batch-1", "doc-batch-2"])
        self.assertEqual(payload["missingCount"], 1)
        self.assertEqual(payload["missingDocumentIds"], ["missing-doc"])
        self.assertEqual(payload["totalCount"], 1)

        list_response = self.client.get("/api/v1/admin/documents", headers=self.headers)
        self.assertEqual(list_response.status_code, 200)
        self.assertEqual(list_response.json()["totalCount"], 1)
        self.assertEqual(list_response.json()["documents"][0]["id"], "doc-batch-3")


if __name__ == "__main__":
    unittest.main()
