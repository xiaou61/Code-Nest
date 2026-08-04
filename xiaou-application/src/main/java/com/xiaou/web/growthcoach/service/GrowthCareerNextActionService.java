package com.xiaou.web.growthcoach.service;

import com.xiaou.mockinterview.domain.CareerLoopAction;
import com.xiaou.mockinterview.dto.response.CareerLoopCurrentResponse;
import com.xiaou.mockinterview.service.CareerLoopService;
import com.xiaou.web.growthcoach.dto.GrowthCareerNextActionResponse;
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

    private final CareerLoopService careerLoopService;

    public GrowthCareerNextActionResponse getNextAction(Long userId) {
        CareerLoopCurrentResponse current = careerLoopService.findCurrentIfPresent(userId);
        if (current == null || current.getActions() == null) {
            return null;
        }
        CareerLoopAction action = current.getActions().stream()
                .filter(this::isOpenAction)
                .sorted(Comparator.comparingInt(this::statusWeight)
                        .thenComparingInt(item -> priorityWeight(item.getPriority()))
                        .thenComparing(CareerLoopAction::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CareerLoopAction::getId, Comparator.nullsLast(Long::compareTo)))
                .findFirst()
                .orElse(null);
        return action == null ? null : toResponse(action);
    }

    public void markDone(Long userId, Long actionId) {
        careerLoopService.markExistingActionDone(userId, actionId);
    }

    private GrowthCareerNextActionResponse toResponse(CareerLoopAction action) {
        GrowthCareerNextActionResponse response = new GrowthCareerNextActionResponse();
        response.setActionId(action.getId());
        response.setStage(action.getStage());
        response.setActionType(action.getActionType());
        response.setTitle(action.getTitle());
        response.setDescription(action.getDescription());
        response.setPriority(action.getPriority());
        response.setStatus(action.getStatus());
        response.setDueDate(action.getDueDate());
        response.setRoutePath(resolveRoute(action.getActionType()));
        response.setExpectedChange("完成后会记录当前阶段动作完成；阶段推进由对应业务事件同步。");
        return response;
    }

    private boolean isOpenAction(CareerLoopAction action) {
        return action != null && ("todo".equalsIgnoreCase(action.getStatus()) || "doing".equalsIgnoreCase(action.getStatus()));
    }

    private int statusWeight(CareerLoopAction action) {
        return "todo".equalsIgnoreCase(action.getStatus()) ? 0 : 1;
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
