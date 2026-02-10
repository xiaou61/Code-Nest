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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 异步判题服务
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {

    private final GoJudgeClient goJudgeClient;
    private final OjJudgeProperties properties;
    private final OjSubmissionMapper submissionMapper;
    private final OjProblemMapper problemMapper;
    private final OjTestCaseMapper testCaseMapper;
    private final List<JudgeStrategy> strategies;

    /**
     * 异步执行判题
     */
    @Async
    public void judge(Long submissionId) {
        OjSubmission submission = null;
        try {
            submission = submissionMapper.selectById(submissionId);
            if (submission == null) {
                log.error("[Judge] 提交记录不存在: {}", submissionId);
                return;
            }

            // 更新为判题中
            submission.setStatus(SubmissionStatus.JUDGING.getValue());
            submissionMapper.updateById(submission);
            log.info("[Judge] 开始判题: submissionId={}, problemId={}, language={}",
                    submissionId, submission.getProblemId(), submission.getLanguage());

            OjProblem problem = problemMapper.selectById(submission.getProblemId());
            List<OjTestCase> testCases = testCaseMapper.selectByProblemId(submission.getProblemId());
            log.info("[Judge] 加载完成: problem={}, testCases={}",
                    problem != null ? problem.getTitle() : "null", testCases.size());

            if (testCases.isEmpty()) {
                updateSubmission(submission, SubmissionStatus.SYSTEM_ERROR, 0, 0, 0, 0, "没有测试用例");
                return;
            }

            JudgeLanguage language = JudgeLanguage.of(submission.getLanguage());
            JudgeStrategy strategy = getStrategy(language);

            long timeLimit = problem.getTimeLimit() != null ? problem.getTimeLimit() : properties.getDefaultTimeLimit();
            long memoryLimit = problem.getMemoryLimit() != null ? problem.getMemoryLimit() : properties.getDefaultMemoryLimit();

            // 纳秒 和 字节
            long cpuLimitNs = timeLimit * 1_000_000L;
            long memoryLimitBytes = memoryLimit * 1024 * 1024L;

            Map<String, String> sourceFiles = Map.of(strategy.getSourceFileName(), submission.getCode());

            // 1. 编译阶段 (编译产出通过 copyOutCached 缓存)
            Map<String, String> compiledFileIds = null;
            if (strategy.getCompileArgs() != null) {
                log.info("[Judge] 开始编译: submissionId={}", submissionId);
                long compileCpuLimit = properties.getMaxCompileTime() * 1_000_000L;
                ExecuteResult compileResult = goJudgeClient.run(
                        strategy.getCompileArgs(), sourceFiles, null, compileCpuLimit, memoryLimitBytes,
                        null, strategy.getCompiledFileNames());

                if (!compileResult.isAccepted() || compileResult.getExitStatus() != 0) {
                    String errorMsg = compileResult.getStderr() != null ? compileResult.getStderr() : compileResult.getError();
                    log.info("[Judge] 编译失败: submissionId={}, error={}", submissionId, errorMsg);
                    updateSubmission(submission, SubmissionStatus.COMPILE_ERROR, 0, 0, 0, testCases.size(), errorMsg);
                    return;
                }
                compiledFileIds = compileResult.getFileIds();
                log.info("[Judge] 编译成功: submissionId={}, fileIds={}", submissionId, compiledFileIds);
            }

            // 2. 运行阶段 - 逐个测试用例
            int passCount = 0;
            long maxTime = 0;
            long maxMemory = 0;
            Map<String, String> runFiles = (compiledFileIds != null) ? null : sourceFiles;

            try {
                for (int i = 0; i < testCases.size(); i++) {
                    OjTestCase testCase = testCases.get(i);
                    log.info("[Judge] 运行用例 {}/{}: submissionId={}", i + 1, testCases.size(), submissionId);

                    ExecuteResult runResult = goJudgeClient.run(
                            strategy.getRunArgs(), runFiles, compiledFileIds,
                            testCase.getInput(), cpuLimitNs, memoryLimitBytes, null, null);

                    maxTime = Math.max(maxTime, runResult.getTimeUsed());
                    maxMemory = Math.max(maxMemory, runResult.getMemoryUsed());

                    if (runResult.isTimeLimitExceeded()) {
                        log.info("[Judge] 超时: submissionId={}, case={}", submissionId, i + 1);
                        updateSubmission(submission, SubmissionStatus.TIME_LIMIT_EXCEEDED,
                                maxTime, maxMemory, passCount, testCases.size(), null);
                        return;
                    }

                    if (runResult.isMemoryLimitExceeded()) {
                        log.info("[Judge] 超内存: submissionId={}, case={}", submissionId, i + 1);
                        updateSubmission(submission, SubmissionStatus.MEMORY_LIMIT_EXCEEDED,
                                maxTime, maxMemory, passCount, testCases.size(), null);
                        return;
                    }

                    if (!runResult.isAccepted() || runResult.getExitStatus() != 0) {
                        String errorMsg = runResult.getStderr() != null ? runResult.getStderr() : runResult.getError();
                        log.info("[Judge] 运行错误: submissionId={}, case={}, error={}", submissionId, i + 1, errorMsg);
                        updateSubmission(submission, SubmissionStatus.RUNTIME_ERROR,
                                maxTime, maxMemory, passCount, testCases.size(), errorMsg);
                        return;
                    }

                    // 比较输出
                    if (!compareOutput(runResult.getStdout(), testCase.getExpectedOutput())) {
                        log.info("[Judge] 答案错误: submissionId={}, case={}", submissionId, i + 1);
                        updateSubmission(submission, SubmissionStatus.WRONG_ANSWER,
                                maxTime, maxMemory, passCount, testCases.size(), null);
                        return;
                    }

                    passCount++;
                }

                // 全部通过
                log.info("[Judge] 全部通过: submissionId={}, time={}ms, memory={}KB", submissionId, maxTime, maxMemory);

                // 更新题目AC数 (仅首次AC) — 必须在 updateSubmission 之前检查，否则会查到自身
                boolean firstAc = !submissionMapper.existsAccepted(submission.getUserId(), submission.getProblemId());
                updateSubmission(submission, SubmissionStatus.ACCEPTED, maxTime, maxMemory, passCount, testCases.size(), null);
                if (firstAc) {
                    problemMapper.increaseAcceptedCount(submission.getProblemId());
                }
            } finally {
                // 清理 go-judge 缓存文件
                if (compiledFileIds != null) {
                    compiledFileIds.values().forEach(goJudgeClient::deleteFile);
                }
            }

        } catch (Throwable t) {
            log.error("[Judge] 判题异常: submissionId={}", submissionId, t);
            if (submission != null) {
                try {
                    updateSubmission(submission, SubmissionStatus.SYSTEM_ERROR, 0, 0, 0, 0, "系统错误: " + t.getMessage());
                } catch (Throwable t2) {
                    log.error("[Judge] 更新状态也失败: submissionId={}", submissionId, t2);
                }
            }
        }
    }

    private JudgeStrategy getStrategy(JudgeLanguage language) {
        return strategies.stream()
                .filter(s -> s.getLanguage() == language)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("不支持的语言: " + language.getValue()));
    }

    /**
     * 比较用户输出与期望输出 (忽略末尾空白)
     */
    private boolean compareOutput(String actual, String expected) {
        if (actual == null) actual = "";
        if (expected == null) expected = "";
        return actual.strip().equals(expected.strip());
    }

    private void updateSubmission(OjSubmission submission, SubmissionStatus status,
                                  long timeUsed, long memoryUsed, int passCount, int totalCount, String errorMessage) {
        submission.setStatus(status.getValue());
        submission.setTimeUsed(timeUsed);
        submission.setMemoryUsed(memoryUsed);
        submission.setPassCount(passCount);
        submission.setTotalCount(totalCount);
        if (errorMessage != null && errorMessage.length() > 4000) {
            errorMessage = errorMessage.substring(0, 4000);
        }
        submission.setErrorMessage(errorMessage);
        submissionMapper.updateById(submission);
    }
}
