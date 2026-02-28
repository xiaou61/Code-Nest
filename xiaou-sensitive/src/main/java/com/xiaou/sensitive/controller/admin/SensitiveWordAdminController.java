package com.xiaou.sensitive.controller.admin;

import cn.hutool.core.util.StrUtil;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.sensitive.api.SensitiveCheckService;
import com.xiaou.sensitive.domain.SensitiveCategory;
import com.xiaou.sensitive.domain.SensitiveWord;
import com.xiaou.sensitive.dto.SensitiveWordDTO;
import com.xiaou.sensitive.dto.SensitiveWordQuery;

import com.xiaou.sensitive.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 敏感词管理后台控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/sensitive")
@RequiredArgsConstructor
public class SensitiveWordAdminController {

    private final SensitiveWordService sensitiveWordService;
    private final SensitiveCheckService sensitiveCheckService;

    /**
     * 分页查询敏感词列表
     */
    @PostMapping("/words/list")
    @RequireAdmin
    public Result<PageResult<SensitiveWordDTO>> listWords(@RequestBody SensitiveWordQuery query) {
        try {
            PageResult<SensitiveWordDTO> response = sensitiveWordService.listWords(query);
            return Result.success(response);
        } catch (Exception e) {
            log.error("查询敏感词列表失败：{}", e.getMessage(), e);
            return Result.error("查询敏感词列表失败");
        }
    }

    /**
     * 根据ID查询敏感词
     */
    @PostMapping("/words/{id}")
    @RequireAdmin
    public Result<SensitiveWord> getWordById(@PathVariable Long id) {
        try {
            SensitiveWord word = sensitiveWordService.getWordById(id);
            if (word == null) {
                return Result.error("敏感词不存在");
            }
            return Result.success(word);
        } catch (Exception e) {
            log.error("查询敏感词失败：{}", e.getMessage(), e);
            return Result.error("查询敏感词失败");
        }
    }

    /**
     * 新增敏感词
     */
    @PostMapping("/words")
    @RequireAdmin
    public Result<Void> addWord(@RequestBody SensitiveWord word) {
        try {
            if (StrUtil.isBlank(word.getWord())) {
                return Result.error("敏感词不能为空");
            }

            // 设置创建人
            word.setCreatorId(StpAdminUtil.getLoginIdAsLong());

            boolean success = sensitiveWordService.addWord(word);
            if (success) {
                return Result.success();
            } else {
                return Result.error("敏感词已存在或添加失败");
            }
        } catch (Exception e) {
            log.error("新增敏感词失败：{}", e.getMessage(), e);
            return Result.error("新增敏感词失败");
        }
    }

    /**
     * 更新敏感词
     */
    @PostMapping("/words/update")
    @RequireAdmin
    public Result<Void> updateWord(@RequestBody SensitiveWord word) {
        try {
            if (word.getId() == null) {
                return Result.error("敏感词ID不能为空");
            }
            word.setCreatorId(StpAdminUtil.getLoginIdAsLong());

            boolean success = sensitiveWordService.updateWord(word);
            if (success) {
                return Result.success();
            } else {
                return Result.error("更新敏感词失败");
            }
        } catch (Exception e) {
            log.error("更新敏感词失败：{}", e.getMessage(), e);
            return Result.error("更新敏感词失败");
        }
    }

    /**
     * 删除敏感词
     */
    @PostMapping("/words/delete/{id}")
    @RequireAdmin
    public Result<Void> deleteWord(@PathVariable Long id) {
        try {
            boolean success = sensitiveWordService.deleteWord(id);
            if (success) {
                return Result.success();
            } else {
                return Result.error("删除敏感词失败");
            }
        } catch (Exception e) {
            log.error("删除敏感词失败：{}", e.getMessage(), e);
            return Result.error("删除敏感词失败");
        }
    }

    /**
     * 批量删除敏感词
     */
    @PostMapping("/words/delete/batch")
    @RequireAdmin
    public Result<Void> deleteWords(@RequestBody List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的敏感词");
            }

