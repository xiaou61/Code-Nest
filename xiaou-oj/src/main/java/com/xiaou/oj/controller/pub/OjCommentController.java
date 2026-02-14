package com.xiaou.oj.controller.pub;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.oj.dto.OjCommentCreateRequest;
import com.xiaou.oj.dto.OjCommentQueryRequest;
import com.xiaou.oj.dto.OjCommentReplyRequest;
import com.xiaou.oj.dto.OjCommentResponse;
import com.xiaou.oj.service.OjProblemCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OJ题目评论接口
 *
 * @author xiaou
 */
@Tag(name = "OJ评论", description = "OJ题目评论功能")
@RestController
@RequestMapping("/oj")
@RequiredArgsConstructor
public class OjCommentController {

    private final OjProblemCommentService commentService;

    @Operation(summary = "查询题目评论列表")
    @PostMapping("/problems/{problemId}/comments")
    public Result<PageResult<OjCommentResponse>> getComments(
            @PathVariable Long problemId,
            @RequestBody OjCommentQueryRequest request) {
        return Result.success(commentService.getComments(problemId, request));
    }

    @Operation(summary = "发表评论")
    @PostMapping("/problems/{problemId}/comments/create")
    public Result<Void> createComment(
            @PathVariable Long problemId,
            @Valid @RequestBody OjCommentCreateRequest request) {
        commentService.createComment(problemId, request);
        return Result.success();
    }

    @Operation(summary = "回复评论")
    @PostMapping("/comments/{commentId}/reply")
    public Result<Void> replyComment(
            @PathVariable Long commentId,
            @Valid @RequestBody OjCommentReplyRequest request) {
        commentService.replyComment(commentId, request);
        return Result.success();
    }

    @Operation(summary = "点赞评论")
    @PostMapping("/comments/{commentId}/like")
    public Result<Void> likeComment(@PathVariable Long commentId) {
        commentService.likeComment(commentId);
        return Result.success();
    }

    @Operation(summary = "取消点赞评论")
    @DeleteMapping("/comments/{commentId}/like")
    public Result<Void> unlikeComment(@PathVariable Long commentId) {
        commentService.unlikeComment(commentId);
        return Result.success();
    }

    @Operation(summary = "查看评论的回复列表")
    @PostMapping("/comments/{commentId}/replies")
    public Result<PageResult<OjCommentResponse>> getCommentReplies(
            @PathVariable Long commentId,
            @RequestBody OjCommentQueryRequest request) {
        return Result.success(commentService.getCommentReplies(commentId, request));
    }
}
