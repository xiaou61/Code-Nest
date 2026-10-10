package io.github.xiaou61.knowledge.internal.web;

import io.github.xiaou61.knowledge.internal.admin.CategoryAdminService;
import io.github.xiaou61.knowledge.internal.admin.EntryAdminService;
import io.github.xiaou61.knowledge.internal.admin.RelationAdminService;
import io.github.xiaou61.knowledge.internal.category.CategoryService;
import io.github.xiaou61.knowledge.internal.category.CategoryTree;
import io.github.xiaou61.platform.ApiResponse;
import io.github.xiaou61.platform.PageQuery;
import io.github.xiaou61.platform.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库的管理端写端点。
 *
 * <p><b>整个 {@code /api/v1/knowledge/admin/**} 前缀由 {@code SecurityConfiguration} 的
 * 一条 {@code hasRole('ADMIN')} 规则保护</b>，所以这里不需要逐方法加权限注解——
 * 而且路径规则默认覆盖将来新增的接口，方法注解的失败模式是"漏加一个就多一个越权写入点"。
 *
 * <p><b>绝不接受客户端传入的用户标识</b>：本控制器的所有操作都不需要用户 id，
 * 唯一用到身份的地方是日志里的操作者（取自 {@code CurrentUser}）。
 */
@RestController
@RequestMapping("/api/v1/knowledge/admin")
class KnowledgeAdminController {

    private final CategoryService categoryService;
    private final CategoryAdminService categoryAdminService;
    private final EntryAdminService entryAdminService;
    private final RelationAdminService relationAdminService;

    KnowledgeAdminController(CategoryService categoryService,
                             CategoryAdminService categoryAdminService,
                             EntryAdminService entryAdminService,
                             RelationAdminService relationAdminService) {
        this.categoryService = categoryService;
        this.categoryAdminService = categoryAdminService;
        this.entryAdminService = entryAdminService;
        this.relationAdminService = relationAdminService;
    }

    @GetMapping("/categories")
    ApiResponse<List<CategoryTree.Node>> categories() {
        return ApiResponse.ok(categoryService.tree());
    }

    @PostMapping("/categories")
    ApiResponse<Long> createCategory(@Valid @RequestBody CategoryRequest body) {
        return ApiResponse.ok(categoryAdminService
                .create(body.parentId(), body.name(), body.slug(), body.sortOrder())
                .getId());
    }

    @PutMapping("/categories/{id}")
    ApiResponse<Void> updateCategory(@PathVariable("id") long id, @Valid @RequestBody CategoryRequest body) {
        categoryAdminService.update(id, body.parentId(), body.name(), body.slug(), body.sortOrder());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/categories/{id}")
    ApiResponse<Void> deleteCategory(@PathVariable("id") long id) {
        categoryAdminService.delete(id);
        return ApiResponse.ok(null);
    }

    /** 条目列表，**含草稿**；{@code status} 可选，传 draft 或 published 时只看那一种。 */
    @GetMapping("/entries")
    ApiResponse<PageResult<EntryAdminService.AdminEntrySummary>> entries(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(entryAdminService.page(status, PageQuery.of(page, size, null)));
    }

    /** 条目详情，**含草稿**——草稿预览用它，因此返回的是一份不过滤状态的数据。 */
    @GetMapping("/entries/{id}")
    ApiResponse<EntryAdminService.AdminEntryDetail> entry(@PathVariable("id") long id) {
        return ApiResponse.ok(entryAdminService.detail(id));
    }

    @PostMapping("/entries")
    ApiResponse<Long> createEntry(@Valid @RequestBody EntryRequest body) {
        return ApiResponse.ok(entryAdminService.create(
                body.title(), body.body(), body.categoryId(), body.status()));
    }

    /** 更新条目，含状态。**发布就是一次把 status 置为 published 的 PUT**，没有单独的发布端点。 */
    @PutMapping("/entries/{id}")
    ApiResponse<Void> updateEntry(@PathVariable("id") long id, @Valid @RequestBody EntryRequest body) {
        entryAdminService.update(id, body.title(), body.body(), body.categoryId(), body.status());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/entries/{id}")
    ApiResponse<Void> deleteEntry(@PathVariable("id") long id) {
        entryAdminService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/relations")
    ApiResponse<Long> createRelation(@Valid @RequestBody RelationRequest body) {
        return ApiResponse.ok(relationAdminService.create(
                body.fromEntryId(), body.toEntryId(), body.relationType()));
    }

    @DeleteMapping("/relations/{id}")
    ApiResponse<Void> deleteRelation(@PathVariable("id") long id) {
        relationAdminService.delete(id);
        return ApiResponse.ok(null);
    }

    record CategoryRequest(
            Long parentId,
            @NotBlank @Size(max = 64) String name,
            @NotBlank @Size(max = 64) String slug,
            Integer sortOrder) {
    }

    record EntryRequest(
            @NotBlank @Size(max = 200) String title,
            String body,
            Long categoryId,
            @NotBlank String status) {
    }

    record RelationRequest(Long fromEntryId, Long toEntryId, @NotBlank String relationType) {
    }
}
