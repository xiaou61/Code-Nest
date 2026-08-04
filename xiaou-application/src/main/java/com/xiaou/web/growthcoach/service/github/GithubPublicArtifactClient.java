package com.xiaou.web.growthcoach.service.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 固定访问 GitHub 官方公开 API 的只读客户端。
 *
 * <p>用户输入永远不会作为请求主机或完整 URL 使用；只会被严格解析为仓库和提交/PR 标识符。</p>
 */
@Component
public class GithubPublicArtifactClient {

    private static final Pattern COMMIT_URL = Pattern.compile(
            "^/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)/commit/([0-9A-Fa-f]{7,64})/?$");
    private static final Pattern PULL_URL = Pattern.compile(
            "^/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)/pull/([1-9][0-9]{0,9})/?$");
    private static final String PROVIDER = "GITHUB";
    private static final String COMMIT = "COMMIT";
    private static final String PULL_REQUEST = "PULL_REQUEST";
    private static final URI API_BASE = URI.create("https://api.github.com");

    private final ObjectMapper objectMapper;
    private final GrowthCoachProperties properties;
    private final HttpClient httpClient;

    public GithubPublicArtifactClient(ObjectMapper objectMapper, GrowthCoachProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public GithubPublicArtifactSnapshot verify(String rawUrl) {
        GithubArtifactAddress address = parseAddress(rawUrl);
        JsonNode payload = loadPayload(address);
        LocalDateTime verifiedAt = LocalDateTime.now();
        if (COMMIT.equals(address.artifactType())) {
            return commitSnapshot(address, payload, verifiedAt);
        }
        return pullRequestSnapshot(address, payload, verifiedAt);
    }

    private GithubArtifactAddress parseAddress(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new BusinessException("请提供公开 GitHub commit 或 PR 链接");
        }
        final URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException exception) {
            throw new BusinessException("公开 GitHub 链接格式不正确");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)
                || !"github.com".equalsIgnoreCase(uri.getHost())) {
            throw new BusinessException("仅支持标准 https GitHub commit 或 PR 链接");
        }

        String path = uri.getRawPath();
        Matcher commitMatcher = COMMIT_URL.matcher(path == null ? "" : path);
        if (commitMatcher.matches()) {
            return address(commitMatcher, COMMIT);
        }
        Matcher pullMatcher = PULL_URL.matcher(path == null ? "" : path);
        if (pullMatcher.matches()) {
            return address(pullMatcher, PULL_REQUEST);
        }
        throw new BusinessException("仅支持 github.com/{owner}/{repo}/commit/{sha} 或 /pull/{number} 链接");
    }

    private GithubArtifactAddress address(Matcher matcher, String artifactType) {
        return new GithubArtifactAddress(
                matcher.group(1),
                matcher.group(2),
                matcher.group(3),
                artifactType
        );
    }

    private JsonNode loadPayload(GithubArtifactAddress address) {
        String endpoint = API_BASE + "/repos/" + address.owner() + "/" + address.repository()
                + (COMMIT.equals(address.artifactType())
                ? "/commits/" + address.identifier()
                : "/pulls/" + address.identifier());
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofMillis(readTimeoutMillis()))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "code-nest-growth-coach")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 404) {
                throw new BusinessException("未找到该公开 GitHub 提交或 PR");
            }
            if (status == 403 || status == 429) {
                throw new BusinessException("GitHub 公开接口暂时不可用，请稍后重试");
            }
            if (status < 200 || status >= 300) {
                throw new BusinessException("GitHub 公开接口返回异常，请稍后重试");
            }
            return objectMapper.readTree(response.body());
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException("GitHub 公开接口暂时不可用，请稍后重试");
        }
    }

    private GithubPublicArtifactSnapshot commitSnapshot(
            GithubArtifactAddress address,
            JsonNode payload,
            LocalDateTime verifiedAt
    ) {
        String sha = text(payload, "sha", address.identifier());
        return new GithubPublicArtifactSnapshot(
                PROVIDER,
                COMMIT,
                address.repositoryFullName(),
                sha,
                githubUserId(payload.at("/author/id")),
                githubUserId(payload.at("/committer/id")),
                canonicalUrl(address.owner(), address.repository(), COMMIT, sha),
                firstTimestamp(payload.at("/commit/author/date"), payload.at("/commit/committer/date"), verifiedAt),
                intValue(payload, "files"),
                nullableInt(payload, "additions"),
                nullableInt(payload, "deletions"),
                null,
                false,
                verifiedAt
        );
    }

    private GithubPublicArtifactSnapshot pullRequestSnapshot(
            GithubArtifactAddress address,
            JsonNode payload,
            LocalDateTime verifiedAt
    ) {
        String number = text(payload, "number", address.identifier());
        return new GithubPublicArtifactSnapshot(
                PROVIDER,
                PULL_REQUEST,
                address.repositoryFullName(),
                number,
                githubUserId(payload.at("/user/id")),
                null,
                canonicalUrl(address.owner(), address.repository(), PULL_REQUEST, number),
                firstTimestamp(payload.path("updated_at"), payload.path("created_at"), verifiedAt),
                nullableInt(payload, "changed_files"),
                nullableInt(payload, "additions"),
                nullableInt(payload, "deletions"),
                text(payload, "state", null),
                payload.path("merged").asBoolean(false),
                verifiedAt
        );
    }

    private String canonicalUrl(String owner, String repository, String artifactType, String identifier) {
        String segment = COMMIT.equals(artifactType) ? "commit" : "pull";
        return "https://github.com/" + owner + "/" + repository + "/" + segment + "/" + identifier;
    }

    private LocalDateTime firstTimestamp(JsonNode first, JsonNode second, LocalDateTime fallback) {
        LocalDateTime resolved = timestamp(first);
        return resolved == null ? firstPresent(timestamp(second), fallback) : resolved;
    }

    private LocalDateTime firstPresent(LocalDateTime value, LocalDateTime fallback) {
        return value == null ? fallback : value;
    }

    private LocalDateTime timestamp(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull() || !StringUtils.hasText(value.asText())) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(value.asText()), ZoneId.systemDefault());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private Integer nullableInt(JsonNode payload, String field) {
        JsonNode value = payload.path(field);
        return value.isMissingNode() || value.isNull() ? null : Math.max(0, value.asInt());
    }

    private Integer intValue(JsonNode payload, String field) {
        JsonNode value = payload.path(field);
        if (value.isArray()) {
            return value.size();
        }
        return nullableInt(payload, field);
    }

    private String text(JsonNode payload, String field, String fallback) {
        JsonNode value = payload.path(field);
        return value.isMissingNode() || value.isNull() || !StringUtils.hasText(value.asText())
                ? fallback
                : value.asText().trim();
    }

    private Long githubUserId(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull() || !value.canConvertToLong()) {
            return null;
        }
        long id = value.asLong(0L);
        return id > 0 ? id : null;
    }

    private int connectTimeoutMillis() {
        GrowthCoachProperties.CodeArtifact config = properties.getCodeArtifact();
        return config == null ? 3000 : Math.max(500, Math.min(config.getConnectTimeoutMillis(), 15000));
    }

    private int readTimeoutMillis() {
        GrowthCoachProperties.CodeArtifact config = properties.getCodeArtifact();
        return config == null ? 8000 : Math.max(1000, Math.min(config.getReadTimeoutMillis(), 30000));
    }

    private record GithubArtifactAddress(String owner, String repository, String identifier, String artifactType) {
        private String repositoryFullName() {
            return owner + "/" + repository;
        }
    }

    public record GithubPublicArtifactSnapshot(
            String provider,
            String artifactType,
            String repository,
            String externalId,
            Long authorGithubUserId,
            Long committerGithubUserId,
            String canonicalUrl,
            LocalDateTime sourceObservedAt,
            Integer changedFiles,
            Integer additions,
            Integer deletions,
            String artifactState,
            boolean merged,
            LocalDateTime verifiedAt
    ) {
    }
}
