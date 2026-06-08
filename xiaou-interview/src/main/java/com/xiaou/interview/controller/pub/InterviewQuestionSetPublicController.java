package com.xiaou.interview.controller.pub;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.interview.domain.InterviewQuestion;
import com.xiaou.interview.domain.InterviewQuestionSet;
import com.xiaou.interview.dto.RandomQuestionRequest;
import com.xiaou.interview.service.InterviewQuestionService;
import com.xiaou.interview.service.InterviewQuestionSetService;
import com.xiaou.interview.dto.SearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 面试题单用户端接口
 *
 * @author xiaou
 */
@Tag(name = "面试题单用户端", description = "用户端面试题单相关接口")
@RestController
@RequestMapping("/interview/question-sets")
@RequiredArgsConstructor
public class InterviewQuestionSetPublicController {

    private final InterviewQuestionSetService questionSetService;
    private final InterviewQuestionService questionService;

    @Operation(summary = "获取公开题单列表")
    @GetMapping
    public Result<PageResult<InterviewQuestionSet>> getPublicQuestionSets(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<InterviewQuestionSet> result = questionSetService.getPublicQuestionSets(categoryId, page, size);
        return Result.success(result);
    }

    @Operation(summary = "获取题单详情")
    @GetMapping("/{id}")
    public Result<InterviewQuestionSet> getQuestionSet(@PathVariable Long id) {
        // 增加浏览次数
        questionSetService.increaseViewCount(id);
        
        InterviewQuestionSet questionSet = questionSetService.getQuestionSetById(id);
        return Result.success(questionSet);
    }

    @Operation(summary = "获取题单的题目列表")
    @GetMapping("/{id}/questions")
    public Result<List<InterviewQuestion>> getQuestions(@PathVariable Long id) {
        // 检查权限
        Long userId = StpUserUtil.getLoginIdAsLong();
        if (userId != null && !questionSetService.hasAccessPermission(id, userId)) {
            return Result.error(ResultCode.PERMISSION_DENIED.getCode(), "无权限访问该题单");
        }
        
        List<InterviewQuestion> questions = questionService.getQuestionsBySetId(id);
        return Result.success(questions);
    }

    @Operation(summary = "获取题目详情")
    @GetMapping("/{setId}/questions/{questionId}")
    public Result<InterviewQuestion> getQuestion(@PathVariable Long setId, 
                                                  @PathVariable Long questionId) {
        // 检查权限
        Long userId = StpUserUtil.getLoginIdAsLong();
        if (userId != null && !questionSetService.hasAccessPermission(setId, userId)) {
            return Result.error(ResultCode.PERMISSION_DENIED.getCode(), "无权限访问该题单");
        }
        
        // 增加浏览次数
        questionService.increaseViewCount(questionId);
        
        InterviewQuestion question = questionService.getQuestionById(questionId);
        return Result.success(question);
    }

    @Operation(summary = "获取下一题")
    @GetMapping("/{setId}/questions/{questionId}/next")
    public Result<InterviewQuestion> getNextQuestion(@PathVariable Long setId, 
                                                     @PathVariable Long questionId) {
        // 检查权限
        Long userId = StpUserUtil.getLoginIdAsLong();
        if (userId != null && !questionSetService.hasAccessPermission(setId, userId)) {
            return Result.error(ResultCode.PERMISSION_DENIED.getCode(), "无权限访问该题单");
        }
        
        // 先获取当前题目的排序号
        InterviewQuestion currentQuestion = questionService.getQuestionById(questionId);
        if (currentQuestion == null) {
            return Result.error("题目不存在");
        }
        
        InterviewQuestion nextQuestion = questionService.getNextQuestion(setId, currentQuestion.getSortOrder());
        return Result.success(nextQuestion);
    }

    @Operation(summary = "获取上一题")
    @GetMapping("/{setId}/questions/{questionId}/prev")
    public Result<InterviewQuestion> getPrevQuestion(@PathVariable Long setId, 
                                                     @PathVariable Long questionId) {
        // 检查权限
        Long userId = StpUserUtil.getLoginIdAsLong();
        if (userId != null && !questionSetService.hasAccessPermission(setId, userId)) {
            return Result.error(ResultCode.PERMISSION_DENIED.getCode(), "无权限访问该题单");
        }
        
        // 先获取当前题目的排序号
        InterviewQuestion currentQuestion = questionService.getQuestionById(questionId);
        if (currentQuestion == null) {
            return Result.error("题目不存在");
        }
        
        InterviewQuestion prevQuestion = questionService.getPrevQuestion(setId, currentQuestion.getSortOrder());
        return Result.success(prevQuestion);
    }



    @Operation(summary = "搜索题目")
    @PostMapping("/search")
    public Result<PageResult<InterviewQuestion>> searchQuestions(@RequestBody SearchRequest request) {
        PageResult<InterviewQuestion> result = questionService.searchQuestions(
            request.getKeyword(), request.getPage(), request.getSize());
        return Result.success(result);
    }

    @Operation(summary = "随机抽题")
    @PostMapping("/questions/random")
    public Result<List<InterviewQuestion>> getRandomQuestions(@RequestBody RandomQuestionRequest request) {
        List<InterviewQuestion> questions = questionService.getRandomQuestions(request);
        return Result.success(questions);
    }
} 
