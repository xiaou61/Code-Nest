package com.xiaou.sensitive.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.sensitive.domain.SensitiveCategory;
import com.xiaou.sensitive.domain.SensitiveWord;
import com.xiaou.sensitive.dto.SensitiveWordDTO;
import com.xiaou.sensitive.dto.SensitiveWordQuery;

import java.util.List;

/**
 * 敏感词管理服务接口
 */
public interface SensitiveWordService {

    /**
     * 分页查询敏感词列表
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<SensitiveWordDTO> listWords(SensitiveWordQuery query);

    /**
     * 根据ID查询敏感词
     * @param id 敏感词ID
     * @return 敏感词信息
     */
    SensitiveWord getWordById(Long id);

    /**
     * 新增敏感词
     * @param word 敏感词信息
     * @return 是否成功
     */
    boolean addWord(SensitiveWord word);

    /**
     * 更新敏感词
     * @param word 敏感词信息
     * @return 是否成功
     */
    boolean updateWord(SensitiveWord word);

    /**
     * 删除敏感词
     * @param id 敏感词ID
     * @return 是否成功
     */
    boolean deleteWord(Long id);

    /**
     * 批量删除敏感词
     * @param ids 敏感词ID列表
     * @return 是否成功
     */
    boolean deleteWords(List<Long> ids);

    /**
     * 批量导入敏感词
     * @param words 敏感词列表
     * @param creatorId 创建人ID
     * @return 导入结果
     */
    ImportResult importWords(List<String> words, Long creatorId);

    /**
     * 预览导入敏感词
     * @param words 待导入词列表
     * @return 预览结果
     */
    ImportPreviewResult previewImportWords(List<String> words);

    /**
     * 导出敏感词（CSV）
     * @param query 查询条件
     * @return 导出结果
     */
    ExportResult exportWords(SensitiveWordQuery query);

    /**
     * 查询所有分类
     * @return 分类列表
     */
    List<SensitiveCategory> listCategories();

    /**
     * 导入结果
     */
    class ImportResult {
        private int total;
        private int success;
        private int duplicate;
        private List<String> errors;

        public ImportResult(int total, int success, int duplicate, List<String> errors) {
            this.total = total;
            this.success = success;
            this.duplicate = duplicate;
            this.errors = errors;
        }

        // getters
        public int getTotal() { return total; }
        public int getSuccess() { return success; }
        public int getDuplicate() { return duplicate; }
        public List<String> getErrors() { return errors; }
    }

    /**
     * 导入预览结果
     */
    class ImportPreviewResult {
        private final int total;
        private final int validCount;
        private final int invalidCount;
        private final int duplicateInFileCount;
        private final int duplicateInDbCount;
        private final int importableCount;
        private final List<String> importableSamples;
        private final List<String> duplicateSamples;
        private final List<String> invalidSamples;

        public ImportPreviewResult(int total, int validCount, int invalidCount,
                                   int duplicateInFileCount, int duplicateInDbCount,
                                   int importableCount, List<String> importableSamples,
                                   List<String> duplicateSamples, List<String> invalidSamples) {
            this.total = total;
            this.validCount = validCount;
            this.invalidCount = invalidCount;
            this.duplicateInFileCount = duplicateInFileCount;
            this.duplicateInDbCount = duplicateInDbCount;
            this.importableCount = importableCount;
            this.importableSamples = importableSamples;
            this.duplicateSamples = duplicateSamples;
            this.invalidSamples = invalidSamples;
        }

        public int getTotal() {
            return total;
        }

        public int getValidCount() {
            return validCount;
        }

        public int getInvalidCount() {
            return invalidCount;
        }

        public int getDuplicateInFileCount() {
            return duplicateInFileCount;
        }

        public int getDuplicateInDbCount() {
            return duplicateInDbCount;
        }

        public int getImportableCount() {
            return importableCount;
        }

        public List<String> getImportableSamples() {
            return importableSamples;
        }

        public List<String> getDuplicateSamples() {
            return duplicateSamples;
        }

        public List<String> getInvalidSamples() {
            return invalidSamples;
        }
    }

    /**
     * 导出结果
     */
    class ExportResult {
        private final int total;
        private final String fileName;
        private final String content;

        public ExportResult(int total, String fileName, String content) {
            this.total = total;
            this.fileName = fileName;
            this.content = content;
        }

        public int getTotal() {
            return total;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContent() {
            return content;
        }
    }
}
