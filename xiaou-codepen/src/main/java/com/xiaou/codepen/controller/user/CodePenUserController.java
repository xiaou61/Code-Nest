package com.xiaou.codepen.controller.user;

import com.xiaou.codepen.domain.CodePen;
import com.xiaou.codepen.domain.CodePenComment;
import com.xiaou.codepen.domain.CodePenFolder;
import com.xiaou.codepen.domain.CodePenTag;
import com.xiaou.codepen.dto.*;
import com.xiaou.codepen.service.CodePenCommentService;
import com.xiaou.codepen.service.CodePenFolderService;
import com.xiaou.codepen.service.CodePenService;
import com.xiaou.codepen.service.CodePenTagService;
import com.xiaou.common.annotation.Log;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码共享器用户端控制器
 *
 * @author xiaou
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user/code-pen")
@RequiredArgsConstructor
public class CodePenUserController {

    private final CodePenService codePenService;
    private final CodePenCommentService commentService;
    private final CodePenTagService tagService;
    private final CodePenFolderService folderService;

    // ========== 作品管理相关 ==========

    /**
     * 创建/保存作品（首次发布奖励10积分）
     */
    @Log(module = "代码共享器", type = Log.OperationType.INSERT, description = "创建作品")
    @PostMapping("/create")
    public Result<CodePenCreateResponse> create(@Valid @RequestBody CodePenCreateRequest request) {
        CodePenCreateResponse response = codePenService.createOrSave(request, currentUserIdRequired());
        return Result.success(response);
    }

    /**
     * 保存作品
     */
    @Log(module = "代码共享器", type = Log.OperationType.UPDATE, description = "保存作品")
    @PostMapping("/save")
    public Result<CodePenCreateResponse> save(@Valid @RequestBody CodePenCreateRequest request) {
        CodePenCreateResponse response = codePenService.createOrSave(request, currentUserIdRequired());
        return Result.success(response);
    }

