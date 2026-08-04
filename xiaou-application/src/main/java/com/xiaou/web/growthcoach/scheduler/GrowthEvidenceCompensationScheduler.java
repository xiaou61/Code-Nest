package com.xiaou.web.growthcoach.scheduler;

import com.xiaou.interview.mapper.InterviewMasteryMapper;
import com.xiaou.mockinterview.mapper.MockInterviewSessionMapper;
import com.xiaou.mockinterview.mapper.CareerApplicationRecordMapper;
import com.xiaou.mockinterview.mapper.CareerLoopStageLogMapper;
import com.xiaou.oj.mapper.OjSubmissionMapper;
import com.xiaou.plan.mapper.GrowthAutopilotTaskMapper;
import com.xiaou.sqloptimizer.mapper.SqlOptimizeRecordMapper;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import com.xiaou.web.growthcoach.mapper.GrowthCodeArtifactMapper;
import com.xiaou.web.growthcoach.mapper.GrowthCodeReviewRecordMapper;
import com.xiaou.web.growthcoach.service.GrowthEvidenceProjectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * 补偿应用重启或按需投影遗漏的近期成长证据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrowthEvidenceCompensationScheduler {

    private final GrowthAutopilotTaskMapper taskMapper;
    private final MockInterviewSessionMapper sessionMapper;
    private final CareerApplicationRecordMapper careerApplicationRecordMapper;
    private final CareerLoopStageLogMapper careerLoopStageLogMapper;
    private final InterviewMasteryMapper masteryMapper;
    private final OjSubmissionMapper submissionMapper;
    private final SqlOptimizeRecordMapper sqlOptimizeRecordMapper;
    private final GrowthCodeArtifactMapper codeArtifactMapper;
    private final GrowthCodeReviewRecordMapper codeReviewRecordMapper;
    private final GrowthEvidenceProjectorService projectorService;
    private final GrowthCoachProperties properties;

    @Scheduled(cron = "${xiaou.growth-coach.evidence-projection.compensation-cron:0 */10 * * * ?}")
    public void refreshRecentlyChangedUsers() {
        GrowthCoachProperties.EvidenceProjection config = properties.getEvidenceProjection();
        if (config == null || !config.isEnabled() || !config.isCompensationEnabled()) {
            return;
        }
        int limit = normalizedBatchSize(config.getCompensationUserBatchSize());
        LocalDateTime updatedAfter = LocalDateTime.now().minusMinutes(normalizedLookbackMinutes(config.getCompensationLookbackMinutes()));
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        addUsers(userIds, loadUsers("成长计划任务", () -> taskMapper.selectUserIdsChangedForEvidence(updatedAfter, limit)));
        addUsers(userIds, loadUsers("模拟面试会话", () -> sessionMapper.selectUserIdsChangedForEvidence(updatedAfter, limit)));
        addUsers(userIds, loadUsers("投递记录", () -> careerApplicationRecordMapper.selectUserIdsChangedForEvidence(updatedAfter, limit)));
        addUsers(userIds, loadUsers("求职阶段日志", () -> careerLoopStageLogMapper.selectUserIdsChangedForEvidence(updatedAfter, limit)));
        addUsers(userIds, loadUsers("面试题掌握度", () -> masteryMapper.selectUserIdsChangedForEvidence(updatedAfter, limit)));
        addUsers(userIds, loadUsers("OJ判题结果", () -> submissionMapper.selectUserIdsChangedForEvidence(updatedAfter, limit)));
        addUsers(userIds, loadUsers("SQL优化记录", () -> sqlOptimizeRecordMapper.selectUserIdsChangedForEvidence(updatedAfter, limit)));
        addUsers(userIds, loadUsers("公开代码来源", () -> codeArtifactMapper.selectUserIdsChangedForEvidence(updatedAfter, limit)));
        addUsers(userIds, loadUsers("CodePen审查", () -> codeReviewRecordMapper.selectUserIdsChangedForEvidence(updatedAfter, limit)));

        for (Long userId : userIds) {
            try {
                projectorService.refreshForUser(userId);
            } catch (RuntimeException exception) {
                log.warn("成长证据补偿投影失败: {}", exception.getClass().getSimpleName());
            }
        }
    }

    private List<Long> loadUsers(String sourceName, Supplier<List<Long>> supplier) {
        try {
            List<Long> users = supplier.get();
            return users == null ? List.of() : users;
        } catch (RuntimeException exception) {
            log.warn("成长证据补偿扫描失败，来源={}: {}", sourceName, exception.getClass().getSimpleName());
            return List.of();
        }
    }

    private void addUsers(LinkedHashSet<Long> target, List<Long> source) {
        for (Long userId : source) {
            if (userId != null && userId > 0) {
                target.add(userId);
            }
        }
    }

    private int normalizedBatchSize(int configured) {
        return configured <= 0 ? 100 : Math.min(configured, 500);
    }

    private int normalizedLookbackMinutes(int configured) {
        return configured <= 0 ? 1440 : Math.min(configured, 60 * 24 * 30);
    }
}
