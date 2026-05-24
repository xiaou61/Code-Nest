package com.xiaou.points.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户抽奖限制实体类
 * 
 * @author xiaou
 */
@Data
public class UserLotteryLimit {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 今日抽奖次数
     */
    private Integer todayDrawCount;
    
    /**
     * 本周抽奖次数
     */
    private Integer weekDrawCount;
    
    /**
     * 本月抽奖次数
     */
    private Integer monthDrawCount;
    
    /**
     * 总抽奖次数
     */
    private Integer totalDrawCount;
    
    /**
     * 今日中奖次数
     */
    private Integer todayWinCount;
    
    /**
     * 总中奖次数
     */
    private Integer totalWinCount;
    
    /**
     * 最大连续未中奖次数
     */
    private Integer maxContinuousNoWin;
    
    /**
     * 当前连续未中奖次数
     */
    private Integer currentContinuousNoWin;
    
    /**
     * 最后抽奖时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastDrawTime;
    
    /**
     * 最后中奖时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastWinTime;
    
    /**
     * 是否黑名单：0-否 1-是
     */
    private Integer isBlacklist;
    
    /**
     * 风险等级：0-正常 1-低风险 2-中风险 3-高风险
     */
    private Integer riskLevel;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    public Integer getContinuousNoWin() {
        return currentContinuousNoWin;
    }
}

