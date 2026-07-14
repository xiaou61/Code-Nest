package com.xiaou.oj.judge;

import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.domain.OjSubmission;
import com.xiaou.oj.domain.OjTestCase;
import com.xiaou.oj.enums.JudgeLanguage;
import com.xiaou.oj.enums.SubmissionStatus;
import com.xiaou.oj.judge.config.OjJudgeProperties;
import com.xiaou.oj.judge.sandbox.GoJudgeClient;
import com.xiaou.oj.judge.sandbox.GoJudgeClient.ExecuteResult;
import com.xiaou.oj.judge.strategy.JudgeStrategy;
import com.xiaou.oj.mapper.OjProblemMapper;
import com.xiaou.oj.mapper.OjSubmissionMapper;
import com.xiaou.oj.mapper.OjTestCaseMapper;
import com.xiaou.points.enums.PointsType;
import com.xiaou.points.service.PointsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JudgeServiceTest {

    @Mock
    private GoJudgeClient goJudgeClient;

    @Mock
    private OjSubmissionMapper submissionMapper;

    @Mock
    private OjProblemMapper problemMapper;

    @Mock
    private OjTestCaseMapper testCaseMapper;

    @Mock
    private JudgeStrategy strategy;

    @Mock
    private PointsService pointsService;

    private final OjJudgeProperties properties = new OjJudgeProperties();
    private JudgeService service;

    @BeforeEach
    void setUp() {
        service = new JudgeService(
                goJudgeClient,
                properties,
                submissionMapper,
                problemMapper,
                testCaseMapper,
                List.of(strategy),
                pointsService
        );

    }

    @Test
    void compileFailureShouldPreserveSandboxErrorWhenStderrIsBlank() {
        OjSubmission submission = submission();
        prepareSubmission(submission);
        when(strategy.getLanguage()).thenReturn(JudgeLanguage.JAVA);
        when(strategy.getSourceFileName()).thenReturn("Main.java");
        when(strategy.getCompileArgs()).thenReturn(List.of("/usr/bin/javac", "Main.java"));
        when(strategy.getCompiledFileNames()).thenReturn(List.of("Main.class"));

        ExecuteResult compileResult = new ExecuteResult();
        compileResult.setStatus("System Error");
        compileResult.setExitStatus(1);
        compileResult.setStderr("");
        compileResult.setError("compiler service unavailable");
        doReturn(compileResult).when(goJudgeClient).run(
                anyList(), anyMap(), isNull(), anyLong(), anyLong(), isNull(), anyList());

        service.judge(submission.getId());

        assertEquals(SubmissionStatus.COMPILE_ERROR.getValue(), submission.getStatus());
        assertEquals("compiler service unavailable", submission.getErrorMessage());
        assertEquals(1, submission.getTotalCount());
        verify(goJudgeClient, never()).deleteFile("compiled-file-id");
    }

    @Test
    void runtimeFailureShouldPreserveSandboxErrorWhenStderrIsBlank() {
        OjSubmission submission = submission().setLanguage("python");
        prepareSubmission(submission);
        when(strategy.getLanguage()).thenReturn(JudgeLanguage.PYTHON);
        when(strategy.getSourceFileName()).thenReturn("main.py");
        when(strategy.getCompileArgs()).thenReturn(null);
        when(strategy.getRunArgs()).thenReturn(List.of("/usr/bin/python3", "main.py"));

        ExecuteResult runResult = new ExecuteResult();
        runResult.setStatus("Runtime Error");
        runResult.setExitStatus(1);
        runResult.setStderr("");
        runResult.setError("sandbox process failed");
        doReturn(runResult).when(goJudgeClient).run(
                anyList(), anyMap(), isNull(), anyString(), anyLong(), anyLong(), isNull(), isNull());

        service.judge(submission.getId());

        assertEquals(SubmissionStatus.RUNTIME_ERROR.getValue(), submission.getStatus());
        assertEquals("sandbox process failed", submission.getErrorMessage());
        assertEquals(0, submission.getPassCount());
    }

    @Test
    void acceptedSubmissionShouldUpdateCountersAndCleanCompiledFiles() {
        OjSubmission submission = submission();
        prepareSubmission(submission);
        when(strategy.getLanguage()).thenReturn(JudgeLanguage.JAVA);
        when(strategy.getSourceFileName()).thenReturn("Main.java");
        when(strategy.getCompileArgs()).thenReturn(List.of("/usr/bin/javac", "Main.java"));
        when(strategy.getCompiledFileNames()).thenReturn(List.of("Main.class"));
        when(strategy.getRunArgs()).thenReturn(List.of("/usr/bin/java", "Main"));
        when(submissionMapper.existsAccepted(submission.getUserId(), submission.getProblemId())).thenReturn(true);

        ExecuteResult compileResult = new ExecuteResult();
        compileResult.setStatus("Accepted");
        compileResult.setExitStatus(0);
        compileResult.setFileIds(Map.of("Main.class", "compiled-file-id"));

        ExecuteResult runResult = new ExecuteResult();
        runResult.setStatus("Accepted");
        runResult.setExitStatus(0);
        runResult.setStdout("ok\n");
        runResult.setTimeUsed(12);
        runResult.setMemoryUsed(34);
        doReturn(compileResult).when(goJudgeClient).run(
                anyList(), anyMap(), isNull(), anyLong(), anyLong(), isNull(), anyList());
        doReturn(runResult).when(goJudgeClient).run(
                anyList(), isNull(), anyMap(), anyString(), anyLong(), anyLong(), isNull(), isNull());

        service.judge(submission.getId());

        assertEquals(SubmissionStatus.ACCEPTED.getValue(), submission.getStatus());
        assertEquals(1, submission.getPassCount());
        assertEquals(1, submission.getTotalCount());
        assertEquals(12L, submission.getTimeUsed());
        assertEquals(34L, submission.getMemoryUsed());
        verify(goJudgeClient).deleteFile("compiled-file-id");
        verify(problemMapper, never()).increaseAcceptedCount(submission.getProblemId());
        verify(pointsService, never()).grantSystemPoints(org.mockito.ArgumentMatchers.anyLong(),
                anyInt(), anyInt(),
                org.mockito.ArgumentMatchers.anyString());
        verify(submissionMapper, times(2)).updateById(submission);
    }

    @Test
    void timeLimitExceededShouldStopRemainingCasesAndKeepPassedCount() {
        OjSubmission submission = submission();
        OjTestCase firstCase = testCase().setExpectedOutput("ok");
        OjTestCase secondCase = testCase().setId(2L).setInput("slow");
        prepareSubmission(submission, firstCase, secondCase);
        when(strategy.getLanguage()).thenReturn(JudgeLanguage.JAVA);
        when(strategy.getSourceFileName()).thenReturn("Main.java");
        when(strategy.getCompileArgs()).thenReturn(List.of("/usr/bin/javac", "Main.java"));
        when(strategy.getCompiledFileNames()).thenReturn(List.of("Main.class"));
        when(strategy.getRunArgs()).thenReturn(List.of("/usr/bin/java", "Main"));

        ExecuteResult compileResult = acceptedCompileResult();
        ExecuteResult acceptedRun = acceptedRunResult("ok\n", 12, 34);
        ExecuteResult timeoutRun = new ExecuteResult();
        timeoutRun.setStatus("Time Limit Exceeded");
        timeoutRun.setExitStatus(-1);
        timeoutRun.setTimeUsed(101);
        timeoutRun.setMemoryUsed(40);
        doReturn(compileResult).when(goJudgeClient).run(
                anyList(), anyMap(), isNull(), anyLong(), anyLong(), isNull(), anyList());
        doReturn(acceptedRun, timeoutRun).when(goJudgeClient).run(
                anyList(), isNull(), anyMap(), anyString(), anyLong(), anyLong(), isNull(), isNull());

        service.judge(submission.getId());

        assertEquals(SubmissionStatus.TIME_LIMIT_EXCEEDED.getValue(), submission.getStatus());
        assertEquals(1, submission.getPassCount());
        assertEquals(2, submission.getTotalCount());
        assertEquals(101L, submission.getTimeUsed());
        verify(goJudgeClient, times(2)).run(
                anyList(), isNull(), anyMap(), anyString(), anyLong(), anyLong(), isNull(), isNull());
    }

    @Test
    void memoryLimitExceededShouldStopWithNoPassedCases() {
        OjSubmission submission = submission().setLanguage("python");
        prepareSubmission(submission);
        when(strategy.getLanguage()).thenReturn(JudgeLanguage.PYTHON);
        when(strategy.getSourceFileName()).thenReturn("main.py");
        when(strategy.getCompileArgs()).thenReturn(null);
        when(strategy.getRunArgs()).thenReturn(List.of("/usr/bin/python3", "main.py"));

        ExecuteResult memoryLimitResult = new ExecuteResult();
        memoryLimitResult.setStatus("Memory Limit Exceeded");
        memoryLimitResult.setExitStatus(-1);
        memoryLimitResult.setTimeUsed(9);
        memoryLimitResult.setMemoryUsed(65 * 1024L);
        doReturn(memoryLimitResult).when(goJudgeClient).run(
                anyList(), anyMap(), isNull(), anyString(), anyLong(), anyLong(), isNull(), isNull());

        service.judge(submission.getId());

        assertEquals(SubmissionStatus.MEMORY_LIMIT_EXCEEDED.getValue(), submission.getStatus());
        assertEquals(0, submission.getPassCount());
        assertEquals(1, submission.getTotalCount());
        assertEquals(9L, submission.getTimeUsed());
        assertEquals(65 * 1024L, submission.getMemoryUsed());
    }

    @Test
    void leadingWhitespaceDifferenceShouldBeWrongAnswer() {
        OjSubmission submission = submission();
        prepareSubmission(submission, testCase().setExpectedOutput("  ok"));
        when(strategy.getLanguage()).thenReturn(JudgeLanguage.JAVA);
        when(strategy.getSourceFileName()).thenReturn("Main.java");
        when(strategy.getCompileArgs()).thenReturn(List.of("/usr/bin/javac", "Main.java"));
        when(strategy.getCompiledFileNames()).thenReturn(List.of("Main.class"));
        when(strategy.getRunArgs()).thenReturn(List.of("/usr/bin/java", "Main"));

        doReturn(acceptedCompileResult()).when(goJudgeClient).run(
                anyList(), anyMap(), isNull(), anyLong(), anyLong(), isNull(), anyList());
        doReturn(acceptedRunResult("ok", 1, 2)).when(goJudgeClient).run(
                anyList(), isNull(), anyMap(), anyString(), anyLong(), anyLong(), isNull(), isNull());

        service.judge(submission.getId());

        assertEquals(SubmissionStatus.WRONG_ANSWER.getValue(), submission.getStatus());
        assertEquals(0, submission.getPassCount());
    }

    @Test
    void firstAcceptedSubmissionShouldUpdateCounterAndGrantDifficultyPoints() {
        OjSubmission submission = submission();
        prepareSubmission(submission, problem().setDifficulty("medium"), testCase());
        configureJavaStrategy();
        when(submissionMapper.existsAccepted(submission.getUserId(), submission.getProblemId())).thenReturn(false);
        stubAcceptedCompiledRun("ok", 11, 22);

        service.judge(submission.getId());

        assertEquals(SubmissionStatus.ACCEPTED.getValue(), submission.getStatus());
        verify(problemMapper).increaseAcceptedCount(submission.getProblemId());
        verify(pointsService).grantSystemPoints(
                submission.getUserId(),
                200,
                PointsType.OJ_AC.getCode(),
                "首次通过题目「test problem」"
        );
    }

    @Test
    void pointsFailureShouldNotOverwriteAcceptedJudgement() {
        OjSubmission submission = submission();
        prepareSubmission(submission);
        configureJavaStrategy();
        when(submissionMapper.existsAccepted(submission.getUserId(), submission.getProblemId())).thenReturn(false);
        doThrow(new IllegalStateException("points unavailable")).when(pointsService)
                .grantSystemPoints(anyLong(), anyInt(), anyInt(), anyString());
        stubAcceptedCompiledRun("ok", 11, 22);

        service.judge(submission.getId());

        assertEquals(SubmissionStatus.ACCEPTED.getValue(), submission.getStatus());
        assertNull(submission.getErrorMessage());
        verify(problemMapper).increaseAcceptedCount(submission.getProblemId());
        verify(submissionMapper, times(2)).updateById(submission);
        verify(goJudgeClient).deleteFile("compiled-file-id");
    }

    @Test
    void counterFailureShouldNotOverwriteAcceptedJudgementOrSkipPoints() {
        OjSubmission submission = submission();
        prepareSubmission(submission);
        configureJavaStrategy();
        when(submissionMapper.existsAccepted(submission.getUserId(), submission.getProblemId())).thenReturn(false);
        doThrow(new IllegalStateException("counter unavailable")).when(problemMapper)
                .increaseAcceptedCount(submission.getProblemId());
        stubAcceptedCompiledRun("ok", 11, 22);

        service.judge(submission.getId());

        assertEquals(SubmissionStatus.ACCEPTED.getValue(), submission.getStatus());
        assertNull(submission.getErrorMessage());
        verify(pointsService).grantSystemPoints(
                eq(submission.getUserId()), anyInt(), eq(PointsType.OJ_AC.getCode()), anyString());
        verify(submissionMapper, times(2)).updateById(submission);
        verify(goJudgeClient).deleteFile("compiled-file-id");
    }

    @Test
    void runtimeExceptionShouldSetSystemErrorAndCleanCompiledFiles() {
        OjSubmission submission = submission();
        prepareSubmission(submission);
        configureJavaStrategy();
        doReturn(acceptedCompileResult()).when(goJudgeClient).run(
                anyList(), anyMap(), isNull(), anyLong(), anyLong(), isNull(), anyList());
        doThrow(new IllegalStateException("sandbox crashed")).when(goJudgeClient).run(
                anyList(), isNull(), anyMap(), anyString(), anyLong(), anyLong(), isNull(), isNull());

        service.judge(submission.getId());

        assertEquals(SubmissionStatus.SYSTEM_ERROR.getValue(), submission.getStatus());
        assertTrue(submission.getErrorMessage().contains("sandbox crashed"));
        verify(goJudgeClient).deleteFile("compiled-file-id");
        verify(pointsService, never()).grantSystemPoints(anyLong(), anyInt(), anyInt(), anyString());
    }

    private void prepareSubmission(OjSubmission submission) {
        prepareSubmission(submission, testCase());
    }

    private void prepareSubmission(OjSubmission submission, OjTestCase... testCases) {
        prepareSubmission(submission, problem(), testCases);
    }

    private void prepareSubmission(OjSubmission submission, OjProblem problem, OjTestCase... testCases) {
        when(submissionMapper.selectById(submission.getId())).thenReturn(submission);
        when(problemMapper.selectById(submission.getProblemId())).thenReturn(problem);
        when(testCaseMapper.selectByProblemId(submission.getProblemId())).thenReturn(List.of(testCases));
    }

    private void configureJavaStrategy() {
        when(strategy.getLanguage()).thenReturn(JudgeLanguage.JAVA);
        when(strategy.getSourceFileName()).thenReturn("Main.java");
        when(strategy.getCompileArgs()).thenReturn(List.of("/usr/bin/javac", "Main.java"));
        when(strategy.getCompiledFileNames()).thenReturn(List.of("Main.class"));
        when(strategy.getRunArgs()).thenReturn(List.of("/usr/bin/java", "Main"));
    }

    private void stubAcceptedCompiledRun(String stdout, long timeUsed, long memoryUsed) {
        doReturn(acceptedCompileResult()).when(goJudgeClient).run(
                anyList(), anyMap(), isNull(), anyLong(), anyLong(), isNull(), anyList());
        doReturn(acceptedRunResult(stdout, timeUsed, memoryUsed)).when(goJudgeClient).run(
                anyList(), isNull(), anyMap(), anyString(), anyLong(), anyLong(), isNull(), isNull());
    }

    private ExecuteResult acceptedCompileResult() {
        ExecuteResult result = new ExecuteResult();
        result.setStatus("Accepted");
        result.setExitStatus(0);
        result.setFileIds(Map.of("Main.class", "compiled-file-id"));
        return result;
    }

    private ExecuteResult acceptedRunResult(String stdout, long timeUsed, long memoryUsed) {
        ExecuteResult result = new ExecuteResult();
        result.setStatus("Accepted");
        result.setExitStatus(0);
        result.setStdout(stdout);
        result.setTimeUsed(timeUsed);
        result.setMemoryUsed(memoryUsed);
        return result;
    }

    private OjSubmission submission() {
        return new OjSubmission()
                .setId(10L)
                .setProblemId(20L)
                .setUserId(30L)
                .setLanguage("java")
                .setCode("class Main { public static void main(String[] args) {} }");
    }

    private OjProblem problem() {
        return new OjProblem()
                .setId(20L)
                .setTitle("test problem")
                .setDifficulty("easy")
                .setTimeLimit(100)
                .setMemoryLimit(64);
    }

    private OjTestCase testCase() {
        return new OjTestCase()
                .setId(1L)
                .setProblemId(20L)
                .setInput("input")
                .setExpectedOutput("ok");
    }
}
