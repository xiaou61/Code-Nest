from __future__ import annotations

import math
import json
import re
from collections import Counter
from threading import RLock
from typing import Optional
from typing import Set
from typing import Tuple

from app.config import ServiceSettings
from app.models import DocumentListItem
from app.models import KnowledgeDocument
from app.models import RetrieveNode
from app.models import RetrieveRequest
from app.models import RetrieveResponse

ASCII_TOKEN_PATTERN = re.compile(r"[a-z0-9_+#.\-]{2,}")
CJK_TOKEN_PATTERN = re.compile(r"[\u4e00-\u9fff]{2,}")
MAX_PREVIEW_LENGTH = 160
SNIPPET_CONTEXT_CHARS = 48
TEXT_TERM_WEIGHT = 1.9
SCENE_TERM_WEIGHT = 1.6
METADATA_TERM_WEIGHT = 1.25
ID_TERM_WEIGHT = 0.85
TITLE_METADATA_TERM_WEIGHT = 1.75
KEYWORD_METADATA_TERM_WEIGHT = 1.7
TAG_METADATA_TERM_WEIGHT = 1.65
SUMMARY_METADATA_TERM_WEIGHT = 1.45
CATEGORY_METADATA_TERM_WEIGHT = 1.2
EXACT_TEXT_MATCH_WEIGHT = 3.6
EXACT_METADATA_MATCH_WEIGHT = 1.8
EXACT_SCENE_MATCH_WEIGHT = 2.1
EXACT_ID_MATCH_WEIGHT = 1.2
TERM_COVERAGE_WEIGHT = 2.4
TERM_DENSITY_WEIGHT = 0.8
METADATA_FILTER_MATCH_WEIGHT = 0.3
ASCII_STOPWORDS = {
    "about",
    "after",
    "and",
    "are",
    "because",
    "before",
    "between",
    "can",
    "does",
    "for",
    "from",
    "how",
    "into",
    "like",
    "need",
    "or",
    "that",
    "the",
    "their",
    "them",
    "this",
    "those",
    "use",
    "used",
    "using",
    "what",
    "when",
    "which",
    "why",
    "with",
}
METADATA_FIELD_WEIGHTS = {
    "title": ("metadataTitleScore", TITLE_METADATA_TERM_WEIGHT),
    "question": ("metadataTitleScore", TITLE_METADATA_TERM_WEIGHT),
    "topic": ("metadataKeywordScore", KEYWORD_METADATA_TERM_WEIGHT),
    "keyword": ("metadataKeywordScore", KEYWORD_METADATA_TERM_WEIGHT),
    "keywords": ("metadataKeywordScore", KEYWORD_METADATA_TERM_WEIGHT),
    "tag": ("metadataTagScore", TAG_METADATA_TERM_WEIGHT),
    "tags": ("metadataTagScore", TAG_METADATA_TERM_WEIGHT),
    "summary": ("metadataSummaryScore", SUMMARY_METADATA_TERM_WEIGHT),
    "category": ("metadataCategoryScore", CATEGORY_METADATA_TERM_WEIGHT),
}


