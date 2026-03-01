package com.xiaou.ai.dto.jobbattle;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 求职作战台-行动计划结果
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class JobBattlePlanResult {

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 总天数
     */
    private Integer totalDays;

    /**
     * 周目标
     */
    private List<String> weeklyGoals;

    /**
     * 每日任务
     */
    private List<DailyTask> dailyTasks;

    /**
     * 里程碑
     */
    private List<Milestone> milestones;

    /**
     * 风险与兜底建议
     */
    private List<String> riskAndFallback;

    /**
     * 是否为降级结果
     */
    private boolean fallback;

    @Data
    @Accessors(chain = true)
    public static class DailyTask {
        private Integer day;
        private String task;
        private Integer durationMinutes;
        private String taskType;
        private String deliverable;
    }

    @Data
    @Accessors(chain = true)
    public static class Milestone {
        private Integer day;
        private String goal;
    }

    /**
     * 创建降级结果
     */
    public static JobBattlePlanResult fallbackResult(Integer targetDays, Integer weeklyHours) {
        int days = targetDays == null || targetDays <= 0 ? 30 : targetDays;
        int hours = weeklyHours == null || weeklyHours <= 0 ? 6 : weeklyHours;

        List<String> weeklyGoals = new ArrayList<>();
        weeklyGoals.add("第1周：梳理P0技能差距并完成首轮学习");
        weeklyGoals.add("第2周：强化项目表达与技术深挖");
        weeklyGoals.add("第3周：专题面试题训练与复盘");
        weeklyGoals.add("第4周：模拟面试冲刺与简历迭代");

        List<DailyTask> dailyTasks = new ArrayList<>();
        int previewDays = Math.min(days, 7);
        for (int i = 1; i <= previewDays; i++) {
            dailyTasks.add(new DailyTask()
                    .setDay(i)
                    .setTask("完成1个高优先级能力点学习并输出笔记")
                    .setDurationMinutes(Math.min(120, Math.max(45, hours * 10)))
                    .setTaskType("learn")
                    .setDeliverable("1页知识点总结+1个示例"));
        }

        List<Milestone> milestones = new ArrayList<>();
        if (days >= 7) {
            milestones.add(new Milestone().setDay(7).setGoal("完成P0差距项首轮补齐"));
        }
        if (days >= 14) {
            milestones.add(new Milestone().setDay(14).setGoal("完成2轮项目表达优化"));
        }
        if (days >= 21) {
            milestones.add(new Milestone().setDay(21).setGoal("完成系统设计专题训练"));
        }
        milestones.add(new Milestone().setDay(days).setGoal("完成冲刺计划并复盘结果"));

        return new JobBattlePlanResult()
                .setPlanName("求职作战台冲刺计划（降级版）")
                .setTotalDays(days)
                .setWeeklyGoals(weeklyGoals)
                .setDailyTasks(dailyTasks)
                .setMilestones(milestones)
                .setRiskAndFallback(List.of("若本周时间不足，优先完成P0任务，其余任务顺延"))
                .setFallback(true);
    }
}

