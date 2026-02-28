package com.xiaou.sensitive.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.sensitive.api.SensitiveCheckService;
import com.xiaou.sensitive.domain.SensitiveCategory;
import com.xiaou.sensitive.domain.SensitiveWord;
import com.xiaou.sensitive.dto.SensitiveWordDTO;
import com.xiaou.sensitive.dto.SensitiveWordQuery;
import com.xiaou.sensitive.mapper.SensitiveCategoryMapper;
import com.xiaou.sensitive.mapper.SensitiveWordMapper;
import com.xiaou.sensitive.service.SensitiveVersionService;
import com.xiaou.sensitive.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 敏感词管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private static final int DEFAULT_CATEGORY_ID = 3;
    private static final int DEFAULT_LEVEL = 1;
    private static final int DEFAULT_ACTION = 1;
    private static final int DEFAULT_STATUS = 1;
    private static final long DEFAULT_OPERATOR_ID = 0L;
    private static final int BATCH_SIZE = 500;
    private static final int SAMPLE_SIZE = 50;

    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveCategoryMapper sensitiveCategoryMapper;
    private final SensitiveCheckService sensitiveCheckService;
    private final SensitiveVersionService sensitiveVersionService;

    @Override
    public PageResult<SensitiveWordDTO> listWords(SensitiveWordQuery query) {
        return PageHelper.doPage(query.getPageNum(), query.getPageSize(), () -> sensitiveWordMapper.selectWordList(query));
    }

    @Override
    public SensitiveWord getWordById(Long id) {
        return sensitiveWordMapper.selectWordById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addWord(SensitiveWord word) {
        try {
            String normalizedWord = normalizeWord(word.getWord());
            if (StrUtil.isBlank(normalizedWord)) {
                return false;
            }

            SensitiveWord existingWord = sensitiveWordMapper.selectWordByWord(normalizedWord);
            if (existingWord != null) {
                log.warn("敏感词已存在：{}", normalizedWord);
                return false;
            }

            word.setWord(normalizedWord);
            if (word.getCategoryId() == null) {
                word.setCategoryId(DEFAULT_CATEGORY_ID);
            }
            if (word.getLevel() == null) {
                word.setLevel(DEFAULT_LEVEL);
            }
            if (word.getAction() == null) {
                word.setAction(DEFAULT_ACTION);
            }
            if (word.getStatus() == null) {
                word.setStatus(DEFAULT_STATUS);
            }

            int result = sensitiveWordMapper.insertWord(word);
            if (result <= 0) {
                return false;
            }

            sensitiveCheckService.refreshWordLibrary();
            long operatorId = word.getCreatorId() == null ? DEFAULT_OPERATOR_ID : word.getCreatorId();
            recordVersionSafely(
                    "add",
                    1,
                    JSONUtil.toJsonStr(Map.of("word", normalizedWord, "categoryId", word.getCategoryId())),
                    operatorId,
                    "新增敏感词"
            );
            return true;
        } catch (Exception e) {
            log.error("新增敏感词失败：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWord(SensitiveWord word) {
        try {
            int result = sensitiveWordMapper.updateWord(word);
            if (result <= 0) {
                return false;
            }

            sensitiveCheckService.refreshWordLibrary();
            long operatorId = word.getCreatorId() == null ? DEFAULT_OPERATOR_ID : word.getCreatorId();
            recordVersionSafely(
                    "update",
                    1,
                    JSONUtil.toJsonStr(Map.of("id", word.getId(), "word", StrUtil.blankToDefault(word.getWord(), ""))),
                    operatorId,
                    "更新敏感词"
            );
            return true;
        } catch (Exception e) {
            log.error("更新敏感词失败：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteWord(Long id) {
        try {
            SensitiveWord oldWord = sensitiveWordMapper.selectWordById(id);
            int result = sensitiveWordMapper.deleteWordById(id);
            if (result <= 0) {
                return false;
            }

            sensitiveCheckService.refreshWordLibrary();
            recordVersionSafely(
                    "delete",
                    1,
                    JSONUtil.toJsonStr(Map.of(
                            "id", id,
                            "word", oldWord == null ? "" : StrUtil.blankToDefault(oldWord.getWord(), "")
                    )),
                    DEFAULT_OPERATOR_ID,
                    "删除敏感词"
            );
            return true;
        } catch (Exception e) {
            log.error("删除敏感词失败：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteWords(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return false;
            }

            int result = sensitiveWordMapper.deleteWordByIds(ids);
            if (result <= 0) {
                return false;
            }

            sensitiveCheckService.refreshWordLibrary();
            recordVersionSafely(
                    "delete",
                    result,
                    JSONUtil.toJsonStr(Map.of("ids", ids)),
                    DEFAULT_OPERATOR_ID,
                    "批量删除敏感词"
            );
            return true;
        } catch (Exception e) {
            log.error("批量删除敏感词失败：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public ImportPreviewResult previewImportWords(List<String> words) {
        ImportPrepareResult prepareResult = prepareImportWords(words);
        if (prepareResult.validWords.isEmpty()) {
            return new ImportPreviewResult(
                    prepareResult.total,
                    0,
                    prepareResult.invalidCount,
                    prepareResult.duplicateInFileCount,
                    0,
                    0,
                    List.of(),
                    List.of(),
                    prepareResult.invalidSamples
            );
        }

        Set<String> dbDuplicates = loadExistingWordSet(prepareResult.validWords);
        List<String> importable = new ArrayList<>();
        List<String> duplicateSamples = new ArrayList<>(prepareResult.duplicateInFileSamples);
        for (String word : prepareResult.validWords) {
            if (dbDuplicates.contains(word)) {
                if (duplicateSamples.size() < SAMPLE_SIZE) {
                    duplicateSamples.add(word);
                }
            } else {
                importable.add(word);
            }
        }

        return new ImportPreviewResult(
                prepareResult.total,
                prepareResult.validWords.size(),
                prepareResult.invalidCount,
                prepareResult.duplicateInFileCount,
                dbDuplicates.size(),
                importable.size(),
                sample(importable, SAMPLE_SIZE),
                duplicateSamples,
                prepareResult.invalidSamples
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importWords(List<String> words, Long creatorId) {
        ImportPrepareResult prepareResult = prepareImportWords(words);
        List<String> errors = new ArrayList<>();
        int success = 0;

        try {
            if (prepareResult.validWords.isEmpty()) {
                if (prepareResult.invalidCount > 0) {
                    errors.add("无有效词条，可检查词长或空白行");
                }
                return new ImportResult(prepareResult.total, 0, prepareResult.duplicateInFileCount, errors);
            }

            Set<String> dbDuplicates = loadExistingWordSet(prepareResult.validWords);
            List<SensitiveWord> toInsert = new ArrayList<>();
            for (String wordText : prepareResult.validWords) {
                if (dbDuplicates.contains(wordText)) {
                    continue;
                }

                SensitiveWord word = new SensitiveWord();
                word.setWord(wordText);
                word.setCategoryId(DEFAULT_CATEGORY_ID);
                word.setLevel(DEFAULT_LEVEL);
                word.setAction(DEFAULT_ACTION);
                word.setStatus(DEFAULT_STATUS);
                word.setCreatorId(creatorId);
                toInsert.add(word);
            }

            for (List<SensitiveWord> batch : splitList(toInsert, BATCH_SIZE)) {
                if (!batch.isEmpty()) {
                    success += sensitiveWordMapper.batchInsertWords(batch);
                }
            }

            if (success > 0) {
                sensitiveCheckService.refreshWordLibrary();
                long operatorId = creatorId == null ? DEFAULT_OPERATOR_ID : creatorId;
                recordVersionSafely(
                        "import",
                        success,
                        JSONUtil.toJsonStr(Map.of(
                                "total", prepareResult.total,
                                "success", success,
                                "duplicateInDb", dbDuplicates.size(),
                                "duplicateInFile", prepareResult.duplicateInFileCount,
                                "invalid", prepareResult.invalidCount
                        )),
                        operatorId,
                        "批量导入敏感词"
                );
            }

            int duplicate = prepareResult.duplicateInFileCount + dbDuplicates.size();
            if (prepareResult.invalidCount > 0) {
                errors.add("存在无效词条：" + prepareResult.invalidCount + " 条");
            }
            return new ImportResult(prepareResult.total, success, duplicate, errors);
        } catch (Exception e) {
            log.error("批量导入敏感词失败：{}", e.getMessage(), e);
            errors.add("导入过程中发生异常：" + e.getMessage());
            int duplicate = prepareResult.duplicateInFileCount;
            return new ImportResult(prepareResult.total, success, duplicate, errors);
        }
    }

    @Override
    public ExportResult exportWords(SensitiveWordQuery query) {
        List<SensitiveWordDTO> words = sensitiveWordMapper.selectWordList(query);
        StringBuilder csv = new StringBuilder();
        // UTF-8 BOM，兼容 Excel 打开中文乱码
        csv.append('\uFEFF');
        csv.append("ID,敏感词,分类,风险等级,处理动作,状态,创建人,创建时间\n");

        for (SensitiveWordDTO item : words) {
            csv.append(item.getId() == null ? "" : item.getId()).append(',')
                    .append(escapeCsv(item.getWord())).append(',')
                    .append(escapeCsv(item.getCategoryName())).append(',')
                    .append(escapeCsv(getLevelText(item.getLevel()))).append(',')
                    .append(escapeCsv(getActionText(item.getAction()))).append(',')
                    .append(escapeCsv(getStatusText(item.getStatus()))).append(',')
                    .append(escapeCsv(item.getCreatorName())).append(',')
                    .append(formatDateTime(item.getCreateTime()))
                    .append('\n');
        }

        String fileName = "sensitive_words_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                ".csv";
        return new ExportResult(words.size(), fileName, csv.toString());
    }

    @Override
    public List<SensitiveCategory> listCategories() {
        return sensitiveCategoryMapper.selectAllCategories();
    }

    private ImportPrepareResult prepareImportWords(List<String> words) {
        int total = words == null ? 0 : words.size();
        if (words == null || words.isEmpty()) {
            return new ImportPrepareResult(total, List.of(), 0, 0, List.of(), List.of());
        }

        Set<String> uniqueWords = new LinkedHashSet<>();
        List<String> validWords = new ArrayList<>();
        List<String> invalidSamples = new ArrayList<>();
        List<String> duplicateInFileSamples = new ArrayList<>();
        int invalidCount = 0;
        int duplicateInFileCount = 0;

        for (String rawWord : words) {
            String normalizedWord = normalizeWord(rawWord);
            if (StrUtil.isBlank(normalizedWord) || normalizedWord.length() > 255) {
                invalidCount++;
                if (invalidSamples.size() < SAMPLE_SIZE && StrUtil.isNotBlank(rawWord)) {
                    invalidSamples.add(rawWord.trim());
                }
                continue;
            }

            if (!uniqueWords.add(normalizedWord)) {
                duplicateInFileCount++;
                if (duplicateInFileSamples.size() < SAMPLE_SIZE) {
                    duplicateInFileSamples.add(normalizedWord);
                }
                continue;
            }
            validWords.add(normalizedWord);
        }

        return new ImportPrepareResult(
                total,
                validWords,
                invalidCount,
                duplicateInFileCount,
                invalidSamples,
                duplicateInFileSamples
        );
    }

    private Set<String> loadExistingWordSet(List<String> words) {
        Set<String> result = new LinkedHashSet<>();
        for (List<String> batch : splitList(words, BATCH_SIZE)) {
            if (batch.isEmpty()) {
                continue;
            }
            List<SensitiveWord> existingWords = sensitiveWordMapper.selectWordsByWords(batch);
            if (existingWords == null || existingWords.isEmpty()) {
                continue;
            }
            for (SensitiveWord existing : existingWords) {
                result.add(existing.getWord());
            }
        }
        return result;
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

    private List<String> sample(List<String> source, int maxSize) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        int end = Math.min(maxSize, source.size());
        return new ArrayList<>(source.subList(0, end));
    }

    private String normalizeWord(String word) {
        if (word == null) {
            return null;
        }
        String normalized = word.trim().replace('\u3000', ' ').replace("\t", " ");
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        return normalized.trim();
    }

    private void recordVersionSafely(String changeType, int changeCount, String changeDetail, Long operatorId, String remark) {
        try {
            sensitiveVersionService.createVersion(
                    changeType,
                    changeCount,
                    changeDetail,
                    operatorId == null ? DEFAULT_OPERATOR_ID : operatorId,
                    remark
            );
        } catch (Exception e) {
            log.warn("记录敏感词版本失败: changeType={}, reason={}", changeType, e.getMessage());
        }
    }

    private String getLevelText(Integer level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case 1 -> "低风险";
            case 2 -> "中风险";
            case 3 -> "高风险";
            default -> "未知";
        };
    }

    private String getActionText(Integer action) {
        if (action == null) {
            return "";
        }
        return switch (action) {
            case 1 -> "替换";
            case 2 -> "拒绝";
            case 3 -> "审核";
            default -> "未知";
        };
    }

    private String getStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return status == 1 ? "启用" : "禁用";
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static class ImportPrepareResult {
        private final int total;
        private final List<String> validWords;
        private final int invalidCount;
        private final int duplicateInFileCount;
        private final List<String> invalidSamples;
        private final List<String> duplicateInFileSamples;

        private ImportPrepareResult(int total, List<String> validWords, int invalidCount,
                                    int duplicateInFileCount, List<String> invalidSamples,
                                    List<String> duplicateInFileSamples) {
            this.total = total;
            this.validWords = validWords;
            this.invalidCount = invalidCount;
            this.duplicateInFileCount = duplicateInFileCount;
            this.invalidSamples = invalidSamples;
            this.duplicateInFileSamples = duplicateInFileSamples;
        }
    }
}
