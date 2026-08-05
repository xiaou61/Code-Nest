package com.xiaou.web.growthcoach.service;

import com.xiaou.web.growthcoach.dto.GrowthCareerNextActionResponse;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort.CareerActionData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.Locale;

/**
 * 将现有求职闭环动作清单暴露为 Growth Coach 的单一下一动作。
 */
@Service
@RequiredArgsConstructor
public class GrowthCareerNextActionService {

    private final GrowthCareerDataPort careerDataPort;

    public GrowthCareerNextActionResponse getNextAction(Long userId) {
        CareerActionData action = careerDataPort.currentCareerActions(userId).stream()
                .filter(this::isOpenAction)
                .sorted(Comparator.comparingInt(this::statusWeight)
                        .thenComparingInt(item -> priorityWeight(item.priority()))
                        .thenComparing(CareerActionData::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CareerActionData::id, Comparator.nullsLast(Long::compareTo)))
                .findFirst()
                .orElse(null);
        return action == null ? null : toResponse(action);
    }

    public void markDone(Long userId, Long actionId) {
        careerDataPort.markCareerActionDone(userId, actionId);
    }

    private GrowthCareerNextActionResponse toResponse(CareerActionData action) {
        GrowthCareerNextActionResponse response = new GrowthCareerNextActionResponse();
        response.setActionId(action.id());
        response.setStage(action.stage());
        response.setActionType(action.actionType());
        response.setTitle(action.title());
        response.setDescription(action.description());
        response.setPriority(action.priority());
        response.setStatus(action.status());
        response.setDueDate(action.dueDate());
        response.setRoutePath(resolveRoute(action.actionType()));
        response.setExpectedChange("完成后会记录当前阶段动作完成；阶段推进由对应业务事件同步。");
        return response;
    }

    private boolean isOpenAction(CareerActionData action) {
        return action != null && ("todo".equalsIgnoreCase(action.status()) || "doing".equalsIgnoreCase(action.status()));
    }

    private int statusWeight(CareerActionData action) {
        return "todo".equalsIgnoreCase(action.status()) ? 0 : 1;
    }

    private int priorityWeight(String priority) {
        if ("P1".equalsIgnoreCase(priority)) {
            return 1;
        }
        if ("P2".equalsIgnoreCase(priority)) {
            return 2;
        }
        return 3;
    }

    private String resolveRoute(String actionType) {
        String type = StringUtils.hasText(actionType) ? actionType.trim().toLowerCase(Locale.ROOT) : "";
        return switch (type) {
            case "setup" -> "/job-battle?step=0&from=growth-coach&action=setup";
            case "resume" -> "/job-battle?step=1&from=growth-coach&action=resume";
            case "plan" -> "/job-battle?step=2&from=growth-coach&action=plan";
            case "study" -> "/plan";
            case "mock" -> "/mock-interview/config";
            case "review" -> "/job-battle?step=3&from=growth-coach&action=review";
            case "offer" -> "/career-loop?focus=applications";
            default -> "/career-loop";
        };
    }
}
