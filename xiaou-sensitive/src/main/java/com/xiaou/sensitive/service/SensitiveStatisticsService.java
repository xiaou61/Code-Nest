package com.xiaou.sensitive.service;

import com.xiaou.sensitive.dto.HotWordVO;
import com.xiaou.sensitive.dto.SensitiveStatisticsQuery;
import com.xiaou.sensitive.dto.StatisticsOverviewVO;
import com.xiaou.sensitive.dto.TrendDataVO;

import java.util.List;
import java.util.Map;

/**
 * 敏感词统计服务接口
 *
 * @author xiaou
 */
public interface SensitiveStatisticsService {

    /**
     * 记录敏感词命中
     *
     * @param word 敏感词
     * @param module 业务模块
     * @param categoryId 分类ID
     */
    void recordHit(String word, String module, Integer categoryId);

    /**
     * 记录用户违规
     *
     * @param userId 用户ID
     */
    void recordUserViolation(Long userId);

    /**
     * 获取统计总览
     *
     * @param query 查询条件
     * @return 统计总览
     */
    StatisticsOverviewVO getOverview(SensitiveStatisticsQuery query);

    /**
     * 获取命中趋势
     *
     * @param query 查询条件
     * @return 趋势数据列表
     */
    List<TrendDataVO> getHitTrend(SensitiveStatisticsQuery query);

    /**
     * 获取热门敏感词
     *
     * @param query 查询条件
     * @return 热门敏感词列表
     */
    List<HotWordVO> getHotWords(SensitiveStatisticsQuery query);

    /**
     * 获取分类分布
     *
     * @param query 查询条件
     * @return 分类分布数据
     */
    List<Map<String, Object>> getCategoryDistribution(SensitiveStatisticsQuery query);

    /**
     * 获取模块分布
     *
     * @param query 查询条件
     * @return 模块分布数据
     */
    List<Map<String, Object>> getModuleDistribution(SensitiveStatisticsQuery query);

    /**
     * 导出统计报表（CSV）
     *
     * @param query 查询条件
     * @return 导出结果
     */
    ExportResult exportReport(SensitiveStatisticsQuery query);

    /**
     * 导出结果
     */
    class ExportResult {
        private final String fileName;
        private final String content;

        public ExportResult(String fileName, String content) {
            this.fileName = fileName;
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContent() {
            return content;
        }
    }
}
