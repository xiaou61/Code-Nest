package com.xiaou.resume.controller.admin;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.resume.domain.ResumeTemplate;
import com.xiaou.resume.dto.ResumeTemplateRequest;
import com.xiaou.resume.dto.TemplateQueryRequest;
import com.xiaou.resume.service.ResumeTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端模板接口
 */
@Tag(name = "管理端模板", description = "模板维护")
@RestController
@RequestMapping("/admin/resume/templates")
@RequiredArgsConstructor
public class ResumeTemplateAdminController {

    private final ResumeTemplateService resumeTemplateService;

    @Operation(summary = "分页查询模板")
    @GetMapping
    @RequireAdmin
    public Result<PageResult<ResumeTemplate>> list(@Valid TemplateQueryRequest request) {
        return Result.success(resumeTemplateService.listTemplates(request));
    }

    @Operation(summary = "创建模板")
    @PostMapping
    @RequireAdmin
    public Result<Long> create(@Valid @RequestBody ResumeTemplateRequest request) {
        return Result.success(resumeTemplateService.createTemplate(StpAdminUtil.getLoginIdAsLong(), request));
    }

    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    @RequireAdmin
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ResumeTemplateRequest request) {
        resumeTemplateService.updateTemplate(StpAdminUtil.getLoginIdAsLong(), id, request);
        return Result.success();
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    @RequireAdmin
    public Result<Void> delete(@PathVariable Long id) {
        resumeTemplateService.deleteTemplate(id);
        return Result.success();
    }

    @Operation(summary = "模板详情")
    @GetMapping("/{id}")
    @RequireAdmin
    public Result<ResumeTemplate> detail(@PathVariable Long id) {
        return Result.success(resumeTemplateService.getTemplate(id));
    }
}
