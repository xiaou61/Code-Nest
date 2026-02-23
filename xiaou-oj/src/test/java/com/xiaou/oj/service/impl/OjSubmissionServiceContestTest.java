package com.xiaou.oj.service.impl;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.oj.domain.OjContest;
import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.domain.OjSubmission;
import com.xiaou.oj.dto.SubmitCodeRequest;
import com.xiaou.oj.judge.JudgeService;
import com.xiaou.oj.mapper.OjContestMapper;
import com.xiaou.oj.mapper.OjContestParticipantMapper;
import com.xiaou.oj.mapper.OjContestProblemMapper;
import com.xiaou.oj.mapper.OjProblemMapper;
import com.xiaou.oj.mapper.OjSubmissionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OjSubmissionServiceContestTest {

    @Mock
    private OjSubmissionMapper submissionMapper;

    @Mock
    private OjProblemMapper problemMapper;

    @Mock
    private OjContestMapper contestMapper;

    @Mock
    private OjContestProblemMapper contestProblemMapper;

    @Mock
    private OjContestParticipantMapper contestParticipantMapper;

    @Mock
    private JudgeService judgeService;

    @InjectMocks
    private OjSubmissionServiceImpl service;

    @Test
    void submitShouldFailWhenUserNotJoinedContest() {
        SubmitCodeRequest req = new SubmitCodeRequest();
        req.setContestId(101L);
        req.setProblemId(1L);
        req.setLanguage("java");
        req.setCode("class Main{}");

        when(problemMapper.selectById(1L)).thenReturn(new OjProblem().setId(1L));
        when(contestMapper.selectById(101L)).thenReturn(buildRunningContest(101L));
        when(contestParticipantMapper.existsByContestIdAndUserId(101L, 1001L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.submitCode(1001L, req));
        verify(submissionMapper, never()).insert(any(OjSubmission.class));
    }

    @Test
    void submitShouldWriteContestIdWhenContestContextProvided() {
        SubmitCodeRequest req = new SubmitCodeRequest();
        req.setContestId(101L);
        req.setProblemId(1L);
        req.setLanguage("java");
        req.setCode("class Main{}");

        when(problemMapper.selectById(1L)).thenReturn(new OjProblem().setId(1L));
        when(contestMapper.selectById(101L)).thenReturn(buildRunningContest(101L));
        when(contestParticipantMapper.existsByContestIdAndUserId(101L, 1001L)).thenReturn(true);
        when(contestProblemMapper.existsProblemInContest(101L, 1L)).thenReturn(true);
        doAnswer(invocation -> {
            OjSubmission submission = invocation.getArgument(0);
            submission.setId(66L);
            return 1;
        }).when(submissionMapper).insert(any(OjSubmission.class));

        Long submissionId = service.submitCode(1001L, req);

        ArgumentCaptor<OjSubmission> captor = ArgumentCaptor.forClass(OjSubmission.class);
        verify(submissionMapper).insert(captor.capture());
        assertEquals(101L, captor.getValue().getContestId());
        assertEquals(66L, submissionId);
        verify(judgeService).judge(eq(66L));
    }

    private OjContest buildRunningContest(Long contestId) {
        LocalDateTime now = LocalDateTime.now();
        return new OjContest()
                .setId(contestId)
                .setStatus(2)
                .setStartTime(now.minusMinutes(20))
                .setEndTime(now.plusMinutes(20));
    }
}
