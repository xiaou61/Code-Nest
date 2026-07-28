package com.xiaou.sre.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.domain.SreRcaEvaluationSuite;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteCase;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersion;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersionSnapshot;
import com.xiaou.sre.dto.request.SreRcaEvaluationSuiteCreateCommand;
import com.xiaou.sre.dto.request.SreRcaEvaluationSuiteVersionPublishCommand;
import com.xiaou.sre.mapper.SreRcaEvaluationCaseMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationSuiteCaseMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationSuiteMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationSuiteVersionMapper;
import com.xiaou.sre.service.SreRcaEvaluationFingerprint;
import com.xiaou.sre.service.SreRcaEvaluationSuiteService;
import com.xiaou.sre.service.SreReplayContextPolicy;
import com.xiaou.sre.service.SreValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Transactional publication and integrity verification for versioned suites.
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreRcaEvaluationSuiteServiceImpl implements SreRcaEvaluationSuiteService {

    private static final int MAX_CASES_PER_VERSION = 100;
    private static final int MAX_HISTORY_LIMIT = 50;
    private static final int MAX_CONTEXT_LENGTH = 60_000;
    private static final int MAX_REPORT_LENGTH = 1_000_000;
    private static final Pattern SUITE_KEY_PATTERN = Pattern.compile("[a-z][a-z0-9-]{2,63}");

    private final SreRcaEvaluationSuiteMapper suiteMapper;
    private final SreRcaEvaluationSuiteVersionMapper suiteVersionMapper;
    private final SreRcaEvaluationSuiteCaseMapper suiteCaseMapper;
    private final SreRcaEvaluationCaseMapper caseMapper;

    @Override
    @Transactional
    public SreRcaEvaluationSuite create(SreRcaEvaluationSuiteCreateCommand command) {
        if (command == null) {
            throw new SreValidationException("评测套件不能为空");
        }
        String suiteKey = requiredText(command.suiteKey(), 64, "评测套件 Key 不合法").toLowerCase();
        if (!SUITE_KEY_PATTERN.matcher(suiteKey).matches()) {
            throw new SreValidationException("评测套件 Key 不合法");
        }
        String name = requiredText(command.name(), 128, "评测套件名称不合法");
        String description = optionalText(command.description(), 500, "评测套件说明不合法");
        requirePositive(command.createdBy(), "创建管理员 ID 不合法");

        SreRcaEvaluationSuite existing = suiteMapper.selectBySuiteKey(suiteKey);
        if (existing != null) {
            if (!name.equals(existing.getName())) {
                throw new SreValidationException("评测套件 Key 已存在且名称不一致");
            }
            return existing;
        }

        SreRcaEvaluationSuite suite = new SreRcaEvaluationSuite();
        suite.setSuiteKey(suiteKey);
        suite.setName(name);
        suite.setDescription(description);
        suite.setCreatedBy(command.createdBy());
        suite.setCreatedAt(LocalDateTime.now());
        try {
            if (suiteMapper.insert(suite) != 1 || suite.getId() == null) {
                throw new IllegalStateException("SRE RCA 评测套件创建失败");
            }
            return suite;
        } catch (DuplicateKeyException exception) {
            SreRcaEvaluationSuite raced = suiteMapper.selectBySuiteKey(suiteKey);
            if (raced == null || !name.equals(raced.getName())) {
                throw exception;
            }
            return raced;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SreRcaEvaluationSuite> findSuiteById(Long suiteId) {
        if (suiteId == null || suiteId <= 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(suiteMapper.selectById(suiteId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SreRcaEvaluationSuite> listSuites(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, MAX_HISTORY_LIMIT));
        List<SreRcaEvaluationSuite> suites = suiteMapper.selectRecent(boundedLimit);
        return suites == null ? List.of() : List.copyOf(suites);
    }

    @Override
    @Transactional
    public SreRcaEvaluationSuiteVersionSnapshot publishVersion(
            SreRcaEvaluationSuiteVersionPublishCommand command) {
        validatePublishCommand(command);
        List<Long> caseIds = command.caseIds().stream().sorted().toList();
        SreRcaEvaluationSuite suite = suiteMapper.selectByIdForUpdate(command.suiteId());
        if (suite == null) {
            throw new SreValidationException("评测套件不存在");
        }
        List<SreRcaEvaluationCase> cases = loadAndValidateCases(caseIds);
        List<SreRcaEvaluationSuiteCase> unhashedMembers = buildMembers(null, cases);

        SreRcaEvaluationSuiteVersion version = buildVersion(command, suite.getId(), cases.size());
        version.setManifestSha256(SreRcaEvaluationFingerprint.suiteManifest(
                suite, version, unhashedMembers));
        SreRcaEvaluationSuiteVersion existing = suiteVersionMapper.selectBySuiteIdAndManifestSha256(
                suite.getId(), version.getManifestSha256());
        if (existing != null) {
            return verifiedSnapshot(suite, existing);
        }

        Integer latestVersion = suiteVersionMapper.selectLatestVersionNo(suite.getId());
        version.setVersionNo((latestVersion == null ? 0 : latestVersion) + 1);
        if (suiteVersionMapper.insert(version) != 1 || version.getId() == null) {
            throw new IllegalStateException("SRE RCA 评测套件版本发布失败");
        }
        List<SreRcaEvaluationSuiteCase> members = buildMembers(version.getId(), cases);
        if (suiteCaseMapper.insertBatch(members) != members.size()) {
            throw new IllegalStateException("SRE RCA 评测套件成员写入失败");
        }
        return new SreRcaEvaluationSuiteVersionSnapshot(suite, version, cases);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SreRcaEvaluationSuiteVersionSnapshot> findVersionById(Long suiteVersionId) {
        if (suiteVersionId == null || suiteVersionId <= 0) {
            return Optional.empty();
        }
        SreRcaEvaluationSuiteVersion version = suiteVersionMapper.selectById(suiteVersionId);
        if (version == null) {
            return Optional.empty();
        }
        SreRcaEvaluationSuite suite = suiteMapper.selectById(version.getSuiteId());
        if (suite == null) {
            throw new SreValidationException("RCA 评测套件版本归属校验失败");
        }
        return Optional.of(verifiedSnapshot(suite, version));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SreRcaEvaluationSuiteVersion> listVersions(Long suiteId, int limit) {
        if (suiteId == null || suiteId <= 0 || suiteMapper.selectById(suiteId) == null) {
            return List.of();
        }
        int boundedLimit = Math.max(1, Math.min(limit, MAX_HISTORY_LIMIT));
        List<SreRcaEvaluationSuiteVersion> versions = suiteVersionMapper.selectBySuiteId(
                suiteId, boundedLimit);
        return versions == null ? List.of() : List.copyOf(versions);
    }

    private SreRcaEvaluationSuiteVersionSnapshot verifiedSnapshot(
            SreRcaEvaluationSuite suite,
            SreRcaEvaluationSuiteVersion version) {
        List<SreRcaEvaluationSuiteCase> members = suiteCaseMapper.selectBySuiteVersionId(version.getId());
        if (members == null || members.size() != version.getCaseCount()) {
            throw integrityFailure();
        }
        List<SreRcaEvaluationCase> cases = new ArrayList<>(members.size());
        for (int index = 0; index < members.size(); index++) {
            SreRcaEvaluationSuiteCase member = members.get(index);
            if (member.getCaseOrdinal() == null || member.getCaseOrdinal() != index + 1) {
                throw integrityFailure();
            }
            SreRcaEvaluationCase evaluationCase = caseMapper.selectById(member.getCaseId());
            validateCase(evaluationCase);
            if (!SreRcaEvaluationFingerprint.caseContent(evaluationCase)
                    .equalsIgnoreCase(member.getCaseContentSha256())) {
                throw integrityFailure();
            }
            cases.add(evaluationCase);
        }
        String manifest = SreRcaEvaluationFingerprint.suiteManifest(suite, version, members);
        if (!manifest.equalsIgnoreCase(version.getManifestSha256())) {
            throw integrityFailure();
        }
        return new SreRcaEvaluationSuiteVersionSnapshot(suite, version, cases);
    }

    private void validatePublishCommand(SreRcaEvaluationSuiteVersionPublishCommand command) {
        if (command == null) {
            throw new SreValidationException("评测套件版本不能为空");
        }
        requirePositive(command.suiteId(), "评测套件 ID 不合法");
        requirePositive(command.publishedBy(), "发布管理员 ID 不合法");
        if (command.caseIds().isEmpty() || command.caseIds().size() > MAX_CASES_PER_VERSION) {
            throw new SreValidationException("评测套件用例数量不合法");
        }
        Set<Long> distinct = new HashSet<>();
        for (Long caseId : command.caseIds()) {
            requirePositive(caseId, "评测用例 ID 不合法");
            if (!distinct.add(caseId)) {
                throw new SreValidationException("评测套件不能包含重复用例");
            }
        }
        percentage(command.minimumPassRate(), "最低通过率不合法");
        percentage(command.minimumAverageScore(), "最低平均分不合法");
    }

    private SreRcaEvaluationSuiteVersion buildVersion(
            SreRcaEvaluationSuiteVersionPublishCommand command,
            Long suiteId,
            int caseCount) {
        SreRcaEvaluationSuiteVersion version = new SreRcaEvaluationSuiteVersion();
        version.setSuiteId(suiteId);
        version.setCaseCount(caseCount);
        version.setManifestSchemaId(SreRcaEvaluationFingerprint.MANIFEST_SCHEMA_ID);
        version.setScoringPolicyId(SreRcaEvaluationFingerprint.SCORING_POLICY_ID);
        version.setGateEvaluatorId(SreRcaEvaluationFingerprint.GATE_EVALUATOR_ID);
        version.setMinimumPassRate(percentage(command.minimumPassRate(), "最低通过率不合法"));
        version.setMinimumAverageScore(percentage(
                command.minimumAverageScore(), "最低平均分不合法"));
        version.setRequireAllSafety(command.requireAllSafety());
        version.setRequireNoDegraded(command.requireNoDegraded());
        version.setPublishedBy(command.publishedBy());
        version.setPublishedAt(LocalDateTime.now());
        return version;
    }

    private List<SreRcaEvaluationCase> loadAndValidateCases(List<Long> caseIds) {
        List<SreRcaEvaluationCase> cases = new ArrayList<>(caseIds.size());
        for (Long caseId : caseIds) {
            SreRcaEvaluationCase evaluationCase = caseMapper.selectById(caseId);
            validateCase(evaluationCase);
            cases.add(evaluationCase);
        }
        return cases;
    }

    private void validateCase(SreRcaEvaluationCase evaluationCase) {
        if (evaluationCase == null
                || !StringUtils.hasText(evaluationCase.getContextJson())
                || evaluationCase.getContextJson().length() > MAX_CONTEXT_LENGTH
                || !StringUtils.hasText(evaluationCase.getContextSha256())
                || !DigestUtil.sha256Hex(evaluationCase.getContextJson())
                .equalsIgnoreCase(evaluationCase.getContextSha256())
                || !StringUtils.hasText(evaluationCase.getBaselineReportJson())
                || evaluationCase.getBaselineReportJson().length() > MAX_REPORT_LENGTH) {
            throw new SreValidationException("评测套件用例不存在或完整性校验失败");
        }
        if (SreReplayContextPolicy.containsUnsafeContent(evaluationCase.getContextJson())) {
            throw new SreValidationException("评测套件用例包含未脱敏凭据");
        }
    }

    private List<SreRcaEvaluationSuiteCase> buildMembers(
            Long suiteVersionId,
            List<SreRcaEvaluationCase> cases) {
        List<SreRcaEvaluationSuiteCase> members = new ArrayList<>(cases.size());
        for (int index = 0; index < cases.size(); index++) {
            SreRcaEvaluationCase evaluationCase = cases.get(index);
            SreRcaEvaluationSuiteCase member = new SreRcaEvaluationSuiteCase();
            member.setSuiteVersionId(suiteVersionId);
            member.setCaseId(evaluationCase.getId());
            member.setCaseOrdinal(index + 1);
            member.setCaseContentSha256(SreRcaEvaluationFingerprint.caseContent(evaluationCase));
            members.add(member);
        }
        return members;
    }

    private BigDecimal percentage(BigDecimal value, String message) {
        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new SreValidationException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
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
        String normalized = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
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

    private SreValidationException integrityFailure() {
        return new SreValidationException("RCA 评测套件版本完整性校验失败");
    }
}
