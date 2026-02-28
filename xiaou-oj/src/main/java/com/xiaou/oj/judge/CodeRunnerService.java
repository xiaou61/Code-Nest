package com.xiaou.oj.judge;

import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.domain.OjTestCase;
import com.xiaou.oj.dto.SelfTestRequest;
import com.xiaou.oj.dto.SelfTestResult;
import com.xiaou.oj.enums.JudgeLanguage;
import com.xiaou.oj.judge.config.OjJudgeProperties;
import com.xiaou.oj.judge.sandbox.GoJudgeClient;
import com.xiaou.oj.judge.sandbox.GoJudgeClient.ExecuteResult;
import com.xiaou.oj.judge.strategy.JudgeStrategy;
import com.xiaou.oj.mapper.OjProblemMapper;
import com.xiaou.oj.mapper.OjTestCaseMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 自由运行代码服务 (练习场)
 * 不绑定题目，直接编译运行并返回结果
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeRunnerService {

    private final GoJudgeClient goJudgeClient;
    private final OjJudgeProperties properties;
    private final List<JudgeStrategy> strategies;
    private final OjTestCaseMapper testCaseMapper;
    private final OjProblemMapper problemMapper;

    /**
     * 运行代码
     */
    public RunResult run(RunRequest request) {
        Map<String, String> compiledFileIds = null;
        try {
            JudgeLanguage language = JudgeLanguage.of(request.getLanguage());
            JudgeStrategy strategy = strategies.stream()
                    .filter(s -> s.getLanguage() == language)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("不支持的语言: " + request.getLanguage()));

            long cpuLimitNs = properties.getDefaultTimeLimit() * 1_000_000L;
            long memoryLimitBytes = properties.getDefaultMemoryLimit() * 1024 * 1024L;
            Map<String, String> sourceFiles = Map.of(strategy.getSourceFileName(), request.getCode());

            // 1. 编译 (编译产出通过 copyOutCached 缓存)
            if (strategy.getCompileArgs() != null) {
                long compileCpuLimit = properties.getMaxCompileTime() * 1_000_000L;
                ExecuteResult compileResult = goJudgeClient.run(
                        strategy.getCompileArgs(), sourceFiles, null, compileCpuLimit, memoryLimitBytes,
                        null, strategy.getCompiledFileNames());

                if (!compileResult.isAccepted() || compileResult.getExitStatus() != 0) {
                    RunResult result = new RunResult();
                    result.setStatus("compile_error");
                    result.setStderr(compileResult.getStderr() != null ? compileResult.getStderr() : compileResult.getError());
                    return result;
                }
                compiledFileIds = compileResult.getFileIds();
            }

            // 2. 运行 (引用编译产出的缓存文件)
            // 对于解释型语言 (Python), 直接传源码
            Map<String, String> runFiles = (compiledFileIds != null) ? null : sourceFiles;
            ExecuteResult runResult = goJudgeClient.run(
                    strategy.getRunArgs(), runFiles, compiledFileIds,
                    request.getStdin() != null ? request.getStdin() : "",
                    cpuLimitNs, memoryLimitBytes, null, null);

            RunResult result = new RunResult();
            result.setStdout(runResult.getStdout());
            result.setStderr(runResult.getStderr());
            result.setTimeUsed(runResult.getTimeUsed());
            result.setMemoryUsed(runResult.getMemoryUsed());
            result.setExitCode(runResult.getExitStatus());

            if (runResult.isTimeLimitExceeded()) {
                result.setStatus("time_limit_exceeded");
            } else if (runResult.isMemoryLimitExceeded()) {
                result.setStatus("memory_limit_exceeded");
            } else if (!runResult.isAccepted() || runResult.getExitStatus() != 0) {
                result.setStatus("runtime_error");
            } else {
                result.setStatus("success");
            }
            return result;

        } catch (IllegalArgumentException e) {
            RunResult result = new RunResult();
            result.setStatus("error");
            result.setStderr(e.getMessage());
            return result;
        } catch (Exception e) {
            log.error("代码运行异常", e);
            RunResult result = new RunResult();
            result.setStatus("error");
            result.setStderr("系统错误: " + e.getMessage());
            return result;
        } finally {
            // 清理 go-judge 缓存文件
            if (compiledFileIds != null) {
                compiledFileIds.values().forEach(goJudgeClient::deleteFile);
            }
        }
    }

    /**
     * 自测模式：编译+跑示例用例，同步返回结果
     */
    public SelfTestResult selfTest(SelfTestRequest request) {
        Map<String, String> compiledFileIds = null;
        try {
            OjProblem problem = problemMapper.selectById(request.getProblemId());
            if (problem == null) {
                throw new RuntimeException("题目不存在");
            }

            // 优先用 is_sample=1 的用例，没有则用 sampleInput/sampleOutput
            List<OjTestCase> sampleCases = testCaseMapper.selectSampleByProblemId(request.getProblemId());
            if (sampleCases.isEmpty() && problem.getSampleInput() != null) {
                OjTestCase fallback = new OjTestCase();
                fallback.setInput(problem.getSampleInput());
                fallback.setExpectedOutput(problem.getSampleOutput());
                sampleCases = List.of(fallback);
            }
            if (sampleCases.isEmpty()) {
                SelfTestResult result = new SelfTestResult();
                result.setResults(List.of());
                result.setPassCount(0);
                result.setTotalCount(0);
                return result;
            }

            JudgeLanguage language = JudgeLanguage.of(request.getLanguage());
            JudgeStrategy strategy = strategies.stream()
                    .filter(s -> s.getLanguage() == language)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("不支持的语言: " + request.getLanguage()));

            long cpuLimitNs = (problem.getTimeLimit() != null ? problem.getTimeLimit() : properties.getDefaultTimeLimit()) * 1_000_000L;
            long memoryLimitBytes = (problem.getMemoryLimit() != null ? problem.getMemoryLimit() : properties.getDefaultMemoryLimit()) * 1024 * 1024L;
            Map<String, String> sourceFiles = Map.of(strategy.getSourceFileName(), request.getCode());

            // 1. 编译
            if (strategy.getCompileArgs() != null) {
                long compileCpuLimit = properties.getMaxCompileTime() * 1_000_000L;
                ExecuteResult compileResult = goJudgeClient.run(
                        strategy.getCompileArgs(), sourceFiles, null, compileCpuLimit, memoryLimitBytes,
                        null, strategy.getCompiledFileNames());

                if (!compileResult.isAccepted() || compileResult.getExitStatus() != 0) {
                    SelfTestResult result = new SelfTestResult();
                    result.setCompileError(compileResult.getStderr() != null ? compileResult.getStderr() : compileResult.getError());
                    result.setResults(List.of());
                    result.setTotalCount(sampleCases.size());
                    return result;
                }
                compiledFileIds = compileResult.getFileIds();
            }

            // 2. 逐个跑示例用例
            Map<String, String> runFiles = (compiledFileIds != null) ? null : sourceFiles;
            List<SelfTestResult.TestCaseResult> results = new ArrayList<>();
            int passCount = 0;

            for (OjTestCase testCase : sampleCases) {
                SelfTestResult.TestCaseResult tcResult = new SelfTestResult.TestCaseResult();
                tcResult.setInput(testCase.getInput());
                tcResult.setExpectedOutput(testCase.getExpectedOutput());

                ExecuteResult runResult = goJudgeClient.run(
                        strategy.getRunArgs(), runFiles, compiledFileIds,
                        testCase.getInput() != null ? testCase.getInput() : "",
                        cpuLimitNs, memoryLimitBytes, null, null);

                tcResult.setTimeUsed(runResult.getTimeUsed());
                tcResult.setMemoryUsed(runResult.getMemoryUsed());
                tcResult.setActualOutput(runResult.getStdout() != null ? runResult.getStdout().strip() : "");

                if (runResult.isTimeLimitExceeded()) {
                    tcResult.setStatus("time_limit_exceeded");
                    tcResult.setPassed(false);
                } else if (runResult.isMemoryLimitExceeded()) {
                    tcResult.setStatus("memory_limit_exceeded");
                    tcResult.setPassed(false);
                } else if (!runResult.isAccepted() || runResult.getExitStatus() != 0) {
                    tcResult.setStatus("runtime_error");
                    tcResult.setErrorMessage(runResult.getStderr() != null ? runResult.getStderr() : runResult.getError());
                    tcResult.setPassed(false);
                } else {
                    boolean passed = compareOutput(runResult.getStdout(), testCase.getExpectedOutput());
                    tcResult.setPassed(passed);
                    tcResult.setStatus(passed ? "accepted" : "wrong_answer");
                    if (passed) passCount++;
                }

                results.add(tcResult);
            }

            SelfTestResult result = new SelfTestResult();
            result.setResults(results);
            result.setPassCount(passCount);
            result.setTotalCount(sampleCases.size());
            return result;

        } catch (IllegalArgumentException e) {
            SelfTestResult result = new SelfTestResult();
            result.setCompileError(e.getMessage());
            result.setResults(List.of());
            return result;
        } catch (Exception e) {
            log.error("自测异常", e);
            SelfTestResult result = new SelfTestResult();
            result.setCompileError("系统错误: " + e.getMessage());
            result.setResults(List.of());
            return result;
        } finally {
            if (compiledFileIds != null) {
                compiledFileIds.values().forEach(goJudgeClient::deleteFile);
            }
        }
    }

    private boolean compareOutput(String actual, String expected) {
        if (actual == null) actual = "";
        if (expected == null) expected = "";
        return actual.strip().equals(expected.strip());
    }

    @Data
    public static class RunRequest {
        @NotBlank(message = "语言不能为空")
        private String language;

        @NotBlank(message = "代码不能为空")
        private String code;

        /** 标准输入 (可选) */
        private String stdin;
    }

    @Data
    public static class RunResult {
        private String status;
        private String stdout;
        private String stderr;
        private long timeUsed;   // ms
        private long memoryUsed; // KB
        private int exitCode;
    }
}
