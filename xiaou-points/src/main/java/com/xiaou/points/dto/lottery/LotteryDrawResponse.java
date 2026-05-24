package com.xiaou.points.dto.lottery;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 抽奖响应DTO
 * 
 * @author xiaou
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotteryDrawResponse {
    
    /**
     * 记录ID
     */
    private Long recordId;

    /**
     * 兼容前端的记录ID字段
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;
    
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
     * 奖品图标
     */
    private String prizeIcon;
    
    /**
     * 消耗积分
     */
    private Integer costPoints;
    
    /**
     * 净收益
     */
    private Integer netProfit;
    
    /**
     * 当前余额
     */
    private Integer currentBalance;

    /**
     * 抽奖策略
     */
    private String strategyType;

    /**
     * 抽奖IP
     */
    private String ip;

    /**
     * 抽奖设备
     */
    private String device;
    
    /**
     * 今日抽奖次数
     */
    private Integer todayDrawCount;
    
    /**
     * 今日中奖次数
     */
    private Integer todayWinCount;
    
    /**
     * 是否中奖
     */
    private Boolean isWin;
    
    /**
     * 祝贺语
     */
    private String congratulations;
    
    /**
     * 鼓励语
     */
    private String encouragement;
    
    /**
     * 抽奖时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime drawTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public LocalDateTime getCreateTime() {
        return drawTime;
    }
}

