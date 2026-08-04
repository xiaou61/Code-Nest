package com.xiaou.web.growthcoach.service;

import com.xiaou.mockinterview.domain.JobBattleMatchRecord;
import com.xiaou.mockinterview.domain.JobBattlePlanRecord;
import com.xiaou.mockinterview.domain.MockInterviewDirection;
import com.xiaou.mockinterview.domain.MockInterviewSession;
import com.xiaou.mockinterview.mapper.JobBattleMatchRecordMapper;
import com.xiaou.mockinterview.mapper.JobBattlePlanRecordMapper;
import com.xiaou.mockinterview.mapper.MockInterviewSessionMapper;
import com.xiaou.mockinterview.service.MockInterviewService;
import com.xiaou.web.growthcoach.dto.GrowthApplicationOutcomeResponse;
import com.xiaou.web.growthcoach.dto.GrowthJobBattleGapResponse;
import com.xiaou.web.growthcoach.dto.GrowthJobPreparationLoopResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 组织用户已拥有的岗位准备事实，确定性选择当前应推进的一个阶段。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthJobPreparationLoopService {

    private static final int MAX_FOCUS_SKILLS = 3;
    private static final int MAX_FOCUS_SKILL_LENGTH = 32;
    private static final int RECENT_COMPLETED_INTERVIEW_LIMIT = 8;
    private static final String MATCH_ROUTE = "/job-match-engine";
    private static final String PLAN_ROUTE = "/job-battle?step=2";
    private static final String MOCK_INTERVIEW_ROUTE = "/mock-interview";
    private static final String APPLICATION_ROUTE = "/career-loop?focus=applications";

    private final JobBattleMatchRecordMapper matchRecordMapper;
    private final JobBattlePlanRecordMapper planRecordMapper;
    private final MockInterviewSessionMapper mockInterviewSessionMapper;
    private final MockInterviewService mockInterviewService;
    private final GrowthJobBattleGapService growthJobBattleGapService;
    private final GrowthApplicationOutcomeService growthApplicationOutcomeService;

    public GrowthJobPreparationLoopResponse getForUser(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        JobBattleMatchRecord matchRecord = matchRecordMapper.selectLatestByUserId(userId);
        if (matchRecord == null) {
            return null;
        }

        GrowthJobBattleGapResponse gap = safely("job_battle_gap",
                () -> growthJobBattleGapService.getCurrentGap(userId));
        JobBattlePlanRecord planRecord = planRecordMapper.selectLatestByUserId(userId);
        GrowthApplicationOutcomeResponse applications = safely("application_outcomes",
                () -> growthApplicationOutcomeService.getForUser(userId));

        GrowthJobPreparationLoopResponse response = new GrowthJobPreparationLoopResponse();
        response.setMatchRecordId(matchRecord.getId());
        response.setPlanRecordId(planRecord == null ? null : planRecord.getId());
        response.setTargetRole(firstText(
                gap == null ? null : gap.getTargetRole(),
                matchRecord.getBestTargetRole(),
                "当前目标岗位"
        ));
        response.setFocusSkills(focusSkills(gap));

        if (Boolean.TRUE.equals(gap == null ? null : gap.getFallback())) {
            fillFallbackMatchStage(response, matchRecord, planRecord, applications);
            return response;
        }

        boolean planCurrent = isCurrentPlan(planRecord, matchRecord);
        MockInterviewSession completedMock = planCurrent
                ? latestCompletedAfter(userId, planRecord == null ? null : planRecord.getCreateTime())
                : null;
        response.setMockInterviewSessionId(completedMock == null ? null : completedMock.getId());

        if (!planCurrent) {
            fillPlanStage(response, matchRecord, planRecord, applications);
            return response;
        }
        if (completedMock == null) {
            fillMockInterviewStage(response, matchRecord, planRecord, applications);
            return response;
        }
        fillApplicationStage(response, matchRecord, planRecord, completedMock, applications);
        return response;
    }

    private void fillFallbackMatchStage(
            GrowthJobPreparationLoopResponse response,
            JobBattleMatchRecord matchRecord,
            JobBattlePlanRecord planRecord,
            GrowthApplicationOutcomeResponse applications
    ) {
        response.setStage("GAP_ANALYSIS");
        response.setSummary("最近一次岗位匹配包含降级结果，先复核岗位差距，再推进后续准备。");
        response.setCurrentAction(action(
                "复核最近一次岗位匹配",
                "当前结果包含降级项，不把它直接作为学习或模拟面试的专项依据。",
                MATCH_ROUTE,
                null,
                "重新保存可靠的匹配结论后，才能形成可追溯的准备闭环。"
        ));
        response.setSteps(List.of(
                step("GAP_ANALYSIS", "岗位差距", "current", "先复核最近一次匹配结果。", matchRecord.getCreateTime()),
                step("STUDY_PLAN", "补短板计划", "pending", "等待可靠的岗位差距结论。", planRecord == null ? null : planRecord.getCreateTime()),
                step("MOCK_INTERVIEW", "模拟面试", "pending", "先形成可靠的准备范围。", null),
                applicationStep("pending", applications)
        ));
        response.setSourceRefs(matchAndPlanSources(matchRecord, planRecord));
    }

    private void fillPlanStage(
            GrowthJobPreparationLoopResponse response,
            JobBattleMatchRecord matchRecord,
            JobBattlePlanRecord planRecord,
            GrowthApplicationOutcomeResponse applications
    ) {
        response.setStage("STUDY_PLAN");
        response.setSummary("岗位差距已保存，但还没有一份基于这次匹配生成的补短板计划。");
        response.setCurrentAction(action(
                "把岗位差距转为补短板计划",
                "先在求职作战台确认学习周期和可投入时间，生成真实的每日准备任务。",
                PLAN_ROUTE,
                null,
                "会形成当前岗位匹配之后的补短板计划，再进入模拟面试验证。"
        ));
        response.setSteps(List.of(
                step("GAP_ANALYSIS", "岗位差距", "completed", "最近一次岗位匹配已保存。", matchRecord.getCreateTime()),
                step("STUDY_PLAN", "补短板计划", "current", "需要基于当前匹配生成计划。", planRecord == null ? null : planRecord.getCreateTime()),
                step("MOCK_INTERVIEW", "模拟面试", "pending", "计划形成后再进行能力验证。", null),
                applicationStep("pending", applications)
        ));
        response.setSourceRefs(matchAndPlanSources(matchRecord, planRecord));
    }

    private void fillMockInterviewStage(
            GrowthJobPreparationLoopResponse response,
            JobBattleMatchRecord matchRecord,
            JobBattlePlanRecord planRecord,
            GrowthApplicationOutcomeResponse applications
    ) {
        MockInterviewDirection direction = resolveDirection(response.getTargetRole());
        response.setStage("MOCK_INTERVIEW");
        response.setRecommendedDirection(direction == null ? null : direction.getDirectionCode());
        response.setRecommendedDirectionName(direction == null ? null : direction.getDirectionName());
        if (direction == null) {
            response.setSummary("当前补短板计划已经保存；下一步是在模拟面试中手动选择最贴近岗位的技术方向进行验证。");
            response.setCurrentAction(action(
                    "选择方向并开始一次模拟面试",
                    "岗位名称无法安全映射到现有面试方向，因此不会替你猜测技术栈。",
                    MOCK_INTERVIEW_ROUTE,
                    30,
                    "完成后会生成一条可回溯的模拟面试表现记录。"
            ));
        } else {
            String focusDescription = response.getFocusSkills().isEmpty()
                    ? "这次面试会按当前岗位方向和难度生成题目。"
                    : "AI 出题会优先覆盖已确认的技术主题：" + String.join("、", response.getFocusSkills()) + "。";
            response.setSummary("当前补短板计划已经保存；用一次模拟面试把准备阶段转成可回溯的能力验证。");
            response.setCurrentAction(action(
                    "开始一次" + direction.getDirectionName() + "模拟面试",
                    focusDescription + " 题目生成仍受现有方向、数量和结构化输出约束。",
                    mockConfigRoute(direction.getDirectionCode(), response.getFocusSkills()),
                    30,
                    "完成后会生成一条可回溯的模拟面试表现记录，并进入投递跟进阶段。"
            ));
        }
        response.setSteps(List.of(
                step("GAP_ANALYSIS", "岗位差距", "completed", "最近一次岗位匹配已保存。", matchRecord.getCreateTime()),
                step("STUDY_PLAN", "补短板计划", "completed", "已使用当前匹配之后生成的计划。", planRecord.getCreateTime()),
                step("MOCK_INTERVIEW", "模拟面试", "current", "通过一次真实面试会话验证当前准备。", null),
                applicationStep("pending", applications)
        ));
        response.setSourceRefs(matchAndPlanSources(matchRecord, planRecord));
    }

    private void fillApplicationStage(
            GrowthJobPreparationLoopResponse response,
            JobBattleMatchRecord matchRecord,
            JobBattlePlanRecord planRecord,
            MockInterviewSession completedMock,
            GrowthApplicationOutcomeResponse applications
    ) {
        GrowthApplicationOutcomeResponse.NextAction next = applications == null ? null : applications.getNextAction();
        boolean hasApplication = nvl(applications == null ? null : applications.getTotalCount()) > 0;
        response.setStage("APPLICATION_TRACKING");
        response.setSummary("当前补短板计划之后已完成一次模拟面试；下一步由你维护真实投递和跟进状态。投递记录不会被自动归因到某一份 JD。 ");
        response.setCurrentAction(action(
                firstText(next == null ? null : next.getTitle(), "维护投递进展"),
                firstText(next == null ? null : next.getDescription(), "记录真实投递、面试或结果状态，保持求职过程可回溯。"),
                firstText(next == null ? null : next.getRoutePath(), APPLICATION_ROUTE),
                null,
                firstText(next == null ? null : next.getExpectedChange(), "投递记录会进入后续跟进汇总。")
        ));
        response.setSteps(List.of(
                step("GAP_ANALYSIS", "岗位差距", "completed", "最近一次岗位匹配已保存。", matchRecord.getCreateTime()),
                step("STUDY_PLAN", "补短板计划", "completed", "已使用当前匹配之后生成的计划。", planRecord.getCreateTime()),
                step("MOCK_INTERVIEW", "模拟面试", "completed", "已完成一次模拟面试。", observedAt(completedMock)),
                applicationStep(hasApplication ? "observed" : "current", applications)
        ));
        List<GrowthJobPreparationLoopResponse.SourceReference> sources = matchAndPlanSources(matchRecord, planRecord);
        sources.add(source("MOCK_INTERVIEW", completedMock.getId(), "最近一次已完成模拟面试", observedAt(completedMock)));
        response.setSourceRefs(sources);
    }

    private GrowthJobPreparationLoopResponse.Step applicationStep(
            String status,
            GrowthApplicationOutcomeResponse applications
    ) {
        if ("observed".equals(status)) {
            return step(
                    "APPLICATION_TRACKING",
                    "投递跟进",
                    "observed",
                    "已有用户自报投递记录；系统不会自动把它归因到当前岗位。",
                    applications == null ? null : applications.getLatestUpdatedAt()
            );
        }
        if ("current".equals(status)) {
            return step("APPLICATION_TRACKING", "投递跟进", "current", "记录真实投递和后续跟进状态。", null);
        }
        return step("APPLICATION_TRACKING", "投递跟进", "pending", "完成模拟面试后再维护真实投递事实。", null);
    }

    private List<GrowthJobPreparationLoopResponse.SourceReference> matchAndPlanSources(
            JobBattleMatchRecord matchRecord,
            JobBattlePlanRecord planRecord
    ) {
        List<GrowthJobPreparationLoopResponse.SourceReference> sources = new java.util.ArrayList<>();
        sources.add(source("JOB_BATTLE_MATCH", matchRecord.getId(), "最近一次岗位匹配", matchRecord.getCreateTime()));
        if (planRecord != null) {
            sources.add(source("JOB_BATTLE_PLAN", planRecord.getId(), "最近一次补短板计划", planRecord.getCreateTime()));
        }
        return sources;
    }

    private GrowthJobPreparationLoopResponse.SourceReference source(
            String type,
            Long id,
            String label,
            LocalDateTime observedAt
    ) {
        GrowthJobPreparationLoopResponse.SourceReference source = new GrowthJobPreparationLoopResponse.SourceReference();
        source.setSourceType(type);
        source.setSourceId(id);
        source.setLabel(label);
        source.setObservedAt(observedAt);
        return source;
    }

    private GrowthJobPreparationLoopResponse.Step step(
            String key,
            String title,
            String status,
            String description,
            LocalDateTime observedAt
    ) {
        GrowthJobPreparationLoopResponse.Step step = new GrowthJobPreparationLoopResponse.Step();
        step.setKey(key);
        step.setTitle(title);
        step.setStatus(status);
        step.setDescription(description);
        step.setObservedAt(observedAt);
        return step;
    }

    private GrowthJobPreparationLoopResponse.CurrentAction action(
            String title,
            String description,
            String routePath,
            Integer expectedMinutes,
            String expectedChange
    ) {
        GrowthJobPreparationLoopResponse.CurrentAction action = new GrowthJobPreparationLoopResponse.CurrentAction();
        action.setTitle(title);
        action.setDescription(description);
        action.setRoutePath(routePath);
        action.setExpectedMinutes(expectedMinutes);
        action.setExpectedChange(expectedChange);
        return action;
    }

    private boolean isCurrentPlan(JobBattlePlanRecord planRecord, JobBattleMatchRecord matchRecord) {
        if (planRecord == null) {
            return false;
        }
        if (matchRecord == null || matchRecord.getCreateTime() == null || planRecord.getCreateTime() == null) {
            return true;
        }
        return !planRecord.getCreateTime().isBefore(matchRecord.getCreateTime());
    }

    private MockInterviewSession latestCompletedAfter(Long userId, LocalDateTime after) {
        List<MockInterviewSession> sessions = mockInterviewSessionMapper.selectRecentCompleted(
                userId,
                RECENT_COMPLETED_INTERVIEW_LIMIT
        );
        return safeSessions(sessions).stream()
                .filter(session -> isAtOrAfter(observedAt(session), after))
                .max(Comparator.comparing(this::observedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private MockInterviewDirection resolveDirection(String targetRole) {
        if (!StringUtils.hasText(targetRole)) {
            return null;
        }
        List<MockInterviewDirection> directions = safely("mock_interview_directions", mockInterviewService::getDirections);
        String normalizedRole = normalize(targetRole);
        for (MockInterviewDirection direction : safeDirections(directions)) {
            String code = normalize(direction.getDirectionCode());
            String name = normalize(direction.getDirectionName());
            if ((StringUtils.hasText(code) && normalizedRole.contains(code))
                    || (StringUtils.hasText(name) && normalizedRole.contains(name))) {
                return direction;
            }
        }
        return safeDirections(directions).stream()
                .filter(direction -> matchesKnownDirection(normalizedRole, normalize(direction.getDirectionCode())))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesKnownDirection(String normalizedRole, String directionCode) {
        if ("java".equals(directionCode)) {
            return normalizedRole.contains("java");
        }
        if ("frontend".equals(directionCode)) {
            return normalizedRole.contains("frontend") || normalizedRole.contains("前端")
                    || normalizedRole.contains("vue") || normalizedRole.contains("react");
        }
        if ("python".equals(directionCode)) {
            return normalizedRole.contains("python");
        }
        return false;
    }

    private String mockConfigRoute(String direction, List<String> focusSkills) {
        String route = "/mock-interview/config?direction=" + encode(direction) + "&source=growth-coach";
        if (focusSkills == null || focusSkills.isEmpty()) {
            return route;
        }
        return route + "&focus=" + encode(String.join("、", focusSkills));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private List<String> focusSkills(GrowthJobBattleGapResponse gap) {
        if (gap == null || gap.getGaps() == null) {
            return List.of();
        }
        return gap.getGaps().stream()
                .filter(Objects::nonNull)
                .map(GrowthJobBattleGapResponse.GapItem::getSkill)
                .filter(StringUtils::hasText)
                .map(this::shortFocusSkill)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(MAX_FOCUS_SKILLS)
                .toList();
    }

    private String shortFocusSkill(String skill) {
        String normalized = skill.trim().replaceAll("\\s+", " ");
        return normalized.length() <= MAX_FOCUS_SKILL_LENGTH
                ? normalized
                : normalized.substring(0, MAX_FOCUS_SKILL_LENGTH);
    }

    private LocalDateTime observedAt(MockInterviewSession session) {
        if (session == null) {
            return null;
        }
        return firstTime(session.getEndTime(), session.getUpdateTime(), session.getCreateTime());
    }

    private LocalDateTime firstTime(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean isAtOrAfter(LocalDateTime observedAt, LocalDateTime after) {
        return observedAt != null && (after == null || !observedAt.isBefore(after));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private List<MockInterviewSession> safeSessions(List<MockInterviewSession> sessions) {
        return sessions == null ? List.of() : sessions.stream().filter(Objects::nonNull).toList();
    }

    private List<MockInterviewDirection> safeDirections(List<MockInterviewDirection> directions) {
        return directions == null ? List.of() : directions.stream().filter(Objects::nonNull).toList();
    }

    private <T> T safely(String source, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            log.warn("读取岗位准备闭环来源失败，source={}: {}", source, exception.getClass().getSimpleName());
            return null;
        }
    }
}
