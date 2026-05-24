package com.xiaou.points.dto.lottery;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 用户抽奖统计响应DTO
 * 
 * @author xiaou
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotteryStatisticsResponse {
    
    /**
     * 总抽奖次数
     */
    private Integer totalDrawCount;

    /**
     * 历史统计兼容字段
     */
    private Integer drawCount;
    
    /**
     * 总中奖次数
     */
    private Integer totalWinCount;
    
    /**
     * 中奖率
     */
    private String winRate;
    
    /**
     * 总消耗积分
     */
    private Long totalCostPoints;
    
    /**
     * 总获得积分
     */
    private Long totalRewardPoints;

    /**
     * 回报率
     */
    private BigDecimal returnRate;
    
    /**
     * 净收益
     */
    private Long netProfit;

    /**
     * 平台利润积分
     */
    private Long platformProfitPoints;

    /**
     * 参与人数
     */
    private Integer participantCount;

    /**
     * 统计日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate statDate;
    
    /**
     * 今日抽奖次数
     */
    private Integer todayDrawCount;
    
    /**
     * 今日中奖次数
     */
    private Integer todayWinCount;
    
    /**
     * 当前连续未中奖次数
     */
    private Integer currentContinuousNoWin;
    
    /**
     * 最大连续未中奖次数
     */
    private Integer maxContinuousNoWin;
}

