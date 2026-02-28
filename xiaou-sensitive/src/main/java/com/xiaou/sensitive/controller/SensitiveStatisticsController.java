package com.xiaou.sensitive.controller;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.sensitive.dto.HotWordVO;
import com.xiaou.sensitive.dto.SensitiveStatisticsQuery;
import com.xiaou.sensitive.dto.StatisticsOverviewVO;
import com.xiaou.sensitive.dto.TrendDataVO;
import com.xiaou.sensitive.service.SensitiveStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 敏感词统计控制器
 *
 * @author xiaou
 */
@RestController
@RequestMapping("/sensitive/statistics")
@RequiredArgsConstructor
public class SensitiveStatisticsController {

    private final SensitiveStatisticsService statisticsService;

    /**
     * 获取统计总览
     */
    @PostMapping("/overview")
    @RequireAdmin
    public Result<StatisticsOverviewVO> getOverview(@RequestBody SensitiveStatisticsQuery query) {
        StatisticsOverviewVO overview = statisticsService.getOverview(query);
        return Result.success(overview);
    }

    /**
     * 获取命中趋势
     */
    @PostMapping("/trend")
    @RequireAdmin
    public Result<List<TrendDataVO>> getHitTrend(@RequestBody SensitiveStatisticsQuery query) {
        List<TrendDataVO> trend = statisticsService.getHitTrend(query);
        return Result.success(trend);
    }

    /**
     * 获取热门敏感词
     */
    @PostMapping("/hot-words")
    @RequireAdmin
    public Result<List<HotWordVO>> getHotWords(@RequestBody SensitiveStatisticsQuery query) {
        List<HotWordVO> hotWords = statisticsService.getHotWords(query);
        return Result.success(hotWords);
    }

    /**
     * 获取分类分布
     */
    @PostMapping("/category-distribution")
    @RequireAdmin
    public Result<List<Map<String, Object>>> getCategoryDistribution(@RequestBody SensitiveStatisticsQuery query) {
        List<Map<String, Object>> distribution = statisticsService.getCategoryDistribution(query);
        return Result.success(distribution);
    }

    /**
     * 获取模块分布
     */
    @PostMapping("/module-distribution")
    @RequireAdmin
    public Result<List<Map<String, Object>>> getModuleDistribution(@RequestBody SensitiveStatisticsQuery query) {
        List<Map<String, Object>> distribution = statisticsService.getModuleDistribution(query);
        return Result.success(distribution);
    }

    /**
     * 导出统计报表
     */
    @PostMapping("/export")
    @RequireAdmin
    public Result<SensitiveStatisticsService.ExportResult> export(@RequestBody(required = false) SensitiveStatisticsQuery query) {
        SensitiveStatisticsQuery exportQuery = query == null ? new SensitiveStatisticsQuery() : query;
        return Result.success(statisticsService.exportReport(exportQuery));
    }
}
