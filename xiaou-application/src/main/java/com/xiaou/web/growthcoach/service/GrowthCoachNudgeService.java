package com.xiaou.web.growthcoach.service;

import com.xiaou.notification.api.NotificationCommand;
import com.xiaou.notification.api.NotificationPublisher;
import com.xiaou.web.growthcoach.domain.GrowthCoachNudge;
import com.xiaou.web.growthcoach.dto.GrowthWeeklyReviewResponse;
import com.xiaou.web.growthcoach.mapper.GrowthCoachNudgeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 对确定性的本周节奏风险信号发送一次站内提醒。
 *
 * 不会自动调整计划；唯一键保证同一用户每周最多收到一次同类提醒。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthCoachNudgeService {

    private static final String NUDGE_TYPE = "WEEKLY_RISK";
    private static final String STATUS_PENDING = "PENDING";
    private static final String ROUTE_PATH = "/growth-autopilot";

    private final GrowthCoachNudgeMapper nudgeMapper;
    private final NotificationPublisher notificationPublisher;

    @Transactional(rollbackFor = Exception.class)
    public void dispatchWeeklyRisk(Long userId, GrowthWeeklyReviewResponse review) {
        if (!isActionable(review) || review.getWeekStart() == null || userId == null || userId <= 0) {
            return;
        }

        GrowthCoachNudge nudge = new GrowthCoachNudge();
        nudge.setUserId(userId);
        nudge.setNudgeKey("weekly-risk:" + review.getWeekStart());
        nudge.setNudgeType(NUDGE_TYPE);
        nudge.setLevel(review.getLevel());
        nudge.setTitle("high_risk".equalsIgnoreCase(review.getLevel())
                ? "本周成长计划需要调整"
                : "本周成长计划需要关注");
        nudge.setContent(buildContent(review));
        nudge.setRoutePath(ROUTE_PATH);
        nudge.setStatus(STATUS_PENDING);
        if (nudgeMapper.insertIgnore(nudge) != 1) {
            return;
        }
        if (nudge.getId() == null) {
            nudge.setId(nudgeMapper.selectIdByUserAndNudgeKey(userId, nudge.getNudgeKey()));
        }
        if (nudge.getId() == null) {
            throw new IllegalStateException("成长教练提醒记录创建失败");
        }

        Optional<Long> notificationId = notificationPublisher.publish(NotificationCommand.toUser(
                userId,
                nudge.getTitle(),
                nudge.getContent(),
                "SYSTEM",
                "MEDIUM",
                "growth_coach",
                String.valueOf(nudge.getId())
        ));
        if (notificationId.isEmpty()) {
            nudgeMapper.markFailed(nudge.getId(), userId);
            return;
        }
        if (nudgeMapper.markSent(nudge.getId(), userId, notificationId.get()) != 1) {
            throw new IllegalStateException("成长教练提醒状态更新失败");
        }
    }

    private boolean isActionable(GrowthWeeklyReviewResponse review) {
        return review != null
                && ("high_risk".equalsIgnoreCase(review.getLevel())
                || "attention".equalsIgnoreCase(review.getLevel()));
    }

    private String buildContent(GrowthWeeklyReviewResponse review) {
        int signals = safeSignals(review.getSignals()).size();
        int overdueOrMissed = nvl(review.getOverdueTasks()) + nvl(review.getMissedTasks());
        if (overdueOrMissed > 0) {
            return "本周有 " + overdueOrMissed + " 个逾期或错过任务，并检测到 " + signals
                    + " 项节奏风险。请进入成长驾驶舱查看复盘，确认后再调整计划。";
        }
        return "本周检测到 " + signals + " 项节奏风险。请进入成长驾驶舱查看复盘，确认后再调整计划。";
    }

    private List<GrowthWeeklyReviewResponse.RiskSignal> safeSignals(
            List<GrowthWeeklyReviewResponse.RiskSignal> signals
    ) {
        return signals == null ? List.of() : signals;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
