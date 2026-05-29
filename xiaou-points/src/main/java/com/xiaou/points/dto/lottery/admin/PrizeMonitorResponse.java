package com.xiaou.points.dto.lottery.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单奖品监控响应
 * 
 * @author xiaou
 */
@Data
public class PrizeMonitorResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 奖品ID
     */
    private Long prizeId;
    
    /**
     * 奖品名称
     */
    private String prizeName;

    /**
     * 奖品等级
     */
    private Integer prizeLevel;
    
    /**
     * 奖品积分
     */
    private Integer prizePoints;
    
    /**
     * 基础概率
     */
    private BigDecimal baseProbability;
    
    /**
     * 当前概率
     */
    private BigDecimal currentProbability;
    
    /**
     * 目标回报率
     */
    private BigDecimal targetReturnRate;
    
    /**
     * 实际回报率
     */
    private BigDecimal actualReturnRate;
    
    /**
     * 今日抽取次数
     */
    private Integer todayDrawCount;
    
    /**
     * 今日中奖次数
     */
    private Integer todayWinCount;
    
    /**
     * 总抽取次数
     */
    private Integer totalDrawCount;
    
    /**
     * 总中奖次数
     */
    private Integer totalWinCount;
    
    /**
     * 今日总成本（发放总积分）
     */
    private Long todayCost;
    
    /**
     * 总成本
     */
    private Long totalCost;

    /**
     * 当前库存
     */
    private Integer currentStock;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 最大回报率
     */
    private BigDecimal maxReturnRate;

    /**
     * 最小回报率
     */
    private BigDecimal minReturnRate;

    /**
     * 状态
     */
    private String status;
    
    /**
     * 是否暂停
     */
    private Boolean isSuspended;
    
    /**
     * 暂停至
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime suspendUntil;
    
    /**
     * 调整策略
     */
    private String adjustStrategy;
    
    /**
     * 上次调整时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastAdjustTime;
}

