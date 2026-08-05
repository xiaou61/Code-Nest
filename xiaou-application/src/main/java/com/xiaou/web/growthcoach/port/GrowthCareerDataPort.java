package com.xiaou.web.growthcoach.port;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Growth Coach 所需的求职与计划只读数据边界。
 */
public interface GrowthCareerDataPort {

    List<CareerActionData> currentCareerActions(Long userId);

    void markCareerActionDone(Long userId, Long actionId);

    JobBattleMatchData latestJobBattleMatch(Long userId);

    JobBattlePlanData latestJobBattlePlan(Long userId);

    List<MockInterviewSessionData> recentCompletedMockInterviews(Long userId, int limit);

    List<MockInterviewDirectionData> mockInterviewDirections();

    WeeklyPlanData weeklyPlan(Long userId, LocalDate weekStart, int eventLimit);

    List<Long> activeWeeklyPlanUserIds(LocalDate weekStart, int limit);

    record CareerActionData(
            Long id,
            String stage,
            String actionType,
            String title,
            String description,
            String priority,
            String status,
            LocalDate dueDate
    ) {
    }

    record JobBattleMatchData(
            Long id,
            String analysisName,
            Integer bestScore,
            Integer fallbackCount,
            String bestTargetRole,
            String resultJson,
            LocalDateTime createdAt
    ) {
    }

    record JobBattlePlanData(
            Long id,
            String planName,
            String gapsJson,
            String planResultJson,
            LocalDateTime createdAt
    ) {
    }

    record MockInterviewSessionData(
            Long id,
            LocalDateTime endedAt,
            LocalDateTime updatedAt,
            LocalDateTime createdAt
    ) {
    }

    record MockInterviewDirectionData(String code, String name) {
    }

    record WeeklyPlanData(
            Long goalId,
            Long userId,
            LocalDate weekStart,
            LocalDate weekEnd,
            Integer weeklyHours,
            Integer weeklyMinutes,
            List<WeeklyTaskData> tasks,
            List<WeeklyEventData> events
    ) {
        public WeeklyPlanData {
            tasks = tasks == null ? List.of() : List.copyOf(tasks);
            events = events == null ? List.of() : List.copyOf(events);
        }
    }

    record WeeklyTaskData(String status, LocalDate taskDate, Integer plannedMinutes) {
    }

    record WeeklyEventData(String eventType) {
    }
}