            boolean success = sensitiveWordService.deleteWords(ids);
            if (success) {
                return Result.success();
            } else {
                return Result.error("批量删除敏感词失败");
            }
        } catch (Exception e) {
            log.error("批量删除敏感词失败：{}", e.getMessage(), e);
            return Result.error("批量删除敏感词失败");
        }
    }

    /**
     * 批量导入敏感词
     */
    @PostMapping("/words/import")
    @RequireAdmin
    public Result<SensitiveWordService.ImportResult> importWords(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }
            List<String> words = parseWordsFromFile(file);

            if (words.isEmpty()) {
                return Result.error("文件内容为空");
            }

            Long creatorId = StpAdminUtil.getLoginIdAsLong();
            SensitiveWordService.ImportResult result = sensitiveWordService.importWords(words, creatorId);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("导入敏感词失败：{}", e.getMessage(), e);
            return Result.error("导入敏感词失败：" + e.getMessage());
        }
    }

    /**
     * 导入预览
     */
    @PostMapping("/words/preview-import")
    @RequireAdmin
    public Result<SensitiveWordService.ImportPreviewResult> previewImportWords(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }
            List<String> words = parseWordsFromFile(file);
            if (words.isEmpty()) {
                return Result.error("文件内容为空");
            }
            return Result.success(sensitiveWordService.previewImportWords(words));
        } catch (Exception e) {
            log.error("预览导入敏感词失败：{}", e.getMessage(), e);
            return Result.error("预览导入失败：" + e.getMessage());
        }
    }

    /**
     * 确认导入（两步导入流程）
     */
    @PostMapping("/words/confirm-import")
    @RequireAdmin
    public Result<SensitiveWordService.ImportResult> confirmImportWords(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }
            List<String> words = parseWordsFromFile(file);
            if (words.isEmpty()) {
                return Result.error("文件内容为空");
            }
            Long creatorId = StpAdminUtil.getLoginIdAsLong();
            SensitiveWordService.ImportResult result = sensitiveWordService.importWords(words, creatorId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("确认导入敏感词失败：{}", e.getMessage(), e);
            return Result.error("确认导入失败：" + e.getMessage());
        }
    }

    /**
     * 导出敏感词（CSV）
     */
    @PostMapping("/words/export")
    @RequireAdmin
    public Result<SensitiveWordService.ExportResult> exportWords(@RequestBody(required = false) SensitiveWordQuery query) {
        try {
            SensitiveWordQuery exportQuery = query == null ? new SensitiveWordQuery() : query;
            SensitiveWordService.ExportResult result = sensitiveWordService.exportWords(exportQuery);
            return Result.success(result);
        } catch (Exception e) {
            log.error("导出敏感词失败：{}", e.getMessage(), e);
            return Result.error("导出敏感词失败");
        }
    }

    /**
     * 刷新敏感词库
     */
    @PostMapping("/refresh")
    @RequireAdmin
    public Result<Void> refreshWordLibrary() {
        try {
            sensitiveCheckService.refreshWordLibrary();
            return Result.success();
        } catch (Exception e) {
            log.error("刷新敏感词库失败：{}", e.getMessage(), e);
            return Result.error("刷新敏感词库失败");
        }
    }

    /**
     * 查询敏感词分类列表
     */
    @PostMapping("/categories")
    @RequireAdmin
    public Result<List<SensitiveCategory>> listCategories() {
        try {
            List<SensitiveCategory> categories = sensitiveWordService.listCategories();
            return Result.success(categories);
        } catch (Exception e) {
            log.error("查询敏感词分类失败：{}", e.getMessage(), e);
            return Result.error("查询敏感词分类失败");
        }
    }

    private List<String> parseWordsFromFile(MultipartFile file) throws Exception {
        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimLine = StrUtil.trim(line);
                if (StrUtil.isBlank(trimLine) || trimLine.startsWith("#") || trimLine.startsWith("//")) {
                    continue;
                }
                String normalizedLine = trimLine
                        .replace("，", ",")
                        .replace("；", ";")
                        .replace("|", ",");
                String[] parts = normalizedLine.split("[,;]");
                words.addAll(Arrays.stream(parts).map(String::trim).toList());
            }
        }
        return words;
    }
}
