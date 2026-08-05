package com.xiaou.web.growthcoach.adapter.persistence;

import com.xiaou.mockinterview.domain.CareerLoopAction;
import com.xiaou.mockinterview.domain.JobBattleMatchRecord;
import com.xiaou.mockinterview.domain.JobBattlePlanRecord;
import com.xiaou.mockinterview.domain.MockInterviewDirection;
import com.xiaou.mockinterview.domain.MockInterviewSession;
import com.xiaou.mockinterview.dto.response.CareerLoopCurrentResponse;
import com.xiaou.mockinterview.mapper.JobBattleMatchRecordMapper;
import com.xiaou.mockinterview.mapper.JobBattlePlanRecordMapper;
import com.xiaou.mockinterview.mapper.MockInterviewSessionMapper;
import com.xiaou.mockinterview.service.CareerLoopService;
import com.xiaou.mockinterview.service.MockInterviewService;
import com.xiaou.plan.domain.GrowthAutopilotEvent;
import com.xiaou.plan.domain.GrowthAutopilotGoal;
import com.xiaou.plan.domain.GrowthAutopilotTask;
import com.xiaou.plan.mapper.GrowthAutopilotEventMapper;
import com.xiaou.plan.mapper.GrowthAutopilotGoalMapper;
import com.xiaou.plan.mapper.GrowthAutopilotTaskMapper;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 将求职与成长计划模块的持久化对象转换为 Growth Coach 自有读模型。
 */
@Component
@RequiredArgsConstructor
public class MybatisGrowthCareerDataAdapter implements GrowthCareerDataPort {

    private final CareerLoopService careerLoopService;
    private final JobBattleMatchRecordMapper matchRecordMapper;
    private final JobBattlePlanRecordMapper planRecordMapper;
    private final MockInterviewSessionMapper mockInterviewSessionMapper;
    private final MockInterviewService mockInterviewService;
    private final GrowthAutopilotGoalMapper goalMapper;
    private final GrowthAutopilotTaskMapper taskMapper;
    private final GrowthAutopilotEventMapper eventMapper;

    @Override
    public List<CareerActionData> currentCareerActions(Long userId) {
        CareerLoopCurrentResponse current = careerLoopService.findCurrentIfPresent(userId);
        if (current == null || current.getActions() == null) {
            return List.of();
        }
        return current.getActions().stream()
                .filter(Objects::nonNull)
                .map(this::toCareerAction)
                .toList();
    }

    @Override
    public void markCareerActionDone(Long userId, Long actionId) {
        careerLoopService.markExistingActionDone(userId, actionId);
    }

    @Override
    public JobBattleMatchData latestJobBattleMatch(Long userId) {
        JobBattleMatchRecord record = matchRecordMapper.selectLatestByUserId(userId);
        return record == null ? null : new JobBattleMatchData(
                record.getId(),
                record.getAnalysisName(),
                record.getBestScore(),
                record.getFallbackCount(),
                record.getBestTargetRole(),
                record.getResultJson(),
                record.getCreateTime()
        );
    }

    @Override
    public JobBattlePlanData latestJobBattlePlan(Long userId) {
        JobBattlePlanRecord record = planRecordMapper.selectLatestByUserId(userId);
        return record == null ? null : new JobBattlePlanData(
                record.getId(),
                record.getPlanName(),
                record.getGapsJson(),
                record.getPlanResultJson(),
                record.getCreateTime()
        );
    }

    @Override
    public List<MockInterviewSessionData> recentCompletedMockInterviews(Long userId, int limit) {
        List<MockInterviewSession> sessions = mockInterviewSessionMapper.selectRecentCompleted(userId, limit);
        if (sessions == null) {
            return List.of();
        }
        return sessions.stream()
                .filter(Objects::nonNull)
                .map(session -> new MockInterviewSessionData(
                        session.getId(),
                        session.getEndTime(),
                        session.getUpdateTime(),
                        session.getCreateTime()
                ))
                .toList();
    }

    @Override
    public List<MockInterviewDirectionData> mockInterviewDirections() {
        List<MockInterviewDirection> directions = mockInterviewService.getDirections();
        if (directions == null) {
            return List.of();
        }
        return directions.stream()
                .filter(Objects::nonNull)
                .map(direction -> new MockInterviewDirectionData(
                        direction.getDirectionCode(),
                        direction.getDirectionName()
                ))
                .toList();
    }

    @Override
    public WeeklyPlanData weeklyPlan(Long userId, LocalDate weekStart, int eventLimit) {
        GrowthAutopilotGoal goal = goalMapper.selectByUserAndWeek(userId, weekStart);
        if (goal == null) {
            return null;
        }
        return new WeeklyPlanData(
                goal.getId(),
                goal.getUserId(),
                goal.getWeekStart(),
                goal.getWeekEnd(),
                goal.getWeeklyHours(),
                goal.getWeeklyMinutes(),
                toTasks(taskMapper.selectByGoalId(goal.getId())),
                toEvents(eventMapper.selectLatestByGoalId(goal.getId(), eventLimit))
        );
    }

    @Override
    public List<Long> activeWeeklyPlanUserIds(LocalDate weekStart, int limit) {
        List<Long> userIds = goalMapper.selectActiveUserIdsByWeek(weekStart, limit);
        return userIds == null ? List.of() : List.copyOf(userIds);
    }

    private CareerActionData toCareerAction(CareerLoopAction action) {
        return new CareerActionData(
                action.getId(),
                action.getStage(),
                action.getActionType(),
                action.getTitle(),
                action.getDescription(),
                action.getPriority(),
                action.getStatus(),
                action.getDueDate()
        );
    }

    private List<WeeklyTaskData> toTasks(List<GrowthAutopilotTask> tasks) {
        if (tasks == null) {
            return List.of();
        }
        return tasks.stream()
                .filter(Objects::nonNull)
                .map(task -> new WeeklyTaskData(task.getStatus(), task.getTaskDate(), task.getPlannedMinutes()))
                .toList();
    }

    private List<WeeklyEventData> toEvents(List<GrowthAutopilotEvent> events) {
        if (events == null) {
            return List.of();
        }
        return events.stream()
                .filter(Objects::nonNull)
                .map(event -> new WeeklyEventData(event.getEventType()))
                .toList();
    }
}
