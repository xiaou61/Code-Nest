package com.xiaou.web.growthcoach.adapter.persistence;

import com.xiaou.interview.mapper.InterviewMasteryMapper;
import com.xiaou.mockinterview.mapper.CareerApplicationRecordMapper;
import com.xiaou.mockinterview.mapper.CareerLoopStageLogMapper;
import com.xiaou.mockinterview.mapper.MockInterviewSessionMapper;
import com.xiaou.oj.mapper.OjSubmissionMapper;
import com.xiaou.plan.mapper.GrowthAutopilotTaskMapper;
import com.xiaou.sqloptimizer.mapper.SqlOptimizeRecordMapper;
import com.xiaou.web.growthcoach.evidence.GrowthEvidenceAdapter;
import com.xiaou.web.growthcoach.mapper.GrowthCodeArtifactMapper;
import com.xiaou.web.growthcoach.mapper.GrowthCodeReviewRecordMapper;
import com.xiaou.web.growthcoach.port.GrowthEvidenceSourceCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * 汇集成长证据所需的跨模块 MyBatis 查询，避免编排层直接依赖外部 Mapper。
 */
@Slf4j
@Component
public class MybatisGrowthEvidenceSourceCatalog implements GrowthEvidenceSourceCatalog {

    private final List<GrowthEvidenceAdapter> adapters;
    private final GrowthAutopilotTaskMapper taskMapper;
    private final MockInterviewSessionMapper sessionMapper;
    private final CareerApplicationRecordMapper careerApplicationRecordMapper;
    private final CareerLoopStageLogMapper careerLoopStageLogMapper;
    private final InterviewMasteryMapper masteryMapper;
    private final OjSubmissionMapper submissionMapper;
    private final SqlOptimizeRecordMapper sqlOptimizeRecordMapper;
    private final GrowthCodeArtifactMapper codeArtifactMapper;
    private final GrowthCodeReviewRecordMapper codeReviewRecordMapper;

    public MybatisGrowthEvidenceSourceCatalog(
            List<GrowthEvidenceAdapter> adapters,
            GrowthAutopilotTaskMapper taskMapper,
            MockInterviewSessionMapper sessionMapper,
            CareerApplicationRecordMapper careerApplicationRecordMapper,
            CareerLoopStageLogMapper careerLoopStageLogMapper,
            InterviewMasteryMapper masteryMapper,
            OjSubmissionMapper submissionMapper,
            SqlOptimizeRecordMapper sqlOptimizeRecordMapper,
            GrowthCodeArtifactMapper codeArtifactMapper,
            GrowthCodeReviewRecordMapper codeReviewRecordMapper) {
        this.adapters = List.copyOf(adapters);
        this.taskMapper = taskMapper;
        this.sessionMapper = sessionMapper;
        this.careerApplicationRecordMapper = careerApplicationRecordMapper;
        this.careerLoopStageLogMapper = careerLoopStageLogMapper;
        this.masteryMapper = masteryMapper;
        this.submissionMapper = submissionMapper;
        this.sqlOptimizeRecordMapper = sqlOptimizeRecordMapper;
        this.codeArtifactMapper = codeArtifactMapper;
        this.codeReviewRecordMapper = codeReviewRecordMapper;
    }

    @Override
    public List<GrowthEvidenceAdapter> adapters() {
        return adapters;
    }

    @Override
    public List<Long> recentlyChangedUserIds(LocalDateTime updatedAfter, int limit) {
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
        return List.copyOf(userIds);
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
}
