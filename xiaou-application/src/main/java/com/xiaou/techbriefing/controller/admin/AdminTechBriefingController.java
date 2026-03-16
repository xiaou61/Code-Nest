package com.xiaou.techbriefing.controller.admin;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.techbriefing.domain.TechBriefingArticle;
import com.xiaou.techbriefing.domain.TechBriefingFetchLog;
import com.xiaou.techbriefing.domain.TechBriefingSource;
import com.xiaou.techbriefing.domain.TechBriefingSubscription;
import com.xiaou.techbriefing.service.TechBriefingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端科技热点控制器
 */
@RestController
@RequestMapping("/admin/tech-briefing")
@RequiredArgsConstructor
public class AdminTechBriefingController {

    private final TechBriefingService techBriefingService;

    @RequireAdmin
    @GetMapping("/sources")
    public Result<List<TechBriefingSource>> getSources() {
        return Result.success(techBriefingService.listSources());
    }

    @RequireAdmin
    @PostMapping("/sources")
    public Result<Void> createSource(@RequestBody TechBriefingSource source) {
        techBriefingService.saveSource(source);
        return Result.success();
    }

    @RequireAdmin
    @PutMapping("/sources/{sourceId}")
    public Result<Void> updateSource(@PathVariable Long sourceId, @RequestBody TechBriefingSource source) {
        source.setId(sourceId);
        techBriefingService.saveSource(source);
        return Result.success();
    }

    @RequireAdmin
    @DeleteMapping("/sources/{sourceId}")
    public Result<Void> deleteSource(@PathVariable Long sourceId) {
        techBriefingService.deleteSource(sourceId);
        return Result.success();
    }

    @RequireAdmin
    @GetMapping("/articles")
    public Result<List<TechBriefingArticle>> getArticles() {
        return Result.success(techBriefingService.listAdminArticles());
    }

    @RequireAdmin
    @PutMapping("/articles/{articleId}/pin")
    public Result<Void> updatePin(@PathVariable Long articleId, @RequestParam(defaultValue = "true") boolean pinned) {
        techBriefingService.updateArticlePin(articleId, pinned);
        return Result.success();
    }

    @RequireAdmin
    @PutMapping("/articles/{articleId}/offline")
    public Result<Void> offlineArticle(@PathVariable Long articleId) {
        techBriefingService.offlineArticle(articleId);
        return Result.success();
    }

    @RequireAdmin
    @PostMapping("/articles/{articleId}/retry-translate")
    public Result<Void> retryTranslate(@PathVariable Long articleId) {
        techBriefingService.retryTranslate(articleId);
        return Result.success();
    }

    @RequireAdmin
    @PostMapping("/articles/{articleId}/retry-summary")
    public Result<Void> retrySummary(@PathVariable Long articleId) {
        techBriefingService.retrySummary(articleId);
        return Result.success();
    }

    @RequireAdmin
    @GetMapping("/subscriptions")
    public Result<List<TechBriefingSubscription>> getSubscriptions() {
        return Result.success(techBriefingService.listSubscriptions());
    }

    @RequireAdmin
    @GetMapping("/subscriptions/statistics")
    public Result<Map<String, Object>> getSubscriptionStatistics() {
        return Result.success(techBriefingService.getSubscriptionStatistics());
    }

    @RequireAdmin
    @PostMapping("/subscriptions/{subscriptionId}/test")
    public Result<Void> testSubscription(@PathVariable Long subscriptionId) {
        techBriefingService.testSubscription(null, subscriptionId);
        return Result.success();
    }

    @RequireAdmin
    @GetMapping("/tasks/logs")
    public Result<List<TechBriefingFetchLog>> getTaskLogs(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(techBriefingService.listFetchLogs(limit));
    }

    @RequireAdmin
    @PostMapping("/tasks/refresh")
    public Result<Void> refresh() {
        techBriefingService.refreshAllSources();
        return Result.success();
    }
}
