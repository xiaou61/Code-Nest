package com.xiaou.mockinterview.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.mockinterview.domain.CareerLoopAction;
import com.xiaou.mockinterview.domain.CareerLoopSession;
import com.xiaou.mockinterview.domain.CareerLoopSnapshot;
import com.xiaou.mockinterview.domain.CareerLoopStageLog;
import com.xiaou.mockinterview.dto.request.CareerLoopEventRequest;
import com.xiaou.mockinterview.dto.request.CareerLoopProfileUpdateRequest;
import com.xiaou.mockinterview.dto.request.CareerLoopStartRequest;
import com.xiaou.mockinterview.dto.response.CareerLoopCurrentResponse;
import com.xiaou.mockinterview.enums.CareerLoopStageEnum;
import com.xiaou.mockinterview.mapper.CareerLoopActionMapper;
import com.xiaou.mockinterview.mapper.CareerLoopSessionMapper;
import com.xiaou.mockinterview.mapper.CareerLoopSnapshotMapper;
import com.xiaou.mockinterview.mapper.CareerLoopStageLogMapper;
import com.xiaou.mockinterview.service.CareerLoopService;
import com.xiaou.mockinterview.service.CareerLoopStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 求职闭环中台服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CareerLoopServiceImpl implements CareerLoopService {

    private static final int DEFAULT_WEEKLY_HOURS = 8;
    private static final int MIN_WEEKLY_HOURS = 3;
    private static final int MAX_WEEKLY_HOURS = 40;

    private final CareerLoopSessionMapper sessionMapper;
    private final CareerLoopStageLogMapper stageLogMapper;
    private final CareerLoopActionMapper actionMapper;
    private final CareerLoopSnapshotMapper snapshotMapper;
    private final CareerLoopStateMachine stateMachine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CareerLoopSession start(Long userId, CareerLoopStartRequest request) {
        CareerLoopSession active = sessionMapper.selectActiveByUserId(userId);
        if (active != null) {
            return active;
        }

        CareerLoopSession created = new CareerLoopSession()
                .setUserId(userId)
                .setTargetRole(normalizeRole(request == null ? null : request.getTargetRole()))
                .setTargetCompanyType(request == null ? null : request.getTargetCompanyType())
                .setWeeklyHours(normalizeWeeklyHours(request == null ? null : request.getWeeklyHours()))
                .setCurrentStage(CareerLoopStageEnum.INIT.name())
                .setHealthScore(60)
                .setStatus("active");
        sessionMapper.insert(created);

        ensureSnapshot(created.getId());
        resetActionsByStage(created.getId(), CareerLoopStageEnum.INIT);
        return sessionMapper.selectActiveByUserId(userId);
    }

    @Override
    public CareerLoopCurrentResponse getCurrent(Long userId) {
        CareerLoopSession session = ensureActiveSession(userId);
        CareerLoopSnapshot snapshot = ensureSnapshot(session.getId());
        List<CareerLoopAction> actions = actionMapper.selectBySessionId(session.getId());

        return new CareerLoopCurrentResponse()
                .setSession(session)
                .setSnapshot(snapshot)
                .setActions(actions)
                .setRiskFlags(parseJsonList(snapshot.getRiskFlagsJson()))
                .setNextSuggestions(parseJsonList(snapshot.getNextSuggestionJson()));
    }

    @Override
    public List<CareerLoopStageLog> getTimeline(Long userId) {
        CareerLoopSession session = ensureActiveSession(userId);
        return stageLogMapper.selectBySessionId(session.getId());
    }

    @Override
    public List<CareerLoopAction> getActions(Long userId) {
        CareerLoopSession session = ensureActiveSession(userId);
        return actionMapper.selectBySessionId(session.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markActionDone(Long userId, Long actionId) {
        CareerLoopSession session = ensureActiveSession(userId);
        int updated = actionMapper.markDoneById(actionId, session.getId());
        if (updated <= 0) {
            throw new BusinessException("动作项不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CareerLoopSession updateProfile(Long userId, CareerLoopProfileUpdateRequest request) {
        CareerLoopSession session = ensureActiveSession(userId);
        if (request == null) {
            return session;
        }

        boolean changed = false;
        if (request.getTargetRole() != null) {
            session.setTargetRole(normalizeRole(request.getTargetRole()));
            changed = true;
        }
        if (request.getWeeklyHours() != null) {
            session.setWeeklyHours(normalizeWeeklyHours(request.getWeeklyHours()));
            changed = true;
        }
        if (!changed) {
            return session;
        }

        sessionMapper.updateById(session);
        return sessionMapper.selectActiveByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CareerLoopCurrentResponse sync(Long userId, CareerLoopEventRequest request) {
        CareerLoopEventRequest syncRequest = request == null ? new CareerLoopEventRequest() : request;
        String targetStage = inferTargetStage(syncRequest);
        if (StrUtil.isNotBlank(targetStage)) {
            syncRequest.setTargetStage(targetStage);
            syncRequest.setSource(StrUtil.blankToDefault(syncRequest.getSource(), "manual"));
            onEvent(userId, syncRequest);
        } else {
            CareerLoopSession session = ensureActiveSession(userId);
            mergeSnapshot(session.getId(), syncRequest);
        }
        return getCurrent(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onEvent(Long userId, CareerLoopEventRequest request) {
        if (request == null) {
            return;
        }
        CareerLoopSession session = ensureActiveSession(userId);
        CareerLoopStageEnum currentStage = CareerLoopStageEnum.of(session.getCurrentStage());
        CareerLoopStageEnum targetStage = StrUtil.isBlank(request.getTargetStage())
                ? currentStage : CareerLoopStageEnum.of(request.getTargetStage());
        CareerLoopStageEnum nextStage = stateMachine.next(currentStage, targetStage);

        mergeSnapshot(session.getId(), request);

        if (nextStage == currentStage) {
            return;
        }
        session.setCurrentStage(nextStage.name())
                .setHealthScore(nextHealthScore(session.getHealthScore(), nextStage));
        sessionMapper.updateById(session);

        stageLogMapper.insert(new CareerLoopStageLog()
                .setSessionId(session.getId())
                .setFromStage(currentStage.name())
                .setToStage(nextStage.name())
                .setTriggerSource(StrUtil.blankToDefault(request.getSource(), "manual"))
                .setTriggerRefId(request.getRefId())
                .setNote(request.getNote()));

        resetActionsByStage(session.getId(), nextStage);
    }

    private CareerLoopSession ensureActiveSession(Long userId) {
        CareerLoopSession session = sessionMapper.selectActiveByUserId(userId);
        if (session != null) {
            return session;
        }
        return start(userId, new CareerLoopStartRequest());
    }

    private CareerLoopSnapshot ensureSnapshot(Long sessionId) {
        CareerLoopSnapshot snapshot = snapshotMapper.selectBySessionId(sessionId);
        if (snapshot != null) {
            return snapshot;
        }
        CareerLoopSnapshot created = new CareerLoopSnapshot()
                .setSessionId(sessionId)
                .setPlanProgress(0)
                .setMockCount(0)
                .setReviewCount(0)
                .setRiskFlagsJson(JSONUtil.toJsonStr(List.of("尚未形成稳定节奏，请优先完成当前阶段核心动作")))
                .setNextSuggestionJson(JSONUtil.toJsonStr(List.of("完成当前动作后系统将自动推进下一阶段")));
        snapshotMapper.insert(created);
        return snapshotMapper.selectBySessionId(sessionId);
    }

    private void mergeSnapshot(Long sessionId, CareerLoopEventRequest request) {
        CareerLoopSnapshot snapshot = ensureSnapshot(sessionId);
        if (request.getPlanProgress() != null) {
            snapshot.setPlanProgress(Math.max(0, Math.min(100, request.getPlanProgress())));
        }
        if (request.getMockCount() != null) {
            snapshot.setMockCount(Math.max(0, request.getMockCount()));
        }
        if (request.getLatestMockScore() != null) {
            snapshot.setLatestMockScore(Math.max(0, Math.min(100, request.getLatestMockScore())));
        }
        if (request.getReviewCount() != null) {
            snapshot.setReviewCount(Math.max(0, request.getReviewCount()));
        }
        if (request.getResumeUpdatedAt() != null) {
            snapshot.setResumeUpdatedAt(request.getResumeUpdatedAt());
        }
        if (request.getRiskFlags() != null) {
            snapshot.setRiskFlagsJson(JSONUtil.toJsonStr(request.getRiskFlags()));
        }
        if (request.getNextSuggestions() != null) {
            snapshot.setNextSuggestionJson(JSONUtil.toJsonStr(request.getNextSuggestions()));
        }
        snapshotMapper.updateBySessionId(snapshot);
    }

    private int nextHealthScore(Integer currentScore, CareerLoopStageEnum stage) {
        int base = currentScore == null ? 60 : currentScore;
        int gain = switch (stage) {
            case JD_PARSED, RESUME_MATCHED -> 4;
            case PLAN_READY -> 6;
            case PLAN_EXECUTING, INTERVIEW_DONE -> 8;
            case REVIEWED -> 10;
            default -> 0;
        };
        return Math.max(0, Math.min(100, base + gain));
    }

    private String inferTargetStage(CareerLoopEventRequest request) {
        if (request.getReviewCount() != null && request.getReviewCount() > 0) {
            return CareerLoopStageEnum.REVIEWED.name();
        }
        if (request.getMockCount() != null && request.getMockCount() > 0) {
            return CareerLoopStageEnum.INTERVIEW_DONE.name();
        }
        if (request.getPlanProgress() != null && request.getPlanProgress() >= 20) {
            return CareerLoopStageEnum.PLAN_EXECUTING.name();
        }
        return null;
    }

    private List<String> parseJsonList(String jsonText) {
        if (StrUtil.isBlank(jsonText)) {
            return Collections.emptyList();
        }
        try {
            JSONArray arr = JSONUtil.parseArray(jsonText);
            List<String> result = new ArrayList<>();
            for (Object item : arr) {
                if (item != null && StrUtil.isNotBlank(item.toString())) {
                    result.add(item.toString());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("解析JSON数组失败: {}", jsonText);
            return Collections.singletonList(jsonText);
        }
    }

    private Integer normalizeWeeklyHours(Integer weeklyHours) {
        if (weeklyHours == null || weeklyHours <= 0) {
            return DEFAULT_WEEKLY_HOURS;
        }
        return Math.max(MIN_WEEKLY_HOURS, Math.min(MAX_WEEKLY_HOURS, weeklyHours));
    }

    private String normalizeRole(String targetRole) {
        if (targetRole == null) {
            return null;
        }
        String trimmed = targetRole.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void resetActionsByStage(Long sessionId, CareerLoopStageEnum stage) {
        actionMapper.deleteTodoBySessionId(sessionId);
        List<CareerLoopAction> stageActions = buildStageActions(sessionId, stage);
        for (CareerLoopAction action : stageActions) {
            actionMapper.insert(action);
        }
    }

    private List<CareerLoopAction> buildStageActions(Long sessionId, CareerLoopStageEnum stage) {
        List<CareerLoopAction> actions = new ArrayList<>();
        switch (stage) {
            case INIT -> actions.add(newAction(sessionId, stage, "setup", "先完成JD解析", "输入目标岗位JD并完成解析"));
            case JD_PARSED -> actions.add(newAction(sessionId, stage, "resume", "完成简历匹配评估", "粘贴简历并生成差距项列表"));
            case RESUME_MATCHED -> actions.add(newAction(sessionId, stage, "plan", "生成30天行动计划", "根据差距项生成可执行计划"));
            case PLAN_READY -> actions.add(newAction(sessionId, stage, "study", "开始执行计划并完成首次打卡", "建议3天内至少完成1次打卡"));
            case PLAN_EXECUTING -> {
                actions.add(newAction(sessionId, stage, "study", "本周完成3次有效打卡", "保持稳定执行节奏"));
                actions.add(newAction(sessionId, stage, "mock", "完成1次模拟面试", "进入真实问答评估环节"));
            }
            case INTERVIEW_DONE -> actions.add(newAction(sessionId, stage, "review", "完成面试复盘总结", "输出高影响修复动作并落地"));
            case REVIEWED -> actions.add(newAction(sessionId, stage, "resume", "更新简历并准备投递", "把复盘动作反映到简历与项目表达"));
        }
        return actions;
    }

    private CareerLoopAction newAction(Long sessionId, CareerLoopStageEnum stage, String actionType, String title, String desc) {
        return new CareerLoopAction()
                .setSessionId(sessionId)
                .setStage(stage.name())
                .setActionType(actionType)
                .setTitle(title)
                .setDescription(desc)
                .setPriority("P1")
                .setStatus("todo")
                .setDueDate(LocalDate.now().plusDays(3))
                .setSource("rule");
    }
}

