package com.xiaou.moyu.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.JsonUtils;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.moyu.domain.DailyContent;
import com.xiaou.moyu.dto.DailyContentDto;
import com.xiaou.moyu.service.DailyContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 每日内容控制器
 *
 * @author xiaou
 */
@Slf4j
@RestController
@RequestMapping("/moyu/daily-content")
@RequiredArgsConstructor
public class DailyContentController {
    
    private final DailyContentService dailyContentService;
    
    /**
     * 获取今日内容推荐
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "获取今日内容推荐")
    @GetMapping("/today")
    public Result<List<DailyContentDto>> getTodayContent() {
        Long userId = getCurrentUserId();
        
        List<DailyContent> contents = dailyContentService.getTodayContent(userId);
        List<DailyContentDto> contentDtos = convertToContentDtos(contents, userId);
        
        return Result.success(contentDtos);
    }
    
    /**
     * 根据内容类型获取内容列表
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "根据类型获取内容")
    @GetMapping("/type/{contentType}")
    public Result<List<DailyContentDto>> getContentByType(@PathVariable Integer contentType,
                                                          @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = getCurrentUserId();
        
        List<DailyContent> contents = dailyContentService.getContentByType(contentType, limit);
        List<DailyContentDto> contentDtos = convertToContentDtos(contents, userId);
        
        return Result.success(contentDtos);
    }
    
    /**
     * 随机获取指定类型的内容
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "随机获取内容")
    @GetMapping("/random/{contentType}")
    public Result<List<DailyContentDto>> getRandomContentByType(@PathVariable Integer contentType,
                                                                @RequestParam(defaultValue = "5") Integer limit) {
        Long userId = getCurrentUserId();
        
        List<DailyContent> contents = dailyContentService.getRandomContentByType(contentType, limit);
        List<DailyContentDto> contentDtos = convertToContentDtos(contents, userId);
        
        return Result.success(contentDtos);
    }
    
    /**
     * 获取推荐内容
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "获取推荐内容")
    @GetMapping("/recommend")
    public Result<List<DailyContentDto>> getRecommendedContent(@RequestParam(defaultValue = "10") Integer limit) {
        Long userId = getCurrentUserId();
        
        List<DailyContent> contents = dailyContentService.getRecommendedContent(userId, limit);
        List<DailyContentDto> contentDtos = convertToContentDtos(contents, userId);
        
        return Result.success(contentDtos);
    }
    
    /**
     * 获取热门内容
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "获取热门内容")
    @GetMapping("/popular")
    public Result<List<DailyContentDto>> getPopularContent(@RequestParam(defaultValue = "10") Integer limit) {
        Long userId = getCurrentUserId();
        
        List<DailyContent> contents = dailyContentService.getPopularContent(limit);
        List<DailyContentDto> contentDtos = convertToContentDtos(contents, userId);
        
        return Result.success(contentDtos);
    }
    
    /**
     * 根据编程语言获取内容
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "根据编程语言获取内容")
    @GetMapping("/language/{language}")
    public Result<List<DailyContentDto>> getContentByLanguage(@PathVariable String language,
                                                              @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = getCurrentUserId();
        
        List<DailyContent> contents = dailyContentService.getContentByLanguage(language, limit);
        List<DailyContentDto> contentDtos = convertToContentDtos(contents, userId);
        
        return Result.success(contentDtos);
    }

    /**
     * 获取内容详情
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "获取内容详情")
    @GetMapping("/{contentId}")
    public Result<DailyContentDto> getContentDetail(@PathVariable Long contentId) {
        Long userId = getCurrentUserId();

        DailyContent content = dailyContentService.getContentById(contentId);
        if (content == null || content.getStatus() == null || content.getStatus() != 1) {
            throw new BusinessException("内容不存在");
        }

        return Result.success(convertToContentDto(content, userId));
    }
    
    /**
     * 查看内容详情（增加查看次数）
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.UPDATE, description = "查看内容详情")
    @PostMapping("/{contentId}/view")
    public Result<Void> viewContent(@PathVariable Long contentId) {
        boolean success = dailyContentService.incrementViewCount(contentId);
        if (!success) {
            throw new BusinessException("操作失败");
        }
        
        return Result.success();
    }
    
    /**
     * 点赞内容
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.UPDATE, description = "点赞内容")
    @PostMapping("/{contentId}/like")
    public Result<Void> likeContent(@PathVariable Long contentId) {
        boolean success = dailyContentService.likeContent(contentId);
        if (!success) {
            throw new BusinessException("操作失败");
        }
        
        return Result.success();
    }
    
    /**
     * 切换内容收藏状态
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.UPDATE, description = "切换内容收藏状态")
    @PostMapping("/{contentId}/toggle-collection")
    public Result<Void> toggleContentCollection(@PathVariable Long contentId) {
        Long userId = getCurrentUserId();
        
        boolean success = dailyContentService.toggleContentCollection(userId, contentId);
        if (!success) {
            throw new BusinessException("操作失败");
        }
        
        return Result.success();
    }
    
    /**
     * 获取用户收藏的内容列表
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "获取收藏内容")
    @GetMapping("/collections")
    public Result<List<DailyContentDto>> getUserCollectedContent() {
        Long userId = getCurrentUserId();
        
        List<DailyContent> contents = dailyContentService.getUserCollectedContent(userId);
        List<DailyContentDto> contentDtos = convertToContentDtos(contents, userId);
        
        return Result.success(contentDtos);
    }
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        StpUserUtil.checkLogin();
        return StpUserUtil.getLoginIdAsLong();
    }
    
    /**
     * 转换为内容DTO列表
     */
    private List<DailyContentDto> convertToContentDtos(List<DailyContent> contents, Long userId) {
        return contents.stream()
                .map(content -> convertToContentDto(content, userId))
                .collect(Collectors.toList());
    }
    
    /**
     * 转换为内容DTO
     */
    private DailyContentDto convertToContentDto(DailyContent content, Long userId) {
        DailyContentDto dto = new DailyContentDto();
        dto.setId(content.getId());
        dto.setContentType(content.getContentType());
        dto.setContentTypeName(getContentTypeName(content.getContentType()));
        dto.setTitle(content.getTitle());
        dto.setContent(content.getContent());
        dto.setAuthor(content.getAuthor());
        dto.setProgrammingLanguage(content.getProgrammingLanguage());
        dto.setTags(JsonUtils.jsonToStringList(content.getTags()));
        dto.setDifficultyLevel(content.getDifficultyLevel());
        dto.setDifficultyLevelName(getDifficultyLevelName(content.getDifficultyLevel()));
        dto.setSourceUrl(content.getSourceUrl());
        dto.setViewCount(content.getViewCount());
        dto.setLikeCount(content.getLikeCount());
        dto.setIsCollected(dailyContentService.isContentCollected(userId, content.getId()));
        
        return dto;
    }
    
    /**
     * 获取内容类型名称
     */
    private String getContentTypeName(Integer contentType) {
        switch (contentType) {
            case 1: return "编程格言";
            case 2: return "技术小贴士";
            case 3: return "代码片段";
            case 4: return "历史上的今天";
            default: return "未知类型";
        }
    }
    
    /**
     * 获取难度等级名称
     */
    private String getDifficultyLevelName(Integer difficultyLevel) {
        if (difficultyLevel == null) {
            return null;
        }
        switch (difficultyLevel) {
            case 1: return "初级";
            case 2: return "中级";
            case 3: return "高级";
            default: return "未知";
        }
    }
}
