from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field


class RetrieveRequest(BaseModel):
    query: str
    scene: Optional[str] = None
    topK: int = 5
    metadataFilters: dict[str, object] = Field(default_factory=dict)


class RetrieveNode(BaseModel):
    id: str
    score: float
    text: str
    metadata: dict[str, object] = Field(default_factory=dict)
    matchedTerms: list[str] = Field(default_factory=list)
    scoreBreakdown: dict[str, float] = Field(default_factory=dict)
    bestMatchField: Optional[str] = None
    bestSnippet: Optional[str] = None


class RetrieveResponse(BaseModel):
    query: str
    nodes: list[RetrieveNode] = Field(default_factory=list)
    fallback: bool = False


class KnowledgeDocument(BaseModel):
    id: str
    text: str
    scene: Optional[str] = None
    metadata: dict[str, object] = Field(default_factory=dict)


class DocumentImportRequest(BaseModel):
    documents: list[KnowledgeDocument] = Field(default_factory=list)
    replace: bool = False


class DocumentImportResponse(BaseModel):
    importedCount: int
    totalCount: int


class DocumentListItem(BaseModel):
    id: str
    scene: Optional[str] = None
    textPreview: str
    metadata: dict[str, object] = Field(default_factory=dict)


class DocumentListResponse(BaseModel):
    totalCount: int
    documents: list[DocumentListItem] = Field(default_factory=list)


class DocumentExportResponse(BaseModel):
    totalCount: int
    documents: list[KnowledgeDocument] = Field(default_factory=list)


class DocumentDeleteResponse(BaseModel):
    documentId: str
    deletedCount: int
    totalCount: int


class DocumentBatchDeleteRequest(BaseModel):
    documentIds: list[str] = Field(default_factory=list)


class DocumentBatchDeleteResponse(BaseModel):
    requestedCount: int
    deletedCount: int
    deletedDocumentIds: list[str] = Field(default_factory=list)
    missingCount: int
    missingDocumentIds: list[str] = Field(default_factory=list)
    totalCount: int


class HealthResponse(BaseModel):
    status: str
    authEnabled: bool
    documentCount: int
    sceneCount: int
    dataFile: str
