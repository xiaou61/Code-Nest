package com.xiaou.sre.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.xiaou.sre.domain.SreInvestigationArtifact;
import com.xiaou.sre.dto.request.SreInvestigationArtifactCapture;
import com.xiaou.sre.mapper.SreInvestigationArtifactMapper;
import com.xiaou.sre.mapper.SreInvestigationRunMapper;
import com.xiaou.sre.service.SreInvestigationArtifactService;
import com.xiaou.sre.service.SreValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SRE 调查回放产物实现。一条 run 只允许由数据库唯一键保存一份不可变输入。
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreInvestigationArtifactServiceImpl implements SreInvestigationArtifactService {

    private static final int MAX_CONTEXT_LENGTH = 60_000;
    private static final int MAX_PROMPT_ID_LENGTH = 128;
    private static final int MAX_SCHEMA_ID_LENGTH = 255;
    private static final int MAX_PROVIDER_LENGTH = 64;
    private static final int MAX_MODEL_LENGTH = 128;
    private static final Set<String> INVOCATION_OUTCOMES = Set.of(
            "SUCCESS", "MODEL_UNAVAILABLE", "EMPTY_RESPONSE", "INVOCATION_EXCEPTION", "PARSER_FAILURE");
    private static final Pattern CONTROL_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");
    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)\\bBearer\\s+(?!\\[REDACTED])[-A-Za-z0-9._~+/=]{6,}");
    private static final Pattern STANDALONE_CREDENTIAL_PATTERN = Pattern.compile(
            "(?i)\\b(?:sk-[A-Za-z0-9_-]{16,}|gh[pousr]_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16})\\b");
    private static final Pattern JSON_SECRET_VALUE_PATTERN = Pattern.compile(
            "(?i)\\\"[A-Za-z0-9_.-]*(?:authorization|password|passwd|token|secret|apikey|cookie|credential|privatekey)\\\""
                    + "\\s*:\\s*\\\"(?!\\[REDACTED])[^\\\"]{4,}\\\"");

    private final SreInvestigationRunMapper runMapper;
    private final SreInvestigationArtifactMapper artifactMapper;

    @Override
    @Transactional
    public Optional<SreInvestigationArtifact> capture(SreInvestigationArtifactCapture capture) {
        if (capture == null) {
            throw new SreValidationException("调查回放产物不能为空");
        }
        requirePositive(capture.incidentId(), "事故 ID 不合法");
        requirePositive(capture.runId(), "调查运行 ID 不合法");
        if (runMapper.selectByIncidentIdAndId(capture.incidentId(), capture.runId()) == null) {
            return Optional.empty();
        }
        validateContext(capture.contextJson());

        SreInvestigationArtifact artifact = new SreInvestigationArtifact();
        artifact.setIncidentId(capture.incidentId());
        artifact.setRunId(capture.runId());
        artifact.setContextJson(capture.contextJson());
        artifact.setContextSha256(DigestUtil.sha256Hex(capture.contextJson()));
        artifact.setContextLength(capture.contextJson().length());
        artifact.setContextTruncated(capture.contextTruncated());
        artifact.setPromptId(requiredText(capture.promptId(), MAX_PROMPT_ID_LENGTH, "Prompt ID 不合法"));
        artifact.setSchemaId(requiredText(capture.schemaId(), MAX_SCHEMA_ID_LENGTH, "结构化契约 ID 不合法"));
        artifact.setProvider(requiredText(capture.provider(), MAX_PROVIDER_LENGTH, "AI provider 不合法"));
        artifact.setConfiguredModel(optionalText(
                capture.configuredModel(), MAX_MODEL_LENGTH, "配置模型名称不合法"));
        artifact.setActualModel(optionalText(capture.actualModel(), MAX_MODEL_LENGTH, "实际模型名称不合法"));
        artifact.setInvocationOutcome(normalizeOutcome(capture.invocationOutcome()));
        artifact.setCreatedAt(LocalDateTime.now());
        if (artifactMapper.insert(artifact) != 1 || artifact.getId() == null) {
            throw new IllegalStateException("SRE 调查回放产物写入失败");
        }
        return Optional.of(artifact);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SreInvestigationArtifact> findByIncidentIdAndRunId(Long incidentId, Long runId) {
        if (incidentId == null || incidentId <= 0 || runId == null || runId <= 0) {
            return Optional.empty();
        }
        if (runMapper.selectByIncidentIdAndId(incidentId, runId) == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(artifactMapper.selectByIncidentIdAndRunId(incidentId, runId));
    }

    private void validateContext(String contextJson) {
        if (!StringUtils.hasText(contextJson) || contextJson.length() > MAX_CONTEXT_LENGTH) {
            throw new SreValidationException("调查回放上下文大小不合法");
        }
        if (CONTROL_PATTERN.matcher(contextJson).find()
                || BEARER_PATTERN.matcher(contextJson).find()
                || STANDALONE_CREDENTIAL_PATTERN.matcher(contextJson).find()
                || JSON_SECRET_VALUE_PATTERN.matcher(contextJson).find()) {
            throw new SreValidationException("调查回放上下文包含未脱敏凭据");
        }
    }

    private String normalizeOutcome(String value) {
        String normalized = requiredText(value, 32, "模型调用结果不合法").toUpperCase(Locale.ROOT);
        if (!INVOCATION_OUTCOMES.contains(normalized)) {
            throw new SreValidationException("模型调用结果不合法");
        }
        return normalized;
    }

    private String requiredText(String value, int maxLength, String message) {
        String normalized = optionalText(value, maxLength, message);
        if (!StringUtils.hasText(normalized)) {
            throw new SreValidationException(message);
        }
        return normalized;
    }

    private String optionalText(String value, int maxLength, String message) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = CONTROL_PATTERN.matcher(value).replaceAll("").trim();
        if (!StringUtils.hasText(normalized) || normalized.length() > maxLength) {
            throw new SreValidationException(message);
        }
        return normalized;
    }

    private void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new SreValidationException(message);
        }
    }
}
