package com.xiaou.ai.rag;

import com.xiaou.common.config.AiProperties;
import com.xiaou.common.exception.ai.AiRetrievalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

/**
 * LlamaIndex 检索网关客户端。
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlamaIndexClient {

    private static final String HEALTH_PATH = "/health";
    private static final String RETRIEVE_PATH = "/api/v1/retrieve";
    private static final String DOCUMENTS_PATH = "/api/v1/admin/documents";
    private static final String BATCH_DELETE_DOCUMENTS_PATH = "/api/v1/admin/documents/batch-delete";
    private static final String EXPORT_DOCUMENTS_PATH = "/api/v1/admin/documents/export";
    private static final String IMPORT_DOCUMENTS_PATH = "/api/v1/admin/documents/import";

    private final AiProperties aiProperties;
    private final RestClient.Builder restClientBuilder;

    private volatile RestClient restClient;

    public boolean hasEndpointConfigured() {
        return aiProperties.getRag() != null && StringUtils.hasText(aiProperties.getRag().getEndpoint());
    }

    public boolean isAvailable() {
        return aiProperties.getRag() != null
                && aiProperties.getRag().isEnabled()
                && hasEndpointConfigured();
    }

    public LlamaIndexHealthResponse health() {
        ensureEndpointConfigured();
        try {
            LlamaIndexHealthResponse response = getRestClient()
                    .get()
                    .uri(HEALTH_PATH)
                    .retrieve()
                    .body(LlamaIndexHealthResponse.class);
            if (response == null) {
                throw new AiRetrievalException("LlamaIndex 健康检查返回为空");
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new AiRetrievalException("LlamaIndex 健康检查失败，HTTP 状态码: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new AiRetrievalException("调用 LlamaIndex 健康检查失败", e);
        }
    }

    public LlamaIndexDocumentListResponse listDocuments(String scene, Integer limit) {
        ensureEndpointConfigured();
        try {
            LlamaIndexDocumentListResponse response = getRestClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path(DOCUMENTS_PATH)
                            .queryParamIfPresent("scene", optionalText(scene))
                            .queryParamIfPresent("limit", Optional.ofNullable(limit))
                            .build())
                    .retrieve()
                    .body(LlamaIndexDocumentListResponse.class);
            if (response == null) {
                throw new AiRetrievalException("LlamaIndex 文档列表返回为空");
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new AiRetrievalException("LlamaIndex 文档列表查询失败，HTTP 状态码: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new AiRetrievalException("调用 LlamaIndex 文档列表失败", e);
        }
    }

    public LlamaIndexDocumentExportResponse exportDocuments(String scene) {
        ensureEndpointConfigured();
        try {
            LlamaIndexDocumentExportResponse response = getRestClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path(EXPORT_DOCUMENTS_PATH)
                            .queryParamIfPresent("scene", optionalText(scene))
                            .build())
                    .retrieve()
                    .body(LlamaIndexDocumentExportResponse.class);
            if (response == null) {
                throw new AiRetrievalException("LlamaIndex 文档导出返回为空");
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new AiRetrievalException("LlamaIndex 文档导出失败，HTTP 状态码: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new AiRetrievalException("调用 LlamaIndex 文档导出失败", e);
        }
    }

    public LlamaIndexDocumentImportResponse importDocuments(LlamaIndexDocumentImportRequest request) {
        ensureEndpointConfigured();
        if (request == null || request.getDocuments() == null || request.getDocuments().isEmpty()) {
            throw new AiRetrievalException("LlamaIndex 文档导入请求不能为空，且 documents 至少包含一条记录");
        }

        try {
            LlamaIndexDocumentImportResponse response = getRestClient()
                    .post()
                    .uri(IMPORT_DOCUMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(LlamaIndexDocumentImportResponse.class);
            if (response == null) {
                throw new AiRetrievalException("LlamaIndex 文档导入返回为空");
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new AiRetrievalException("LlamaIndex 文档导入失败，HTTP 状态码: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new AiRetrievalException("调用 LlamaIndex 文档导入失败", e);
        }
    }

    public LlamaIndexDocumentDeleteResponse deleteDocument(String documentId) {
        ensureEndpointConfigured();
        if (!StringUtils.hasText(documentId)) {
            throw new AiRetrievalException("LlamaIndex 文档删除请求不能为空，且 documentId 必须有值");
        }

        try {
            LlamaIndexDocumentDeleteResponse response = getRestClient()
                    .delete()
                    .uri(uriBuilder -> uriBuilder.path(DOCUMENTS_PATH)
                            .queryParam("documentId", documentId.trim())
                            .build())
                    .retrieve()
                    .body(LlamaIndexDocumentDeleteResponse.class);
            if (response == null) {
                throw new AiRetrievalException("LlamaIndex 文档删除返回为空");
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new AiRetrievalException("LlamaIndex 文档删除失败，HTTP 状态码: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new AiRetrievalException("调用 LlamaIndex 文档删除失败", e);
        }
    }

    public LlamaIndexDocumentBatchDeleteResponse deleteDocuments(LlamaIndexDocumentBatchDeleteRequest request) {
        ensureEndpointConfigured();
        if (request == null || request.getDocumentIds() == null || request.getDocumentIds().isEmpty()) {
            throw new AiRetrievalException("LlamaIndex 批量删除请求不能为空，且 documentIds 至少包含一条记录");
        }

        try {
            LlamaIndexDocumentBatchDeleteResponse response = getRestClient()
                    .post()
                    .uri(BATCH_DELETE_DOCUMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(LlamaIndexDocumentBatchDeleteResponse.class);
            if (response == null) {
                throw new AiRetrievalException("LlamaIndex 批量删除返回为空");
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new AiRetrievalException("LlamaIndex 批量删除失败，HTTP 状态码: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new AiRetrievalException("调用 LlamaIndex 批量删除失败", e);
        }
    }

    public LlamaIndexRetrieveResponse retrieve(LlamaIndexRetrieveRequest request) {
        if (!isAvailable()) {
            throw new AiRetrievalException("LlamaIndex 检索服务未启用或缺少 endpoint 配置");
        }
        if (request == null || !StringUtils.hasText(request.getQuery())) {
            throw new AiRetrievalException("LlamaIndex 检索请求不能为空，且 query 必须有值");
        }

        try {
            LlamaIndexRetrieveResponse response = getRestClient()
                    .post()
                    .uri(RETRIEVE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(prepareRequest(request))
                    .retrieve()
                    .body(LlamaIndexRetrieveResponse.class);

            if (response == null) {
                throw new AiRetrievalException("LlamaIndex 检索返回为空");
            }
            if (!StringUtils.hasText(response.getQuery())) {
                response.setQuery(request.getQuery());
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new AiRetrievalException("LlamaIndex 检索失败，HTTP 状态码: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new AiRetrievalException("调用 LlamaIndex 检索服务失败", e);
        }
    }

    private LlamaIndexRetrieveRequest prepareRequest(LlamaIndexRetrieveRequest request) {
        if (request.getTopK() == null || request.getTopK() <= 0) {
            request.setTopK(aiProperties.getRag().getDefaultTopK());
        }
        return request;
    }

    private void ensureEndpointConfigured() {
        if (!hasEndpointConfigured()) {
            throw new AiRetrievalException("LlamaIndex 服务缺少 endpoint 配置");
        }
    }

    private Optional<String> optionalText(String value) {
        return StringUtils.hasText(value) ? Optional.of(value.trim()) : Optional.empty();
    }

    private RestClient getRestClient() {
        RestClient localRef = restClient;
        if (localRef != null) {
            return localRef;
        }

        synchronized (this) {
            if (restClient == null) {
                RestClient.Builder builder = restClientBuilder.clone()
                        .baseUrl(aiProperties.getRag().getEndpoint())
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

                if (StringUtils.hasText(aiProperties.getRag().getApiKey())) {
                    builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getRag().getApiKey());
                }
                restClient = builder.build();
            }
            return restClient;
        }
    }
}
