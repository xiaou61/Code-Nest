package com.xiaou.web.growthcoach.service;

import com.xiaou.mockinterview.dto.response.CareerApplicationSummaryResponse;
import com.xiaou.mockinterview.service.CareerApplicationService;
import com.xiaou.web.growthcoach.dto.GrowthApplicationOutcomeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 把用户主动维护的投递记录收敛为一条可执行的求职跟进行动。
 */
@Service
@RequiredArgsConstructor
public class GrowthApplicationOutcomeService {

    private static final String APPLICATION_ROUTE = "/career-loop?focus=applications";

    private final CareerApplicationService careerApplicationService;

    public GrowthApplicationOutcomeResponse getForUser(Long userId) {
        CareerApplicationSummaryResponse summary = careerApplicationService.getSummary(userId);
        GrowthApplicationOutcomeResponse response = new GrowthApplicationOutcomeResponse();
        response.setTotalCount(nvl(summary.getTotalCount()));
        response.setActiveCount(nvl(summary.getActiveCount()));
        response.setAppliedCount(nvl(summary.getAppliedCount()));
        response.setInterviewingCount(nvl(summary.getInterviewingCount()));
        response.setOfferCount(nvl(summary.getOfferCount()));
        response.setRejectedCount(nvl(summary.getRejectedCount()));
        response.setDueFollowUpCount(nvl(summary.getDueFollowUpCount()));
        response.setNextFollowUpDate(summary.getNextFollowUpDate());
        response.setLatestUpdatedAt(summary.getLatestUpdatedAt());
        response.setNextAction(resolveNextAction(response));
        return response;
    }

    private GrowthApplicationOutcomeResponse.NextAction resolveNextAction(GrowthApplicationOutcomeResponse summary) {
        GrowthApplicationOutcomeResponse.NextAction action = new GrowthApplicationOutcomeResponse.NextAction();
        action.setRoutePath(APPLICATION_ROUTE);
        if (nvl(summary.getTotalCount()) == 0) {
            action.setTitle("记录第一条投递进展");
            action.setDescription("将真实投递、面试或结果状态记入闭环，后续才能判断跟进节奏。");
            action.setExpectedChange("会形成一条用户自报的求职过程事实，并进入投递跟进汇总。");
            return action;
        }
        if (nvl(summary.getDueFollowUpCount()) > 0) {
            action.setTitle("处理 " + summary.getDueFollowUpCount() + " 条到期跟进");
            action.setDescription("这些投递仍在推进中，先更新最新状态或下一次跟进日期。");
            action.setExpectedChange("待跟进风险会从当前投递节奏中移除或重新排期。");
            action.setDueDate(summary.getNextFollowUpDate());
            return action;
        }
        if (nvl(summary.getInterviewingCount()) > 0) {
            action.setTitle("更新面试中的投递进展");
            action.setDescription("记录面试后的最新状态，并将反馈同步到下一轮准备。");
            action.setExpectedChange("求职闭环会保留最新的面试进展和跟进安排。");
            action.setDueDate(summary.getNextFollowUpDate());
            return action;
        }
        if (nvl(summary.getActiveCount()) > 0) {
            action.setTitle("安排下一次投递跟进");
            action.setDescription("为仍在推进的投递补充下一次跟进时间，避免结果信息失联。");
            action.setExpectedChange("当前投递会拥有可追踪的下一步和时间点。");
            action.setDueDate(summary.getNextFollowUpDate());
            return action;
        }
        if (nvl(summary.getOfferCount()) > 0) {
            action.setTitle("沉淀 Offer 阶段反馈");
            action.setDescription("将结果与后续选择整理到投递记录，保持求职过程可回溯。");
            action.setExpectedChange("Offer 结果会继续作为用户自报的求职过程事实保留。");
            return action;
        }
        action.setTitle("开启下一轮投递准备");
        action.setDescription("已有投递均已结束，可结合岗位差距和当前计划准备下一轮机会。");
        action.setExpectedChange("新的投递记录会重新进入跟进节奏。");
        return action;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
