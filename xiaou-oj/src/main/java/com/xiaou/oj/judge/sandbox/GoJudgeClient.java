package com.xiaou.oj.judge.sandbox;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.oj.judge.config.OjJudgeProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * go-judge REST API 客户端
 * 封装与 go-judge 沙箱服务的 HTTP 通信
 *
 * @author xiaou
 */
@Slf4j
@Component
public class GoJudgeClient {

    private final RestTemplate restTemplate;
    private final OjJudgeProperties properties;

    public GoJudgeClient(@Qualifier("ojRestTemplate") RestTemplate restTemplate,
                         OjJudgeProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 完整执行
     *
     * @param args           命令参数
     * @param files          输入文件 (key=文件名, value=文件内容)
     * @param cachedFileIn   缓存文件引用 (key=文件名, value=fileId)
     * @param stdin          标准输入
     * @param cpuLimit       CPU时间限制 (纳秒)
     * @param memoryLimit    内存限制 (字节)
     * @param copyOut        需要拷出的文件
     * @param copyOutCached  需要拷出并缓存的文件
     */
    public ExecuteResult run(List<String> args, Map<String, String> files,
                             Map<String, String> cachedFileIn,
                             String stdin, long cpuLimit, long memoryLimit,
                             List<String> copyOut, List<String> copyOutCached) {
        try {
            String url = properties.getGoJudgeUrl() + "/run";

            // 构建 cmd
            Map<String, Object> cmd = new LinkedHashMap<>();
            cmd.put("args", args);
            cmd.put("env", List.of("PATH=/usr/bin:/bin:/usr/local/bin:/usr/lib/jvm/default-java/bin"));
            cmd.put("cpuLimit", cpuLimit);
            cmd.put("memoryLimit", memoryLimit);
            cmd.put("procLimit", 50);

            // 构建 files 数组: [stdin, stdout, stderr]
            List<Map<String, Object>> fileConfigs = new ArrayList<>();

            // stdin - 纯文本
            Map<String, Object> stdinFile = new LinkedHashMap<>();
            stdinFile.put("content", (stdin != null) ? stdin : "");
            fileConfigs.add(stdinFile);

            // stdout - collector
            Map<String, Object> stdoutFile = new LinkedHashMap<>();
            stdoutFile.put("name", "stdout");
            stdoutFile.put("max", 10240);
            fileConfigs.add(stdoutFile);

            // stderr - collector
            Map<String, Object> stderrFile = new LinkedHashMap<>();
            stderrFile.put("name", "stderr");
            stderrFile.put("max", 10240);
            fileConfigs.add(stderrFile);

            cmd.put("files", fileConfigs);

            // copyIn: 源码文件 + 缓存文件引用
            Map<String, Object> copyInMap = new LinkedHashMap<>();
            if (files != null) {
                for (Map.Entry<String, String> entry : files.entrySet()) {
                    copyInMap.put(entry.getKey(), Map.of("content", entry.getValue()));
                }
            }
            if (cachedFileIn != null) {
                for (Map.Entry<String, String> entry : cachedFileIn.entrySet()) {
                    copyInMap.put(entry.getKey(), Map.of("fileId", entry.getValue()));
                }
            }
            if (!copyInMap.isEmpty()) {
                cmd.put("copyIn", copyInMap);
            }

            // copyOut: 从沙箱拷出文件内容
            if (copyOut != null && !copyOut.isEmpty()) {
                cmd.put("copyOut", copyOut);
            }

            // copyOutCached: 从沙箱拷出并缓存 (返回 fileId, 可在后续请求中引用)
            if (copyOutCached != null && !copyOutCached.isEmpty()) {
                cmd.put("copyOutCached", copyOutCached);
            }

            // 构建请求体
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("cmd", List.of(cmd));

            log.debug("go-judge request: {}", requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                });
            List<Map<String, Object>> response = responseEntity.getBody();
            if (response == null || response.isEmpty()) {
                return ExecuteResult.systemError("go-judge 返回空结果");
            }

            log.debug("go-judge response: {}", response.get(0));
            return parseResult(response.get(0));
        } catch (Exception e) {
            log.error("调用 go-judge 失败", e);
            return ExecuteResult.systemError("调用 go-judge 失败: " + e.getMessage());
        }
    }

