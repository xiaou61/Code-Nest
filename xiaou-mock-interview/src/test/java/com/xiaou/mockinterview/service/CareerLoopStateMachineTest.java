package com.xiaou.mockinterview.service;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.mockinterview.enums.CareerLoopStageEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CareerLoopStateMachineTest {

    private final CareerLoopStateMachine machine = new CareerLoopStateMachine();

    @Test
    void shouldRejectBackwardTransition() {
        assertThrows(BusinessException.class, () ->
                machine.next(CareerLoopStageEnum.PLAN_READY, CareerLoopStageEnum.JD_PARSED));
    }

    @Test
    void shouldBeIdempotentForSameStage() {
        assertEquals(CareerLoopStageEnum.PLAN_READY,
                machine.next(CareerLoopStageEnum.PLAN_READY, CareerLoopStageEnum.PLAN_READY));
    }

    @Test
    void shouldAllowForwardTransition() {
        assertEquals(CareerLoopStageEnum.REVIEWED,
                machine.next(CareerLoopStageEnum.INTERVIEW_DONE, CareerLoopStageEnum.REVIEWED));
    }
}

