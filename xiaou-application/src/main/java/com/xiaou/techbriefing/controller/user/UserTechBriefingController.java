package com.xiaou.techbriefing.controller.user;

import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.techbriefing.dto.request.TechBriefingArticleQueryRequest;
import com.xiaou.techbriefing.dto.request.TechBriefingSubscriptionCreateRequest;
import com.xiaou.techbriefing.dto.response.TechBriefingArticleCardResponse;
import com.xiaou.techbriefing.dto.response.TechBriefingArticleDetailResponse;
import com.xiaou.techbriefing.dto.response.TechBriefingSubscriptionResponse;
import com.xiaou.techbriefing.service.TechBriefingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端科技热点控制器
 */
@Validated
@RestController
@RequestMapping("/tech-briefing")
@RequiredArgsConstructor
public class UserTechBriefingController {

    private final TechBriefingService techBriefingService;

    @GetMapping("/articles")
    public Result<?> listArticles(TechBriefingArticleQueryRequest request) {
        return Result.success(techBriefingService.listArticles(request));
    }

    @GetMapping("/articles/{articleId}")
    public Result<TechBriefingArticleDetailResponse> getArticleDetail(@PathVariable Long articleId) {
        return Result.success(techBriefingService.getArticleDetail(articleId));
    }

    @GetMapping("/highlights")
    public Result<List<TechBriefingArticleCardResponse>> getHighlights() {
        return Result.success(techBriefingService.getHighlights());
    }

    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        return Result.success(techBriefingService.getCategories());
    }

    @PostMapping("/subscriptions")
    public Result<TechBriefingSubscriptionResponse> createSubscription(@Valid @RequestBody TechBriefingSubscriptionCreateRequest request) {
        Long userId = StpUserUtil.isLogin() ? StpUserUtil.getLoginIdAsLong() : null;
        return Result.success(techBriefingService.createSubscription(userId, request));
    }

    @PostMapping("/subscriptions/{subscriptionId}/test")
    public Result<Void> testSubscription(@PathVariable Long subscriptionId) {
        Long userId = StpUserUtil.isLogin() ? StpUserUtil.getLoginIdAsLong() : null;
        techBriefingService.testSubscription(userId, subscriptionId);
        return Result.success();
    }

    @DeleteMapping("/subscriptions/{subscriptionId}")
    public Result<Void> cancelSubscription(@PathVariable Long subscriptionId) {
        Long userId = StpUserUtil.isLogin() ? StpUserUtil.getLoginIdAsLong() : null;
        techBriefingService.cancelSubscription(userId, subscriptionId);
        return Result.success();
    }
}
