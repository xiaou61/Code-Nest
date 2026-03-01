package com.xiaou.sensitive.service.impl;

import com.xiaou.sensitive.domain.SensitiveHitStatistics;
import com.xiaou.sensitive.domain.SensitiveUserViolation;
import com.xiaou.sensitive.dto.HotWordVO;
import com.xiaou.sensitive.dto.SensitiveStatisticsQuery;
import com.xiaou.sensitive.dto.StatisticsOverviewVO;
import com.xiaou.sensitive.dto.TrendDataVO;
import com.xiaou.sensitive.mapper.SensitiveHitStatisticsMapper;
import com.xiaou.sensitive.mapper.SensitiveUserViolationMapper;
import com.xiaou.sensitive.service.SensitiveStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 敏感词统计服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveStatisticsServiceImpl implements SensitiveStatisticsService {

    private final SensitiveHitStatisticsMapper hitStatisticsMapper;
    private final SensitiveUserViolationMapper userViolationMapper;

    @Override
    @Async
    public void recordHit(String word, String module, Integer categoryId) {
        try {
            LocalDate today = LocalDate.now();
            
            // 查询是否已有记录
            SensitiveHitStatistics stat = hitStatisticsMapper.selectByDateAndWord(today, word, module);
            
            if (stat == null) {
                // 新增记录
                stat = new SensitiveHitStatistics();
                stat.setStatDate(today);
                stat.setWord(word);
                stat.setCategoryId(categoryId);
                stat.setModule(module);
                stat.setHitCount(1);
                hitStatisticsMapper.insert(stat);
            } else {
                // 更新计数
                hitStatisticsMapper.incrementHitCount(stat.getId());
            }
        } catch (Exception e) {
            log.error("记录敏感词命中失败: word={}, module={}", word, module, e);
        }
    }

    @Override
    @Async
    public void recordUserViolation(Long userId) {
        try {
            LocalDate today = LocalDate.now();
            
            // 查询用户今日违规记录
            SensitiveUserViolation violation = userViolationMapper.selectByUserAndDate(userId, today);
            
            if (violation == null) {
                // 新增记录
                violation = new SensitiveUserViolation();
                violation.setUserId(userId);
                violation.setStatDate(today);
                violation.setViolationCount(1);
                violation.setLastViolationTime(LocalDateTime.now());
                violation.setIsRestricted(0);
                userViolationMapper.insert(violation);
            } else {
                // 更新违规次数
                violation.setViolationCount(violation.getViolationCount() + 1);
                violation.setLastViolationTime(LocalDateTime.now());
                userViolationMapper.updateById(violation);
                
                // 检查是否需要限制用户（24小时内违规超过5次）
                if (violation.getViolationCount() >= 5 && violation.getIsRestricted() == 0) {
                    violation.setIsRestricted(1);
                    violation.setRestrictEndTime(LocalDateTime.now().plusHours(1));
                    userViolationMapper.updateById(violation);
                    log.warn("用户违规次数过多，已限制: userId={}, count={}", userId, violation.getViolationCount());
                }
            }
        } catch (Exception e) {
            log.error("记录用户违规失败: userId={}", userId, e);
        }
    }

    @Override
    public StatisticsOverviewVO getOverview(SensitiveStatisticsQuery query) {
        try {
            applyDefaultTodayRange(query);

            // 查询总命中数
            Long hitCount = hitStatisticsMapper.selectTotalHitCount(query);

            // 查询违规用户数
            Long violationUserCount = userViolationMapper.countRestrictedUsers();

            // 计算拦截率（简化计算，实际应该根据总检测数计算）
            Double hitRate = hitCount > 0 ? (hitCount / 100.0) : 0.0;

            return StatisticsOverviewVO.builder()
                    .totalCheck(hitCount * 10) // 简化估算
                    .hitCount(hitCount)
                    .hitRate(hitRate)
                    .rejectCount(hitCount / 3) // 简化估算
                    .replaceCount(hitCount * 2 / 3) // 简化估算
                    .todayNewWords(0) // 需要额外查询
                    .violationUserCount(violationUserCount)
                    .build();
        } catch (Exception e) {
            log.error("获取统计总览失败", e);
            return StatisticsOverviewVO.builder()
                    .totalCheck(0L)
                    .hitCount(0L)
                    .hitRate(0.0)
                    .rejectCount(0L)
                    .replaceCount(0L)
                    .todayNewWords(0)
                    .violationUserCount(0L)
                    .build();
        }
    }

    @Override
    public List<TrendDataVO> getHitTrend(SensitiveStatisticsQuery query) {
        try {
            applyDefaultRecentSevenDays(query);

            return hitStatisticsMapper.selectTrend(query);
        } catch (Exception e) {
            log.error("获取命中趋势失败", e);
            return List.of();
        }
    }

    @Override
    public List<HotWordVO> getHotWords(SensitiveStatisticsQuery query) {
        try {
            applyDefaultTodayRange(query);
            
            // 设置默认TOP数量
            if (query.getTopN() == null) {
                query.setTopN(20);
            }

            return hitStatisticsMapper.selectHotWords(query);
        } catch (Exception e) {
            log.error("获取热门敏感词失败", e);
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> getCategoryDistribution(SensitiveStatisticsQuery query) {
        try {
            applyDefaultTodayRange(query);

            return hitStatisticsMapper.selectCategoryDistribution(query);
        } catch (Exception e) {
            log.error("获取分类分布失败", e);
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> getModuleDistribution(SensitiveStatisticsQuery query) {
        try {
            applyDefaultTodayRange(query);

            return hitStatisticsMapper.selectModuleDistribution(query);
        } catch (Exception e) {
            log.error("获取模块分布失败", e);
            return List.of();
        }
    }

    @Override
    public ExportResult exportReport(SensitiveStatisticsQuery query) {
        SensitiveStatisticsQuery exportQuery = query == null ? new SensitiveStatisticsQuery() : query;
        applyDefaultRecentSevenDays(exportQuery);

        StatisticsOverviewVO overview = getOverview(cloneQuery(exportQuery));
        List<TrendDataVO> trendData = getHitTrend(cloneQuery(exportQuery));

        SensitiveStatisticsQuery hotQuery = cloneQuery(exportQuery);
        hotQuery.setTopN(100);
        List<HotWordVO> hotWords = getHotWords(hotQuery);

        List<Map<String, Object>> categoryDistribution = getCategoryDistribution(cloneQuery(exportQuery));
        List<Map<String, Object>> moduleDistribution = getModuleDistribution(cloneQuery(exportQuery));

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("统计项,数值\n");
        csv.append("总检测数,").append(nullToZero(overview.getTotalCheck())).append('\n');
        csv.append("命中数,").append(nullToZero(overview.getHitCount())).append('\n');
        csv.append("命中率(%),").append(overview.getHitRate() == null ? 0 : overview.getHitRate()).append('\n');
        csv.append("拒绝数,").append(nullToZero(overview.getRejectCount())).append('\n');
        csv.append("替换数,").append(nullToZero(overview.getReplaceCount())).append('\n');
        csv.append("违规用户数,").append(nullToZero(overview.getViolationUserCount())).append('\n');

        csv.append('\n').append("趋势数据").append('\n');
        csv.append("日期,模块,命中次数\n");
        for (TrendDataVO item : trendData) {
            csv.append(item.getDate() == null ? "" : item.getDate()).append(',')
                    .append(escapeCsv(item.getModule())).append(',')
                    .append(item.getHitCount() == null ? 0 : item.getHitCount())
                    .append('\n');
        }

        csv.append('\n').append("热门敏感词").append('\n');
        csv.append("敏感词,命中次数,分类,模块\n");
        for (HotWordVO item : hotWords) {
            csv.append(escapeCsv(item.getWord())).append(',')
                    .append(item.getHitCount() == null ? 0 : item.getHitCount()).append(',')
                    .append(escapeCsv(item.getCategory())).append(',')
                    .append(escapeCsv(item.getModule()))
                    .append('\n');
        }

        csv.append('\n').append("分类分布").append('\n');
        csv.append("分类,命中次数\n");
        for (Map<String, Object> item : categoryDistribution) {
            csv.append(escapeCsv(valueToString(item.get("category")))).append(',')
                    .append(valueToString(item.get("count")))
                    .append('\n');
        }

        csv.append('\n').append("模块分布").append('\n');
        csv.append("模块,命中次数\n");
        for (Map<String, Object> item : moduleDistribution) {
            csv.append(escapeCsv(valueToString(item.get("module")))).append(',')
                    .append(valueToString(item.get("count")))
                    .append('\n');
        }

        String fileName = "sensitive_statistics_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                ".csv";
        return new ExportResult(fileName, csv.toString());
    }

    private SensitiveStatisticsQuery cloneQuery(SensitiveStatisticsQuery source) {
        SensitiveStatisticsQuery target = new SensitiveStatisticsQuery();
        target.setStartDate(source.getStartDate());
        target.setEndDate(source.getEndDate());
        target.setModule(source.getModule());
        target.setCategoryId(source.getCategoryId());
        target.setTopN(source.getTopN());
        return target;
    }

    private void applyDefaultTodayRange(SensitiveStatisticsQuery query) {
        if (query.getStartDate() == null) {
            query.setStartDate(LocalDate.now());
        }
        if (query.getEndDate() == null) {
            query.setEndDate(LocalDate.now());
        }
    }

    private void applyDefaultRecentSevenDays(SensitiveStatisticsQuery query) {
        if (query.getStartDate() == null) {
            query.setStartDate(LocalDate.now().minusDays(6));
        }
        if (query.getEndDate() == null) {
            query.setEndDate(LocalDate.now());
        }
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private String valueToString(Object value) {
        return value == null ? "0" : String.valueOf(value);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
