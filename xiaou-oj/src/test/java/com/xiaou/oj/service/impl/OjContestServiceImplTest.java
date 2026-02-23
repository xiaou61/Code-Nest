package com.xiaou.oj.service.impl;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.oj.dto.contest.ContestSaveRequest;
import com.xiaou.oj.mapper.OjContestMapper;
import com.xiaou.oj.mapper.OjContestProblemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class OjContestServiceImplTest {

    @Mock
    private OjContestMapper contestMapper;

    @Mock
    private OjContestProblemMapper contestProblemMapper;

    @InjectMocks
    private OjContestServiceImpl service;

    @Test
    void createContestShouldRejectWhenEndBeforeStart() {
        ContestSaveRequest req = new ContestSaveRequest();
        req.setTitle("周赛 #1");
        req.setContestType("weekly");
        req.setStartTime(LocalDateTime.now().plusDays(1));
        req.setEndTime(LocalDateTime.now());
        req.setProblemIds(List.of(1L, 2L));
        assertThrows(BusinessException.class, () -> service.createContest(1L, req));
    }
}
