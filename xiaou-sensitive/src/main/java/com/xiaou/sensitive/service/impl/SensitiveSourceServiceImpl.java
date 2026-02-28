package com.xiaou.sensitive.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.sensitive.domain.SensitiveSource;
import com.xiaou.sensitive.domain.SensitiveWord;
import com.xiaou.sensitive.dto.SensitiveSourceQuery;
import com.xiaou.sensitive.mapper.SensitiveSourceMapper;
import com.xiaou.sensitive.mapper.SensitiveWordMapper;
import com.xiaou.sensitive.api.SensitiveCheckService;
import com.xiaou.sensitive.service.SensitiveSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 敏感词来源管理服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveSourceServiceImpl implements SensitiveSourceService {

    private static final int DEFAULT_CATEGORY_ID = 3;
    private static final int DEFAULT_LEVEL = 1;
    private static final int DEFAULT_ACTION = 1;
    private static final long SYSTEM_CREATOR_ID = 0L;
    private static final int BATCH_SIZE = 500;

    private final SensitiveSourceMapper sourceMapper;
    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveCheckService sensitiveCheckService;

    @Override
    public PageResult<SensitiveSource> listSources(SensitiveSourceQuery query) {
        return PageHelper.doPage(query.getPageNum(), query.getPageSize(), () -> {
            return sourceMapper.selectSourceList(query);
        });
    }

    @Override
    public SensitiveSource getSourceById(Integer id) {
        return sourceMapper.selectSourceById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addSource(SensitiveSource source) {
        try {
            // 设置默认值
            if (source.getStatus() == null) {
                source.setStatus(1);
            }
            if (source.getSyncInterval() == null) {
                source.setSyncInterval(24); // 默认24小时
            }

            int result = sourceMapper.insertSource(source);
            if (result > 0) {
                log.info("新增词库来源成功: sourceName={}", source.getSourceName());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("新增词库来源失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSource(SensitiveSource source) {
        try {
            int result = sourceMapper.updateSource(source);
            if (result > 0) {
                log.info("更新词库来源成功: id={}", source.getId());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("更新词库来源失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSource(Integer id) {
        try {
            int result = sourceMapper.deleteSourceById(id);
            if (result > 0) {
                log.info("删除词库来源成功: id={}", id);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("删除词库来源失败", e);
            return false;
        }
    }

    @Override
    public String testConnection(Integer id) {
        try {
            SensitiveSource source = sourceMapper.selectSourceById(id);
            if (source == null) {
                return "词库来源不存在";
            }

            if ("local".equalsIgnoreCase(source.getSourceType())) {
                return "本地来源无需测试连接";
            }

            if (StrUtil.isBlank(source.getApiUrl())) {
                return "API地址为空，无需测试连接";
            }

            String targetUrl = "github".equalsIgnoreCase(source.getSourceType())
                    ? normalizeGithubUrl(source.getApiUrl())
                    : source.getApiUrl();

            // 测试HTTP连接
            HttpRequest request = HttpRequest.get(targetUrl).timeout(8000);
            if ("github".equalsIgnoreCase(source.getSourceType())) {
                request.header("User-Agent", "CodeNest-Sensitive-Sync");
            }
            String apiKey = StrUtil.trimToEmpty(source.getApiKey());
            if (StrUtil.isNotBlank(apiKey)) {
                request.header("Authorization", buildAuthorizationHeader(apiKey, "github".equalsIgnoreCase(source.getSourceType())));
            }
            HttpResponse response = request.execute();

            if (response.isOk()) {
                return "连接成功";
            } else {
                return "连接失败: HTTP " + response.getStatus();
            }
        } catch (Exception e) {
            log.error("测试连接失败: id={}", id, e);
            return "连接失败: " + e.getMessage();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SyncResult syncSource(Integer id) {
        try {
            SensitiveSource source = sourceMapper.selectSourceById(id);
            if (source == null) {
                return new SyncResult(false, 0, 0, 0, "词库来源不存在");
            }

            if (source.getStatus() == 0) {
                return new SyncResult(false, 0, 0, 0, "词库来源已禁用");
            }

            // 根据来源类型执行不同的同步逻辑
            switch (source.getSourceType()) {
                case "local":
                    return syncLocalSource(source);
                case "api":
                    return syncApiSource(source);
                case "github":
                    return syncGithubSource(source);
                default:
                    return new SyncResult(false, 0, 0, 0, "不支持的来源类型");
            }
        } catch (Exception e) {
            log.error("同步词库失败: id={}", id, e);
            return new SyncResult(false, 0, 0, 0, "同步失败: " + e.getMessage());
        }
    }

    /**
     * 同步本地来源
     */
    private SyncResult syncLocalSource(SensitiveSource source) {
        // 本地来源无需同步
        updateSyncStatus(source.getId(), 1, 0);
        return new SyncResult(true, 0, 0, 0, "本地来源无需同步");
    }

    /**
     * 同步API来源
     */
    private SyncResult syncApiSource(SensitiveSource source) {
        try {
            if (StrUtil.isBlank(source.getApiUrl())) {
                return new SyncResult(false, 0, 0, 0, "API地址为空");
            }

            String responseBody = requestRemoteContent(source, source.getApiUrl(), false);
            SyncResult result = importSourceWords(source, responseBody, "API");
            updateSyncStatus(source.getId(), result.isSuccess() ? 1 : 0, result.getAddedCount() + result.getUpdatedCount());
            return result;
        } catch (Exception e) {
            updateSyncStatus(source.getId(), 0, 0);
            log.error("同步API来源失败", e);
            return new SyncResult(false, 0, 0, 0, "同步失败: " + e.getMessage());
        }
    }

    /**
     * 同步GitHub来源
     */
    private SyncResult syncGithubSource(SensitiveSource source) {
        try {
            if (StrUtil.isBlank(source.getApiUrl())) {
                return new SyncResult(false, 0, 0, 0, "GitHub地址为空");
            }

            String githubUrl = normalizeGithubUrl(source.getApiUrl());
            String responseBody = requestRemoteContent(source, githubUrl, true);
            String content = extractGithubContent(responseBody);
            SyncResult result = importSourceWords(source, content, "GitHub");
            updateSyncStatus(source.getId(), result.isSuccess() ? 1 : 0, result.getAddedCount() + result.getUpdatedCount());
            return result;
        } catch (Exception e) {
            updateSyncStatus(source.getId(), 0, 0);
            log.error("同步GitHub来源失败", e);
            return new SyncResult(false, 0, 0, 0, "同步失败: " + e.getMessage());
        }
    }

    /**
     * 请求远端词库内容
     */
    private String requestRemoteContent(SensitiveSource source, String url, boolean github) {
        HttpRequest request = HttpRequest.get(url)
                .timeout(15000)
                .header("Accept", "application/json,text/plain,*/*");

        if (github) {
            request.header("User-Agent", "CodeNest-Sensitive-Sync");
        }

        String apiKey = StrUtil.trimToEmpty(source.getApiKey());
        if (StrUtil.isNotBlank(apiKey)) {
            request.header("Authorization", buildAuthorizationHeader(apiKey, github));
        }

        HttpResponse response = request.execute();
        if (!response.isOk()) {
            throw new IllegalStateException("远端返回异常状态: HTTP " + response.getStatus());
        }
        return response.body();
    }

    /**
     * 生成认证头
     */
    private String buildAuthorizationHeader(String apiKey, boolean github) {
        String lowerKey = apiKey.toLowerCase();
        if (lowerKey.startsWith("bearer ") || lowerKey.startsWith("token ")) {
            return apiKey;
        }
        return github ? "token " + apiKey : "Bearer " + apiKey;
    }

    /**
     * 归一化GitHub地址
     */
    private String normalizeGithubUrl(String url) {
        String trimUrl = StrUtil.trim(url);
        if (StrUtil.isBlank(trimUrl)) {
            return trimUrl;
        }
        if (trimUrl.contains("github.com") && trimUrl.contains("/blob/")) {
            return trimUrl
                    .replace("https://github.com/", "https://raw.githubusercontent.com/")
                    .replace("http://github.com/", "https://raw.githubusercontent.com/")
                    .replace("/blob/", "/");
        }
        return trimUrl;
    }

    /**
     * 提取GitHub响应内容
     */
    private String extractGithubContent(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return "";
        }

        if (!JSONUtil.isTypeJSON(responseBody)) {
            return responseBody;
        }

        Object parsed = JSONUtil.parse(responseBody);
        if (!(parsed instanceof JSONObject)) {
            return responseBody;
        }

        JSONObject obj = (JSONObject) parsed;
        if (!obj.containsKey("content")) {
            return responseBody;
        }

        String encoding = obj.getStr("encoding", "");
        String content = obj.getStr("content", "");
        if (StrUtil.equalsIgnoreCase("base64", encoding)) {
            byte[] decoded = Base64.getDecoder().decode(content.replaceAll("\\s", ""));
            return new String(decoded, StandardCharsets.UTF_8);
        }
        return content;
    }

    /**
     * 导入远端词库
     */
    private SyncResult importSourceWords(SensitiveSource source, String payload, String sourceLabel) {
        List<SyncWordCandidate> candidates = parseCandidates(payload);
        if (candidates.isEmpty()) {
            return new SyncResult(false, 0, 0, 0, sourceLabel + "词库内容为空或解析失败");
        }

        NormalizeResult normalizeResult = normalizeCandidates(candidates);
        if (normalizeResult.getCandidates().isEmpty()) {
            return new SyncResult(false, 0, 0, normalizeResult.getInvalidCount(), sourceLabel + "词库无有效词条");
        }

        SyncResult syncResult = syncWordsToDatabase(source, normalizeResult);
        if (syncResult.isSuccess() && (syncResult.getAddedCount() > 0 || syncResult.getUpdatedCount() > 0)) {
            sensitiveCheckService.refreshWordLibrary();
        }
        return syncResult;
    }

    /**
     * 解析候选词
     */
    private List<SyncWordCandidate> parseCandidates(String payload) {
        List<SyncWordCandidate> result = new ArrayList<>();
        if (StrUtil.isBlank(payload)) {
            return result;
        }

        String trimmed = payload.trim();
        if (!JSONUtil.isTypeJSON(trimmed)) {
            return parseCandidatesFromText(trimmed);
        }

        try {
            Object json = JSONUtil.parse(trimmed);
            extractCandidatesFromJson(json, result);
            if (result.isEmpty()) {
                // 兜底：JSON但结构不标准，按纯文本处理
                return parseCandidatesFromText(trimmed);
            }
            return result;
        } catch (Exception e) {
            log.warn("解析JSON词库失败，降级为纯文本解析: {}", e.getMessage());
            return parseCandidatesFromText(trimmed);
        }
    }

    private void extractCandidatesFromJson(Object jsonNode, List<SyncWordCandidate> result) {
        if (jsonNode == null) {
            return;
        }
        if (jsonNode instanceof JSONArray) {
            JSONArray array = (JSONArray) jsonNode;
            for (Object item : array) {
                if (item instanceof CharSequence) {
                    result.add(new SyncWordCandidate(item.toString()));
                } else if (item instanceof JSONObject) {
                    SyncWordCandidate candidate = candidateFromObject((JSONObject) item);
                    if (candidate != null) {
                        result.add(candidate);
                    }
                } else if (item instanceof JSONArray || item instanceof JSONObject) {
                    extractCandidatesFromJson(item, result);
                }
            }
            return;
        }
        if (jsonNode instanceof JSONObject) {
            JSONObject object = (JSONObject) jsonNode;
            SyncWordCandidate selfCandidate = candidateFromObject(object);
            if (selfCandidate != null) {
                result.add(selfCandidate);
            }
            for (String key : object.keySet()) {
                Object value = object.get(key);
                if (value instanceof JSONArray || value instanceof JSONObject) {
                    extractCandidatesFromJson(value, result);
                } else if ("words".equalsIgnoreCase(key) && value instanceof CharSequence) {
                    result.addAll(parseCandidatesFromText(value.toString()));
                }
            }
        }
    }

    private SyncWordCandidate candidateFromObject(JSONObject object) {
        String word = firstNotBlank(
                object.getStr("word"),
                object.getStr("keyword"),
                object.getStr("term")
        );
        if (StrUtil.isBlank(word)) {
            return null;
        }

        SyncWordCandidate candidate = new SyncWordCandidate(word);
        candidate.setCategoryId(parseInt(object.get("categoryId")));
        candidate.setLevel(parseInt(object.get("level")));
        candidate.setAction(parseInt(object.get("action")));
        return candidate;
    }

    private List<SyncWordCandidate> parseCandidatesFromText(String text) {
        List<SyncWordCandidate> result = new ArrayList<>();
        if (StrUtil.isBlank(text)) {
            return result;
        }

        String normalizedText = text
                .replace("，", ",")
                .replace("；", ";")
                .replace("|", "\n");

        String[] lines = normalizedText.split("\\r?\\n");
        for (String line : lines) {
            String trimLine = StrUtil.trim(line);
            if (StrUtil.isBlank(trimLine)) {
                continue;
            }
            if (trimLine.startsWith("#") || trimLine.startsWith("//")) {
                continue;
            }
            String[] parts = trimLine.split("[,;]");
            for (String part : parts) {
                String word = StrUtil.trim(part);
                if (StrUtil.isNotBlank(word)) {
                    result.add(new SyncWordCandidate(word));
                }
            }
        }
        return result;
    }

    private NormalizeResult normalizeCandidates(List<SyncWordCandidate> candidates) {
        Map<String, SyncWordCandidate> uniqueMap = new LinkedHashMap<>();
        int invalidCount = 0;

        for (SyncWordCandidate candidate : candidates) {
            String normalizedWord = normalizeWord(candidate.getWord());
            if (StrUtil.isBlank(normalizedWord)) {
                invalidCount++;
                continue;
            }
            if (normalizedWord.length() > 255) {
                invalidCount++;
                continue;
            }
            if (uniqueMap.containsKey(normalizedWord)) {
                continue;
            }

            SyncWordCandidate normalized = new SyncWordCandidate(normalizedWord);
            normalized.setCategoryId(defaultIfNull(candidate.getCategoryId(), DEFAULT_CATEGORY_ID));
            normalized.setLevel(limitRange(defaultIfNull(candidate.getLevel(), DEFAULT_LEVEL), 1, 3));
            normalized.setAction(limitRange(defaultIfNull(candidate.getAction(), DEFAULT_ACTION), 1, 3));
            uniqueMap.put(normalizedWord, normalized);
        }

        return new NormalizeResult(new ArrayList<>(uniqueMap.values()), invalidCount);
    }

    private SyncResult syncWordsToDatabase(SensitiveSource source, NormalizeResult normalizeResult) {
        List<SyncWordCandidate> normalizedCandidates = normalizeResult.getCandidates();
        List<String> words = new ArrayList<>(normalizedCandidates.size());
        for (SyncWordCandidate candidate : normalizedCandidates) {
            words.add(candidate.getWord());
        }

        Map<String, SensitiveWord> existingWordMap = loadExistingWords(words);
        List<SensitiveWord> toInsert = new ArrayList<>();
        Set<String> toEnable = new LinkedHashSet<>();

        for (SyncWordCandidate candidate : normalizedCandidates) {
            SensitiveWord existing = existingWordMap.get(candidate.getWord());
            if (existing == null) {
                SensitiveWord word = new SensitiveWord();
                word.setWord(candidate.getWord());
                word.setCategoryId(candidate.getCategoryId());
                word.setLevel(candidate.getLevel());
                word.setAction(candidate.getAction());
                word.setStatus(1);
                word.setCreatorId(SYSTEM_CREATOR_ID);
                toInsert.add(word);
                continue;
            }
            if (existing.getStatus() != null && existing.getStatus() == 0) {
                toEnable.add(existing.getWord());
            }
        }

        int addedCount = 0;
        int updatedCount = 0;

        for (List<SensitiveWord> batch : splitList(toInsert, BATCH_SIZE)) {
            if (!batch.isEmpty()) {
                addedCount += sensitiveWordMapper.batchInsertWords(batch);
            }
        }
        for (List<String> batch : splitList(new ArrayList<>(toEnable), BATCH_SIZE)) {
            if (!batch.isEmpty()) {
                updatedCount += sensitiveWordMapper.batchEnableByWords(batch);
            }
        }

        int failedCount = normalizeResult.getInvalidCount();
        String message = String.format(
                "同步完成（来源：%s），新增 %d，启用 %d，无效 %d",
                source.getSourceName(), addedCount, updatedCount, failedCount
        );
        return new SyncResult(true, addedCount, updatedCount, failedCount, message);
    }

    private Map<String, SensitiveWord> loadExistingWords(List<String> words) {
        Map<String, SensitiveWord> existingWordMap = new LinkedHashMap<>();
        for (List<String> batch : splitList(words, BATCH_SIZE)) {
            if (batch.isEmpty()) {
                continue;
            }
            List<SensitiveWord> existingWords = sensitiveWordMapper.selectWordsByWords(batch);
            if (existingWords == null || existingWords.isEmpty()) {
                continue;
            }
            for (SensitiveWord word : existingWords) {
                existingWordMap.put(word.getWord(), word);
            }
        }
        return existingWordMap;
    }

    private <T> List<List<T>> splitList(List<T> source, int batchSize) {
        List<List<T>> result = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return result;
        }
        int size = source.size();
        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            result.add(source.subList(i, end));
        }
        return result;
    }

    private String normalizeWord(String word) {
        if (word == null) {
            return null;
        }
        String normalized = word.trim();
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        // 统一全角空格和制表符，避免无效词条
        normalized = normalized.replace('\u3000', ' ').replace("\t", " ").trim();
        return normalized;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private Integer parseInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Integer limitRange(Integer value, int min, int max) {
        if (value == null) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 更新同步状态
     */
    private void updateSyncStatus(Integer sourceId, Integer syncStatus, Integer wordCount) {
        SensitiveSource source = new SensitiveSource();
        source.setId(sourceId);
        source.setSyncStatus(syncStatus);
        source.setLastSyncTime(LocalDateTime.now());
        if (wordCount != null) {
            source.setWordCount(wordCount);
        }
        sourceMapper.updateSource(source);
    }

    private static class SyncWordCandidate {
        private final String word;
        private Integer categoryId;
        private Integer level;
        private Integer action;

        private SyncWordCandidate(String word) {
            this.word = word;
        }

        public String getWord() {
            return word;
        }

        public Integer getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Integer categoryId) {
            this.categoryId = categoryId;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }

        public Integer getAction() {
            return action;
        }

        public void setAction(Integer action) {
            this.action = action;
        }
    }

    private static class NormalizeResult {
        private final List<SyncWordCandidate> candidates;
        private final int invalidCount;

        private NormalizeResult(List<SyncWordCandidate> candidates, int invalidCount) {
            this.candidates = candidates;
            this.invalidCount = invalidCount;
        }

        public List<SyncWordCandidate> getCandidates() {
            return candidates;
        }

        public int getInvalidCount() {
            return invalidCount;
        }
    }
}
