from __future__ import annotations

from typing import Optional

from fastapi import Depends
from fastapi import FastAPI
from fastapi import HTTPException
from fastapi import Query

from app.auth import authorization_header
from app.auth import verify_api_key
from app.config import ServiceSettings
from app.models import DocumentBatchDeleteRequest
from app.models import DocumentBatchDeleteResponse
from app.models import DocumentDeleteResponse
from app.models import DocumentExportResponse
from app.models import DocumentImportRequest
from app.models import DocumentImportResponse
from app.models import DocumentListResponse
from app.models import HealthResponse
from app.models import RetrieveRequest
from app.models import RetrieveResponse
from app.service import DocumentStore


def create_app(settings: Optional[ServiceSettings] = None) -> FastAPI:
    resolved_settings = settings or ServiceSettings.from_env()
    document_store = DocumentStore(resolved_settings)

    app = FastAPI(title="Code-Nest LlamaIndex Service", version="0.2.0")

    def require_api_key(authorization: Optional[str] = Depends(authorization_header)) -> None:
        verify_api_key(authorization, resolved_settings)

    @app.get("/health", response_model=HealthResponse)
    def health() -> HealthResponse:
        return HealthResponse(
            status="ok",
            authEnabled=resolved_settings.auth_enabled,
            documentCount=document_store.document_count(),
            sceneCount=document_store.scene_count(),
            dataFile=str(resolved_settings.data_file),
        )

    @app.post(
        "/api/v1/retrieve",
        response_model=RetrieveResponse,
        dependencies=[Depends(require_api_key)],
    )
    def retrieve(request: RetrieveRequest) -> RetrieveResponse:
        return document_store.retrieve(request)

    @app.post(
        "/api/v1/admin/documents/import",
        response_model=DocumentImportResponse,
        dependencies=[Depends(require_api_key)],
    )
    def import_documents(request: DocumentImportRequest) -> DocumentImportResponse:
        imported_count, total_count = document_store.import_documents(
            request.documents,
            replace=request.replace,
        )
        return DocumentImportResponse(importedCount=imported_count, totalCount=total_count)

    @app.get(
        "/api/v1/admin/documents",
        response_model=DocumentListResponse,
        dependencies=[Depends(require_api_key)],
    )
    def list_documents(
        scene: Optional[str] = Query(default=None),
        limit: int = Query(default=100, ge=1, le=500),
    ) -> DocumentListResponse:
        documents = document_store.list_documents(scene=scene, limit=limit)
        return DocumentListResponse(totalCount=len(documents), documents=documents)

    @app.get(
        "/api/v1/admin/documents/export",
        response_model=DocumentExportResponse,
        dependencies=[Depends(require_api_key)],
    )
    def export_documents(
        scene: Optional[str] = Query(default=None),
    ) -> DocumentExportResponse:
        documents = document_store.export_documents(scene=scene)
        return DocumentExportResponse(totalCount=len(documents), documents=documents)

    @app.delete(
        "/api/v1/admin/documents",
        response_model=DocumentDeleteResponse,
        dependencies=[Depends(require_api_key)],
    )
    def delete_document(
        document_id: str = Query(..., alias="documentId"),
    ) -> DocumentDeleteResponse:
        try:
            deleted_document_id, deleted_count, total_count = document_store.delete_document(document_id)
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
        except KeyError as exc:
            detail = exc.args[0] if exc.args else str(exc)
            raise HTTPException(status_code=404, detail=detail) from exc
        return DocumentDeleteResponse(
            documentId=deleted_document_id,
            deletedCount=deleted_count,
            totalCount=total_count,
        )

    @app.post(
        "/api/v1/admin/documents/batch-delete",
        response_model=DocumentBatchDeleteResponse,
        dependencies=[Depends(require_api_key)],
    )
    def batch_delete_documents(request: DocumentBatchDeleteRequest) -> DocumentBatchDeleteResponse:
        try:
            deleted_document_ids, missing_document_ids, total_count = document_store.delete_documents(request.documentIds)
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
        return DocumentBatchDeleteResponse(
            requestedCount=len(deleted_document_ids) + len(missing_document_ids),
            deletedCount=len(deleted_document_ids),
            deletedDocumentIds=deleted_document_ids,
            missingCount=len(missing_document_ids),
            missingDocumentIds=missing_document_ids,
            totalCount=total_count,
        )

    return app


app = create_app()