    /**
     * 更新作品
     */
    @Log(module = "代码共享器", type = Log.OperationType.UPDATE, description = "更新作品")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody CodePenCreateRequest request) {
        boolean result = codePenService.update(request, currentUserIdRequired());
        return Result.success(result);
    }

    /**
     * 删除作品
     */
    @Log(module = "代码共享器", type = Log.OperationType.DELETE, description = "删除作品")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@Positive(message = "作品ID必须为正数") @PathVariable Long id) {
        boolean result = codePenService.delete(id, currentUserIdRequired());
        return Result.success(result);
    }

    /**
     * Fork作品（免费或付费）
     */
    @Log(module = "代码共享器", type = Log.OperationType.INSERT, description = "Fork作品")
    @PostMapping("/fork")
    public Result<ForkResponse> fork(@Valid @RequestBody ForkRequest request) {
        ForkResponse response = codePenService.forkPen(request, currentUserIdRequired());
        return Result.success(response);
    }

    /**
     * 获取作品详情
     */
    @GetMapping("/{id}")
    public Result<CodePenDetailResponse> getDetail(@Positive(message = "作品ID必须为正数") @PathVariable Long id) {
        CodePenDetailResponse response = codePenService.getDetail(id, currentUserIdOptional());
        return Result.success(response);
    }

    /**
     * 我的作品列表
     */
    @PostMapping("/my-list")
    public Result<List<CodePenDetailResponse>> getMyList(@RequestBody(required = false) CodePenListRequest request) {
        Integer status = request != null ? request.getStatus() : null;
        List<CodePenDetailResponse> list = codePenService.getMyList(currentUserIdRequired(), status);
        return Result.success(list);
    }

    /**
     * 我的草稿列表
     */
    @PostMapping("/draft-list")
    public Result<List<CodePenDetailResponse>> getDraftList() {
        List<CodePenDetailResponse> list = codePenService.getMyList(currentUserIdRequired(), 0);
        return Result.success(list);
    }

    /**
     * 检查Fork价格和用户积分
     */
    @PostMapping("/check-fork-price")
    public Result<CheckForkPriceResponse> checkForkPrice(@Valid @RequestBody CheckForkPriceRequest request) {
        CheckForkPriceResponse response = codePenService.checkForkPrice(request.getPenId(), currentUserIdRequired());
        return Result.success(response);
    }

    /**
     * 查看收益统计
     */
    @PostMapping("/income-stats")
    public Result<IncomeStatsResponse> getIncomeStats() {
        IncomeStatsResponse response = codePenService.getIncomeStats(currentUserIdRequired());
        return Result.success(response);
    }

    // ========== 作品广场相关 ==========

    /**
     * 获取作品列表（代码广场）
     */
    @PostMapping("/list")
    public Result<PageResult<CodePenDetailResponse>> getList(@Valid @RequestBody CodePenListRequest request) {
        PageResult<CodePenDetailResponse> pageResult = codePenService.getList(request, currentUserIdOptional());
        return Result.success(pageResult);
    }

    /**
     * 获取推荐作品列表
     */
    @PostMapping("/recommend-list")
    public Result<List<CodePenDetailResponse>> getRecommendList() {
        return Result.success(codePenService.getRecommendList(10));
    }

    /**
     * 获取热门作品
     */
    @GetMapping("/hot")
    public Result<List<CodePenDetailResponse>> getHotList() {
        return Result.success(codePenService.getHotList(10));
    }

    /**
     * 搜索作品
     */
    @PostMapping("/search")
    public Result<PageResult<CodePenDetailResponse>> search(@Valid @RequestBody CodePenListRequest request) {
        PageResult<CodePenDetailResponse> pageResult = codePenService.getList(request, currentUserIdOptional());
        return Result.success(pageResult);
    }

    /**
     * 按标签获取作品
     */
    @PostMapping("/by-tag")
    public Result<PageResult<CodePenDetailResponse>> getByTag(@Valid @RequestBody CodePenListRequest request) {
        PageResult<CodePenDetailResponse> pageResult = codePenService.getList(request, currentUserIdOptional());
        return Result.success(pageResult);
    }

    /**
     * 按分类获取作品
     */
    @PostMapping("/by-category")
    public Result<PageResult<CodePenDetailResponse>> getByCategory(@Valid @RequestBody CodePenListRequest request) {
        PageResult<CodePenDetailResponse> pageResult = codePenService.getList(request, currentUserIdOptional());
        return Result.success(pageResult);
    }

    /**
     * 获取指定用户的作品
     */
    @PostMapping("/by-user")
    public Result<PageResult<CodePenDetailResponse>> getByUser(@Valid @RequestBody CodePenListRequest request) {
        PageResult<CodePenDetailResponse> pageResult = codePenService.getList(request, currentUserIdOptional());
        return Result.success(pageResult);
    }

    // ========== 互动功能相关 ==========

    /**
     * 点赞作品
     */
    @Log(module = "代码共享器", type = Log.OperationType.INSERT, description = "点赞作品")
    @PostMapping("/like")
    public Result<Boolean> like(@Valid @RequestBody CheckForkPriceRequest request) {
        boolean result = codePenService.like(request.getPenId(), currentUserIdRequired());
        return Result.success(result);
    }

    /**
     * 取消点赞
     */
    @Log(module = "代码共享器", type = Log.OperationType.DELETE, description = "取消点赞")
    @PostMapping("/unlike")
    public Result<Boolean> unlike(@Valid @RequestBody CheckForkPriceRequest request) {
        boolean result = codePenService.unlike(request.getPenId(), currentUserIdRequired());
        return Result.success(result);
    }

    /**
     * 收藏作品
     */
    @Log(module = "代码共享器", type = Log.OperationType.INSERT, description = "收藏作品")
    @PostMapping("/collect")
    public Result<Boolean> collect(@Valid @RequestBody CheckForkPriceRequest request) {
        boolean result = codePenService.collect(request.getPenId(), currentUserIdRequired(), null);
        return Result.success(result);
    }

    /**
     * 取消收藏
     */
    @Log(module = "代码共享器", type = Log.OperationType.DELETE, description = "取消收藏")
    @PostMapping("/uncollect")
    public Result<Boolean> uncollect(@Valid @RequestBody CheckForkPriceRequest request) {
        boolean result = codePenService.uncollect(request.getPenId(), currentUserIdRequired());
        return Result.success(result);
    }

    /**
     * 增加浏览数
     */
    @PostMapping("/view")
    public Result<Boolean> incrementView(@Valid @RequestBody CheckForkPriceRequest request) {
        boolean result = codePenService.incrementView(request.getPenId());
        return Result.success(result);
    }

    /**
     * 发表评论
     */
    @Log(module = "代码共享器", type = Log.OperationType.INSERT, description = "发表评论")
    @PostMapping("/comment")
    public Result<Long> createComment(@Valid @RequestBody CommentCreateRequest request) {
        Long commentId = commentService.createComment(request, currentUserIdRequired());
        return Result.success(commentId);
    }

    /**
     * 删除评论
     */
    @Log(module = "代码共享器", type = Log.OperationType.DELETE, description = "删除评论")
    @DeleteMapping("/comment/{id}")
    public Result<Boolean> deleteComment(@Positive(message = "评论ID必须为正数") @PathVariable Long id) {
        boolean result = commentService.deleteComment(id, currentUserIdRequired());
        return Result.success(result);
    }

    /**
     * 获取评论列表
     */
    @PostMapping("/comment-list")
    public Result<List<CodePenComment>> getCommentList(@Valid @RequestBody CheckForkPriceRequest request) {
        return Result.success(commentService.getCommentList(request.getPenId()));
    }

    // ========== 收藏夹管理相关 ==========

    /**
     * 创建收藏夹
     */
    @Log(module = "代码共享器", type = Log.OperationType.INSERT, description = "创建收藏夹")
    @PostMapping("/folder/create")
    public Result<Long> createFolder(@RequestBody CodePenFolder folder) {
        Long folderId = folderService.createFolder(
                folder.getFolderName(),
                folder.getFolderDescription(),
                currentUserIdRequired()
        );
        return Result.success(folderId);
    }

    /**
     * 更新收藏夹
     */
    @Log(module = "代码共享器", type = Log.OperationType.UPDATE, description = "更新收藏夹")
    @PostMapping("/folder/update")
    public Result<Boolean> updateFolder(@RequestBody CodePenFolder folder) {
        boolean result = folderService.updateFolder(
                folder.getId(),
                folder.getFolderName(),
                folder.getFolderDescription(),
                currentUserIdRequired()
        );
        return Result.success(result);
    }

    /**
     * 删除收藏夹
     */
    @Log(module = "代码共享器", type = Log.OperationType.DELETE, description = "删除收藏夹")
    @DeleteMapping("/folder/{id}")
    public Result<Boolean> deleteFolder(@Positive(message = "收藏夹ID必须为正数") @PathVariable Long id) {
        boolean result = folderService.deleteFolder(id, currentUserIdRequired());
        return Result.success(result);
    }

    /**
     * 我的收藏夹列表
     */
    @PostMapping("/folder/list")
    public Result<List<CodePenFolder>> getFolderList() {
        return Result.success(folderService.getFolderList(currentUserIdRequired()));
    }

    /**
     * 收藏夹内容列表
     */
    @PostMapping("/folder/items")
    public Result<List<CodePenDetailResponse>> getFolderItems(@RequestBody CodePenFolder folder) {
        Long userId = currentUserIdRequired();
        List<Long> penIds = folderService.getFolderItems(folder.getId(), userId);
        List<CodePenDetailResponse> responses = new ArrayList<>();

        for (Long penId : penIds) {
            try {
                CodePenDetailResponse detail = codePenService.getDetail(penId, userId);
                responses.add(detail);
            } catch (Exception e) {
                log.warn("获取作品{}详情失败", penId, e);
            }
        }

        return Result.success(responses);
    }

    // ========== 模板管理相关 ==========

    /**
     * 获取系统模板列表
     */
    @GetMapping("/templates")
    public Result<List<CodePen>> getTemplates() {
        return Result.success(codePenService.getTemplateList());
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/template/{id}")
    public Result<CodePenDetailResponse> getTemplateDetail(@Positive(message = "模板ID必须为正数") @PathVariable Long id) {
        CodePenDetailResponse response = codePenService.getDetail(id, currentUserIdOptional());
        return Result.success(response);
    }

    // ========== 标签管理相关 ==========

    /**
     * 获取所有标签
     */
    @GetMapping("/tags")
    public Result<List<CodePenTag>> getTags() {
        return Result.success(tagService.getAllTags());
    }

    /**
     * 获取热门标签
     */
    @GetMapping("/tags/hot")
    public Result<List<CodePenTag>> getHotTags() {
        return Result.success(tagService.getHotTags(20));
    }

    private Long currentUserIdRequired() {
        StpUserUtil.checkLogin();
        return StpUserUtil.getLoginIdAsLong();
    }

    private Long currentUserIdOptional() {
        return StpUserUtil.isLogin() ? StpUserUtil.getLoginIdAsLong() : null;
    }
}
