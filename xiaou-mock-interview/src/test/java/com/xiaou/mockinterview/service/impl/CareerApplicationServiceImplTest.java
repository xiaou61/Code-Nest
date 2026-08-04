package com.xiaou.mockinterview.service.impl;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.mockinterview.domain.CareerApplicationRecord;
import com.xiaou.mockinterview.domain.CareerLoopSession;
import com.xiaou.mockinterview.domain.JobBattleMatchRecord;
import com.xiaou.mockinterview.domain.JobBattlePlanRecord;
import com.xiaou.mockinterview.domain.MockInterviewSession;
import com.xiaou.mockinterview.dto.request.CareerApplicationUpsertRequest;
import com.xiaou.mockinterview.dto.response.CareerApplicationResponse;
import com.xiaou.mockinterview.mapper.CareerApplicationRecordMapper;
import com.xiaou.mockinterview.mapper.CareerLoopSessionMapper;
import com.xiaou.mockinterview.mapper.JobBattleMatchRecordMapper;
import com.xiaou.mockinterview.mapper.JobBattlePlanRecordMapper;
import com.xiaou.mockinterview.mapper.MockInterviewSessionMapper;
import com.xiaou.mockinterview.service.CareerLoopService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareerApplicationServiceImplTest {

    @Mock
    private CareerApplicationRecordMapper applicationRecordMapper;
    @Mock
    private CareerLoopSessionMapper sessionMapper;
    @Mock
    private CareerLoopService careerLoopService;
    @Mock
    private JobBattleMatchRecordMapper matchRecordMapper;
    @Mock
    private JobBattlePlanRecordMapper planRecordMapper;
    @Mock
    private MockInterviewSessionMapper mockInterviewSessionMapper;

    @Test
    void persistsOnlySourceRecordsOwnedByTheCurrentUser() {
        when(sessionMapper.selectActiveByUserId(7L)).thenReturn(new CareerLoopSession().setId(5L).setUserId(7L));
        when(matchRecordMapper.selectByIdAndUserId(11L, 7L))
                .thenReturn(new JobBattleMatchRecord().setId(11L).setUserId(7L));
        when(planRecordMapper.selectByIdAndUserId(12L, 7L))
                .thenReturn(new JobBattlePlanRecord().setId(12L).setUserId(7L));
        when(mockInterviewSessionMapper.selectById(13L))
                .thenReturn(new MockInterviewSession().setId(13L).setUserId(7L));
        when(applicationRecordMapper.insert(any())).thenAnswer(invocation -> {
            CareerApplicationRecord record = invocation.getArgument(0);
            record.setId(21L);
            return 1;
        });
        when(applicationRecordMapper.selectByIdAndUser(21L, 7L)).thenAnswer(invocation -> savedRecord());

        CareerApplicationResponse response = newService().create(7L, request());

        assertThat(response.getMatchRecordId()).isEqualTo(11L);
        assertThat(response.getPlanRecordId()).isEqualTo(12L);
        assertThat(response.getMockInterviewSessionId()).isEqualTo(13L);
    }

    @Test
    void rejectsAMockInterviewOwnedByAnotherUser() {
        when(sessionMapper.selectActiveByUserId(7L)).thenReturn(new CareerLoopSession().setId(5L).setUserId(7L));
        when(matchRecordMapper.selectByIdAndUserId(11L, 7L))
                .thenReturn(new JobBattleMatchRecord().setId(11L).setUserId(7L));
        when(planRecordMapper.selectByIdAndUserId(12L, 7L))
                .thenReturn(new JobBattlePlanRecord().setId(12L).setUserId(7L));
        when(mockInterviewSessionMapper.selectById(13L))
                .thenReturn(new MockInterviewSession().setId(13L).setUserId(99L));

        assertThatThrownBy(() -> newService().create(7L, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模拟面试");
    }

    private CareerApplicationServiceImpl newService() {
        return new CareerApplicationServiceImpl(
                applicationRecordMapper,
                sessionMapper,
                careerLoopService,
                matchRecordMapper,
                planRecordMapper,
                mockInterviewSessionMapper
        );
    }

    private CareerApplicationUpsertRequest request() {
        CareerApplicationUpsertRequest request = new CareerApplicationUpsertRequest();
        request.setCompanyName("字节跳动");
        request.setPositionName("Java 后端工程师");
        request.setStatus("APPLIED");
        request.setAppliedDate(LocalDate.now());
        request.setMatchRecordId(11L);
        request.setPlanRecordId(12L);
        request.setMockInterviewSessionId(13L);
        return request;
    }

    private CareerApplicationRecord savedRecord() {
        return new CareerApplicationRecord()
                .setId(21L)
                .setUserId(7L)
                .setSessionId(5L)
                .setCompanyName("字节跳动")
                .setPositionName("Java 后端工程师")
                .setStatus("APPLIED")
                .setAppliedDate(LocalDate.now())
                .setMatchRecordId(11L)
                .setPlanRecordId(12L)
                .setMockInterviewSessionId(13L);
    }
}