    /**
     * 简易执行 (无 copyOut, 无 cachedFileIn)
     */
    public ExecuteResult run(List<String> args, Map<String, String> files,
                             String stdin, long cpuLimit, long memoryLimit) {
        return run(args, files, null, stdin, cpuLimit, memoryLimit, null, null);
    }

    /**
     * 带 copyOut 但无 cachedFileIn
     */
    public ExecuteResult run(List<String> args, Map<String, String> files,
                             String stdin, long cpuLimit, long memoryLimit,
                             List<String> copyOut, List<String> copyOutCached) {
        return run(args, files, null, stdin, cpuLimit, memoryLimit, copyOut, copyOutCached);
    }

    /**
     * 删除 go-judge 缓存的文件
     */
    public void deleteFile(String fileId) {
        try {
            String url = properties.getGoJudgeUrl() + "/file/" + fileId;
            restTemplate.delete(url);
        } catch (Exception e) {
            log.warn("删除 go-judge 缓存文件失败: {}", fileId, e);
        }
    }

    private ExecuteResult parseResult(Map<String, Object> result) {
        ExecuteResult executeResult = new ExecuteResult();

        // status: Accepted, Time Limit Exceeded, Memory Limit Exceeded, etc.
        String status = (String) result.get("status");
        executeResult.setStatus(status);

        // exitStatus
        Object exitObj = result.get("exitStatus");
        executeResult.setExitStatus(exitObj != null ? ((Number) exitObj).intValue() : -1);

        // time (ns -> ms)
        Object timeObj = result.get("time");
        executeResult.setTimeUsed(timeObj != null ? ((Number) timeObj).longValue() / 1_000_000 : 0);

        // memory (bytes -> KB)
        Object memObj = result.get("memory");
        executeResult.setMemoryUsed(memObj != null ? ((Number) memObj).longValue() / 1024 : 0);

        // files (stdout/stderr 内容)
        Map<String, String> outputFiles = JsonUtils.toStringMap(result.get("files"));
        if (outputFiles != null) {
            executeResult.setStdout(outputFiles.getOrDefault("stdout", ""));
            executeResult.setStderr(outputFiles.getOrDefault("stderr", ""));
        }

        // fileIds (copyOutCached 返回的缓存文件ID)
        Map<String, String> fileIds = JsonUtils.toStringMap(result.get("fileIds"));
        if (fileIds != null) {
            executeResult.setFileIds(fileIds);
        }

        // error message
        Object errObj = result.get("error");
        if (errObj != null) {
            executeResult.setError((String) errObj);
        }

        return executeResult;
    }

    /**
     * 执行结果
     */
    @Data
    public static class ExecuteResult {
        private String status;
        private int exitStatus;
        private long timeUsed;    // ms
        private long memoryUsed;  // KB
        private String stdout;
        private String stderr;
        private String error;
        /** copyOutCached 返回的文件ID映射 (文件名 -> fileId) */
        private Map<String, String> fileIds;

        /**
         * 是否执行成功 (go-judge 层面)
         */
        public boolean isAccepted() {
            return "Accepted".equals(status);
        }

        public boolean isTimeLimitExceeded() {
            return "Time Limit Exceeded".equals(status);
        }

        public boolean isMemoryLimitExceeded() {
            return "Memory Limit Exceeded".equals(status);
        }

        public static ExecuteResult systemError(String msg) {
            ExecuteResult result = new ExecuteResult();
            result.setStatus("System Error");
            result.setError(msg);
            return result;
        }
    }
}
