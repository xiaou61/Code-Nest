package com.xiaou.web.growthcoach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.mockinterview.dto.response.JobBattleMatchEngineResult;
import com.xiaou.web.growthcoach.dto.GrowthJobMarketSignalResponse;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort.JobBattleMatchData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 将最近一次用户自录 JD 批量分析收敛为样本信号。
 *
 * <p>这里只统计同一次分析中的非降级 JD，不抓取外部招聘平台，也不把样本结论描述为市场趋势。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthJobMarketSignalService {

    private static final int MINIMUM_SAMPLE_COUNT = 3;
    private static final int MAX_SKILLS = 5;
    private static final int MAX_LABELS = 5;
    private static final int MAX_TEXT_LENGTH = 80;
    private static final String MATCH_ENGINE_ROUTE = "/job-match-engine";

    private final GrowthCareerDataPort careerDataPort;
    private final ObjectMapper objectMapper;

    public GrowthJobMarketSignalResponse getCurrentSignal(Long userId) {
        GrowthJobMarketSignalResponse response = new GrowthJobMarketSignalResponse();
        if (userId == null || userId <= 0) {
            response.setInsufficientReason("尚未获得可用的 JD 样本。");
            return response;
        }

        JobBattleMatchData record = careerDataPort.latestJobBattleMatch(userId);
        if (record == null) {
            response.setInsufficientReason("先录入至少 3 条真实 JD，才能形成岗位样本信号。");
            return response;
        }

        response.setSourceAnalysisId(record.id());
        response.setSourceObservedAt(record.createdAt());
        JobBattleMatchEngineResult result = readResult(record);
        List<JobBattleMatchEngineResult.TargetScore> samples = nonFallbackSamples(result);
        response.setSampleCount(samples.size());
        if (samples.size() < MINIMUM_SAMPLE_COUNT) {
            response.setInsufficientReason("最近一次分析只有 " + samples.size()
                    + " 条非降级 JD，至少需要 " + MINIMUM_SAMPLE_COUNT + " 条才会给出样本信号。");
            response.setSummary("当前样本不足，不把单条或少量 JD 当作岗位市场结论。");
            return response;
        }

        List<GrowthJobMarketSignalResponse.SkillFrequency> skills = countRequiredSkills(samples);
        response.setTopRequiredSkills(skills);
        response.setTargetRoles(labels(samples, JobBattleMatchEngineResult.TargetScore::getTargetRole));
        response.setCities(labels(samples, JobBattleMatchEngineResult.TargetScore::getCity));
        response.setSampleReady(true);
        response.setSummary(buildSummary(samples.size(), skills));
        response.setNextAction(buildNextAction(samples.size(), skills));
        return response;
    }

    private JobBattleMatchEngineResult readResult(JobBattleMatchData record) {
        if (record == null || !StringUtils.hasText(record.resultJson())) {
            return null;
        }
        try {
            return objectMapper.readValue(record.resultJson(), JobBattleMatchEngineResult.class);
        } catch (JsonProcessingException exception) {
            log.warn("解析岗位样本信号失败，recordId={}", record.id());
            return null;
        }
    }

    private List<JobBattleMatchEngineResult.TargetScore> nonFallbackSamples(JobBattleMatchEngineResult result) {
        if (result == null || result.getRanking() == null) {
            return List.of();
        }
        return result.getRanking().stream()
                .filter(item -> item != null && !Boolean.TRUE.equals(item.getFallback()))
                .toList();
    }

    private List<GrowthJobMarketSignalResponse.SkillFrequency> countRequiredSkills(
            List<JobBattleMatchEngineResult.TargetScore> samples
    ) {
        Map<String, SkillCounter> counters = new LinkedHashMap<>();
        for (JobBattleMatchEngineResult.TargetScore sample : samples) {
            LinkedHashSet<String> skillsInOneJd = new LinkedHashSet<>();
            for (String rawSkill : safeList(sample.getRequiredSkills())) {
                String skill = normalizeText(rawSkill);
                if (StringUtils.hasText(skill)) {
                    skillsInOneJd.add(skill);
                }
            }
            for (String skill : skillsInOneJd) {
                String key = skill.toLowerCase(Locale.ROOT);
                SkillCounter counter = counters.computeIfAbsent(key, ignored -> new SkillCounter(skill));
                counter.increment();
            }
        }

        int sampleCount = Math.max(samples.size(), 1);
        return counters.values().stream()
                .sorted(Comparator.comparingInt(SkillCounter::count).reversed()
                        .thenComparing(SkillCounter::skill, String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_SKILLS)
                .map(counter -> frequency(counter, sampleCount))
                .toList();
    }

    private GrowthJobMarketSignalResponse.SkillFrequency frequency(SkillCounter counter, int sampleCount) {
        GrowthJobMarketSignalResponse.SkillFrequency result = new GrowthJobMarketSignalResponse.SkillFrequency();
        result.setSkill(counter.skill());
        result.setOccurrenceCount(counter.count());
        result.setCoveragePercent((int) Math.round(counter.count() * 100D / sampleCount));
        return result;
    }

    private List<String> labels(
            List<JobBattleMatchEngineResult.TargetScore> samples,
            java.util.function.Function<JobBattleMatchEngineResult.TargetScore, String> extractor
    ) {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (JobBattleMatchEngineResult.TargetScore sample : samples) {
            String label = normalizeText(extractor.apply(sample));
            if (StringUtils.hasText(label)) {
                labels.add(label);
            }
            if (labels.size() >= MAX_LABELS) {
                break;
            }
        }
        return new ArrayList<>(labels);
    }

    private String buildSummary(int sampleCount, List<GrowthJobMarketSignalResponse.SkillFrequency> skills) {
        if (skills.isEmpty()) {
            return "基于你录入的 " + sampleCount + " 条非降级 JD 样本，暂未抽取到稳定的必备技能标签。";
        }
        GrowthJobMarketSignalResponse.SkillFrequency first = skills.get(0);
        return "基于你录入的 " + sampleCount + " 条非降级 JD 样本，"
                + first.getSkill() + " 在 " + first.getOccurrenceCount() + " 条中被列为必备技能。";
    }

    private GrowthJobMarketSignalResponse.NextAction buildNextAction(
            int sampleCount,
            List<GrowthJobMarketSignalResponse.SkillFrequency> skills
    ) {
        GrowthJobMarketSignalResponse.NextAction action = new GrowthJobMarketSignalResponse.NextAction();
        String skillSummary = skills.stream()
                .map(GrowthJobMarketSignalResponse.SkillFrequency::getSkill)
                .limit(3)
                .reduce((left, right) -> left + "、" + right)
                .orElse("当前岗位样本中的必备技能");
        action.setTitle("按岗位样本收敛本周重点");
        action.setDescription("先围绕 " + skillSummary + " 检查本周已有任务与岗位差距，再决定保留或调整。 ");
        action.setExpectedChange("计划预览会在时间预算和真实资源约束下，优先排序已有可执行任务。");
        action.setRoutePath(MATCH_ENGINE_ROUTE);
        action.setPrefillMessage("我最近录入了 " + sampleCount + " 条非降级岗位 JD，"
                + "其中常见必备技能是 " + skillSummary + "。请在不超过本周可投入时间、"
                + "且只使用已有真实资源的前提下，优先保留相关任务并生成调整预览。");
        return action;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= MAX_TEXT_LENGTH ? normalized : normalized.substring(0, MAX_TEXT_LENGTH);
    }

    private static final class SkillCounter {
        private final String skill;
        private int count;

        private SkillCounter(String skill) {
            this.skill = skill;
        }

        private String skill() {
            return skill;
        }

        private int count() {
            return count;
        }

        private void increment() {
            count++;
        }
    }
}