class DocumentStore:
    def __init__(self, settings: ServiceSettings) -> None:
        self.settings = settings
        self._lock = RLock()
        self._documents: list[KnowledgeDocument] = []
        self._loaded = False

    def import_documents(self, documents: list[KnowledgeDocument], replace: bool = False) -> Tuple[int, int]:
        normalized = [self._normalize_document(document) for document in documents]
        with self._lock:
            self._ensure_loaded()

            existing_by_id = {} if replace else {document.id: document for document in self._documents}
            for document in normalized:
                existing_by_id[document.id] = document

            self._documents = sorted(existing_by_id.values(), key=lambda item: item.id)
            self._persist()
            return len(normalized), len(self._documents)

    def list_documents(self, scene: Optional[str] = None, limit: int = 100) -> list[DocumentListItem]:
        with self._lock:
            self._ensure_loaded()
            selected = []
            normalized_scene = (scene or "").strip()
            for document in self._documents:
                if normalized_scene and document.scene != normalized_scene:
                    continue
                selected.append(
                    DocumentListItem(
                        id=document.id,
                        scene=document.scene,
                        textPreview=self._preview_text(document.text),
                        metadata=document.metadata,
                    )
                )
            return selected[: max(1, limit)]

    def export_documents(self, scene: Optional[str] = None) -> list[KnowledgeDocument]:
        with self._lock:
            self._ensure_loaded()
            normalized_scene = (scene or "").strip()
            exported = []
            for document in self._documents:
                if normalized_scene and document.scene != normalized_scene:
                    continue
                exported.append(
                    KnowledgeDocument(
                        id=document.id,
                        text=document.text,
                        scene=document.scene,
                        metadata=dict(document.metadata or {}),
                    )
                )
            return exported

    def delete_document(self, document_id: str) -> Tuple[str, int, int]:
        normalized_document_id = (document_id or "").strip()
        if not normalized_document_id:
            raise ValueError("documentId 不能为空")

        with self._lock:
            self._ensure_loaded()
            existing_document = next(
                (document for document in self._documents if document.id == normalized_document_id),
                None,
            )
            if existing_document is None:
                raise KeyError(f"未找到文档: {normalized_document_id}")

            self._documents = [
                document for document in self._documents
                if document.id != normalized_document_id
            ]
            self._persist()
            return normalized_document_id, 1, len(self._documents)

    def delete_documents(self, document_ids: list[str]) -> Tuple[list[str], list[str], int]:
        normalized_document_ids: list[str] = []
        seen_document_ids: set[str] = set()
        for document_id in document_ids or []:
            normalized_document_id = (document_id or "").strip()
            if not normalized_document_id:
                raise ValueError("documentIds 不能为空，且每个 documentId 都必须有值")
            if normalized_document_id in seen_document_ids:
                continue
            normalized_document_ids.append(normalized_document_id)
            seen_document_ids.add(normalized_document_id)

        if not normalized_document_ids:
            raise ValueError("documentIds 不能为空，且至少需要一条有效 documentId")

        with self._lock:
            self._ensure_loaded()

            existing_document_ids = {document.id for document in self._documents}
            deleted_document_ids = [
                document_id for document_id in normalized_document_ids
                if document_id in existing_document_ids
            ]
            missing_document_ids = [
                document_id for document_id in normalized_document_ids
                if document_id not in existing_document_ids
            ]

            if deleted_document_ids:
                deleted_document_id_set = set(deleted_document_ids)
                self._documents = [
                    document for document in self._documents
                    if document.id not in deleted_document_id_set
                ]
                self._persist()

            return deleted_document_ids, missing_document_ids, len(self._documents)

    def retrieve(self, request: RetrieveRequest) -> RetrieveResponse:
        query = request.query.strip()
        if not query:
            return RetrieveResponse(query="", nodes=[], fallback=True)

        top_k = min(max(1, request.topK or 1), self.settings.max_top_k)
        query_terms = self._extract_terms(query)

        with self._lock:
            self._ensure_loaded()
            candidate_documents = [
                document for document in self._documents
                if self._matches_scene(document, request.scene)
                and self._matches_metadata(document, request.metadataFilters)
            ]
            search_indexes = {
                document.id: self._build_search_index(document)
                for document in candidate_documents
            }
            document_frequency = self._build_document_frequency(search_indexes)
            ranked_nodes = []
            total_documents = max(len(candidate_documents), 1)
            for document in candidate_documents:
                search_index = search_indexes[document.id]
                score, matched_terms, score_breakdown, best_match_field = self._score_document(
                    document=document,
                    query=query,
                    query_terms=query_terms,
                    search_index=search_index,
                    document_frequency=document_frequency,
                    total_documents=total_documents,
                    metadata_filters=request.metadataFilters,
                )
                if score <= 0:
                    continue
                ranked_nodes.append(
                    RetrieveNode(
                        id=document.id,
                        score=round(score, 4),
                        text=document.text,
                        metadata=self._build_node_metadata(document),
                        matchedTerms=matched_terms,
                        scoreBreakdown=score_breakdown,
                        bestMatchField=best_match_field,
                        bestSnippet=self._build_best_snippet(document.text, raw_query=query, matched_terms=matched_terms),
                    )
                )

        ranked_nodes.sort(key=lambda item: item.score, reverse=True)
        nodes = ranked_nodes[:top_k]
        return RetrieveResponse(query=query, nodes=nodes, fallback=not bool(nodes))

    def document_count(self) -> int:
        with self._lock:
            self._ensure_loaded()
            return len(self._documents)

    def scene_count(self) -> int:
        with self._lock:
            self._ensure_loaded()
            return len({document.scene for document in self._documents if document.scene})

    def _ensure_loaded(self) -> None:
        if self._loaded:
            return

        self.settings.data_file.parent.mkdir(parents=True, exist_ok=True)
        if not self.settings.data_file.exists():
            self._documents = []
            self._loaded = True
            return

        raw_text = self.settings.data_file.read_text(encoding="utf-8").strip()
        if not raw_text:
            self._documents = []
            self._loaded = True
            return

        payload = json.loads(raw_text)
        self._documents = [self._model_validate(KnowledgeDocument, item) for item in payload]
        self._loaded = True

    def _persist(self) -> None:
        serialized = [self._model_dump(document) for document in self._documents]
        self.settings.data_file.parent.mkdir(parents=True, exist_ok=True)
        self.settings.data_file.write_text(
            json.dumps(serialized, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    def _normalize_document(self, document: KnowledgeDocument) -> KnowledgeDocument:
        return KnowledgeDocument(
            id=document.id.strip(),
            text=document.text.strip(),
            scene=(document.scene or "").strip() or None,
            metadata=document.metadata or {},
        )

    def _matches_scene(self, document: KnowledgeDocument, scene: Optional[str]) -> bool:
        normalized_scene = (scene or "").strip()
        return not normalized_scene or document.scene == normalized_scene

    def _matches_metadata(self, document: KnowledgeDocument, metadata_filters: dict[str, object]) -> bool:
        for key, expected_value in (metadata_filters or {}).items():
            actual_value = document.metadata.get(key)
            if isinstance(actual_value, list):
                if expected_value not in actual_value:
                    return False
                continue
            if actual_value != expected_value:
                return False
        return True

    def _score_document(
        self,
        document: KnowledgeDocument,
        query: str,
        query_terms: set[str],
        search_index: dict[str, object],
        document_frequency: Counter,
        total_documents: int,
        metadata_filters: dict[str, object],
    ) -> tuple[float, list[str], dict[str, float], Optional[str]]:
        normalized_query = query.lower()
        raw_query = query.strip()
        score_breakdown: dict[str, float] = {}
        field_breakdown = {
            "text": 0.0,
            "metadata": 0.0,
            "scene": 0.0,
            "id": 0.0,
        }
        matched_terms: set[str] = set()

        def add_score(reason: str, value: float, field: Optional[str] = None) -> None:
            if value <= 0:
                return
            score_breakdown[reason] = round(score_breakdown.get(reason, 0.0) + value, 4)
            if field:
                field_breakdown[field] = round(field_breakdown.get(field, 0.0) + value, 4)

        if normalized_query in search_index["text"]:
            matched_terms.add(raw_query)
            add_score("exactTextMatch", EXACT_TEXT_MATCH_WEIGHT, "text")
        if normalized_query in search_index["metadata_text"]:
            matched_terms.add(raw_query)
            add_score("exactMetadataMatch", EXACT_METADATA_MATCH_WEIGHT, "metadata")
        if normalized_query in search_index["scene_text"]:
            matched_terms.add(raw_query)
            add_score("exactSceneMatch", EXACT_SCENE_MATCH_WEIGHT, "scene")
        if normalized_query in search_index["id_text"]:
            matched_terms.add(raw_query)
            add_score("exactIdMatch", EXACT_ID_MATCH_WEIGHT, "id")

        for term in query_terms:
            idf = 1.0 + math.log((total_documents + 1) / (document_frequency.get(term, 0) + 1))

            if term in search_index["text_terms"]:
                matched_terms.add(term)
                add_score("textTermScore", TEXT_TERM_WEIGHT * idf, "text")
            if term in search_index["scene_terms"]:
                matched_terms.add(term)
                add_score("sceneTermScore", SCENE_TERM_WEIGHT * idf, "scene")
            metadata_field_matched = False
            for field_name, (reason, weight) in METADATA_FIELD_WEIGHTS.items():
                field_terms = search_index["metadata_field_terms"].get(field_name, set())
                if term not in field_terms:
                    continue
                metadata_field_matched = True
                matched_terms.add(term)
                add_score(reason, weight * idf, "metadata")
            if not metadata_field_matched and term in search_index["metadata_terms"]:
                matched_terms.add(term)
                add_score("metadataTermScore", METADATA_TERM_WEIGHT * idf, "metadata")
            if term in search_index["id_terms"]:
                matched_terms.add(term)
                add_score("idTermScore", ID_TERM_WEIGHT * idf, "id")

        if matched_terms:
            coverage = min(len(matched_terms) / max(len(query_terms), 1), 1.0)
            density = len(matched_terms) / max(len(search_index["all_terms"]), 1)
            add_score("termCoverage", coverage * TERM_COVERAGE_WEIGHT)
            add_score("termDensity", min(density * 10, 1.0) * TERM_DENSITY_WEIGHT)

        if metadata_filters:
            add_score("metadataFilterMatch", len(metadata_filters) * METADATA_FILTER_MATCH_WEIGHT)

        total_score = round(sum(score_breakdown.values()), 4)
        best_match_field = self._resolve_best_match_field(field_breakdown)
        return total_score, sorted(matched_terms), score_breakdown, best_match_field

    def _build_node_metadata(self, document: KnowledgeDocument) -> dict[str, object]:
        metadata = dict(document.metadata or {})
        if document.scene and "scene" not in metadata:
            metadata["scene"] = document.scene
        return metadata

    def _build_search_index(self, document: KnowledgeDocument) -> dict[str, object]:
        metadata_text, metadata_field_terms = self._build_metadata_index(document.metadata)
        text = document.text or ""
        scene = document.scene or ""
        document_id = document.id or ""

        text_terms = self._extract_terms(text)
        scene_terms = self._extract_terms(scene)
        id_terms = self._extract_terms(document_id)
        all_metadata_terms: Set[str] = set()
        generic_metadata_terms: Set[str] = set()
        for field_name, field_terms in metadata_field_terms.items():
            all_metadata_terms.update(field_terms)
            if field_name in METADATA_FIELD_WEIGHTS:
                continue
            generic_metadata_terms.update(field_terms)

        return {
            "text": text.lower(),
            "scene_text": scene.lower(),
            "metadata_text": metadata_text.lower(),
            "id_text": document_id.lower(),
            "text_terms": text_terms,
            "scene_terms": scene_terms,
            "metadata_terms": generic_metadata_terms,
            "metadata_field_terms": metadata_field_terms,
            "id_terms": id_terms,
            "all_terms": text_terms | scene_terms | all_metadata_terms | id_terms,
        }

    def _resolve_best_match_field(self, field_breakdown: dict[str, float]) -> Optional[str]:
        ranked_fields = [
            (field, score)
            for field, score in (field_breakdown or {}).items()
            if score > 0
        ]
        if not ranked_fields:
            return None
        ranked_fields.sort(key=lambda item: (-item[1], item[0]))
        return ranked_fields[0][0]

    def _build_best_snippet(self, text: str, raw_query: str, matched_terms: list[str]) -> Optional[str]:
        normalized_text = (text or "").strip()
        if not normalized_text:
            return None

        targets: list[str] = []
        if raw_query and raw_query.strip():
            targets.append(raw_query.strip())

        additional_terms = sorted(
            {term for term in (matched_terms or []) if term and term != raw_query},
            key=len,
            reverse=True,
        )
        targets.extend(additional_terms)

        lowered_text = normalized_text.lower()
        for target in targets:
            lowered_target = target.lower()
            position = lowered_text.find(lowered_target)
            if position >= 0:
                return self._extract_snippet(normalized_text, position, len(target))

        return self._preview_text(normalized_text)

    def _extract_snippet(self, text: str, position: int, match_length: int) -> str:
        start = max(position - SNIPPET_CONTEXT_CHARS, 0)
        end = min(position + match_length + SNIPPET_CONTEXT_CHARS, len(text))
        snippet = text[start:end].strip()
        prefix = "..." if start > 0 else ""
        suffix = "..." if end < len(text) else ""
        return f"{prefix}{snippet}{suffix}"

    def _build_document_frequency(self, search_indexes: dict[str, dict[str, object]]) -> Counter:
        counter: Counter = Counter()
        for search_index in search_indexes.values():
            for term in search_index["all_terms"]:
                counter[term] += 1
        return counter

    def _preview_text(self, text: str) -> str:
        normalized = " ".join(text.split())
        if len(normalized) <= MAX_PREVIEW_LENGTH:
            return normalized
        return normalized[:MAX_PREVIEW_LENGTH] + "..."

    def _build_metadata_index(self, metadata: dict[str, object]) -> tuple[str, dict[str, Set[str]]]:
        field_values: dict[str, list[str]] = {}

        def visit(key: Optional[str], value: object) -> None:
            if value is None:
                return
            if isinstance(value, dict):
                for child_key, child_value in value.items():
                    visit(str(child_key).strip().lower(), child_value)
                return
            if isinstance(value, list):
                for item in value:
                    visit(key, item)
                return

            text = str(value).strip()
            if not text:
                return

            normalized_key = (key or "generic").strip().lower() or "generic"
            field_values.setdefault(normalized_key, []).append(text)

        visit(None, metadata or {})
        field_terms = {
            field_name: self._extract_terms(" ".join(values))
            for field_name, values in field_values.items()
        }
        all_text = " ".join(
            text
            for values in field_values.values()
            for text in values
        )
        return all_text, field_terms

    def _extract_terms(self, text: str) -> Set[str]:
        normalized = (text or "").strip().lower()
        if not normalized:
            return set()

        terms: Set[str] = set()
        for token in ASCII_TOKEN_PATTERN.findall(normalized):
            if token in ASCII_STOPWORDS:
                continue
            terms.add(token)
        for block in CJK_TOKEN_PATTERN.findall(normalized):
            terms.add(block)
            if len(block) <= 2:
                continue
            max_ngram = min(len(block), 4)
            for size in range(2, max_ngram + 1):
                for index in range(len(block) - size + 1):
                    terms.add(block[index : index + size])
        return terms

    def _model_validate(self, model_class, payload):
        if hasattr(model_class, "model_validate"):
            return model_class.model_validate(payload)
        return model_class.parse_obj(payload)

    def _model_dump(self, model):
        if hasattr(model, "model_dump"):
            return model.model_dump(mode="json")
        return model.dict()
