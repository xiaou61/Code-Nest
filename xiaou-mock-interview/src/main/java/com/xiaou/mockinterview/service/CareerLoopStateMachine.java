package com.xiaou.mockinterview.service;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.mockinterview.enums.CareerLoopStageEnum;
import org.springframework.stereotype.Component;

/**
 * 求职闭环状态机
 *
 * @author xiaou
 */
@Component
public class CareerLoopStateMachine {

    /**
     * 阶段推进（仅允许平级幂等或向前推进）
     */
    public CareerLoopStageEnum next(CareerLoopStageEnum current, CareerLoopStageEnum target) {
        CareerLoopStageEnum currentStage = current == null ? CareerLoopStageEnum.INIT : current;
        CareerLoopStageEnum targetStage = target == null ? currentStage : target;

        if (currentStage == targetStage) {
            return currentStage;
        }
        if (targetStage.getOrder() < currentStage.getOrder()) {
            throw new BusinessException("不允许回退求职闭环阶段");
        }
        return targetStage;
    }
}

