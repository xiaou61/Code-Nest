package com.xiaou.oj.contest;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.oj.domain.OjContest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContestRuleValidatorTest {

    @Test
    void shouldRejectSubmitWhenContestNotRunning() {
        OjContest contest = new OjContest()
                .setStatus(1)
                .setStartTime(LocalDateTime.now().plusHours(1))
                .setEndTime(LocalDateTime.now().plusHours(3));
        assertThrows(BusinessException.class,
                () -> ContestRuleValidator.checkCanSubmit(contest, LocalDateTime.now()));
    }

    @Test
    void shouldRejectSubmitWhenOutOfContestWindow() {
        LocalDateTime now = LocalDateTime.now();
        OjContest contest = new OjContest()
                .setStatus(2)
                .setStartTime(now.minusHours(2))
                .setEndTime(now.minusHours(1));
        assertThrows(BusinessException.class,
                () -> ContestRuleValidator.checkCanSubmit(contest, now));
    }

    @Test
    void shouldAllowSubmitWhenContestRunningAndInWindow() {
        LocalDateTime now = LocalDateTime.now();
        OjContest contest = new OjContest()
                .setStatus(2)
                .setStartTime(now.minusMinutes(30))
                .setEndTime(now.plusMinutes(30));
        assertDoesNotThrow(() -> ContestRuleValidator.checkCanSubmit(contest, now));
    }
}
