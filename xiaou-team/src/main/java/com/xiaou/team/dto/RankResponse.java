package com.xiaou.team.dto;

import lombok.Data;

/**
 * 排行榜响应DTO
 * 
 * @author xiaou
 */
@Data
public class RankResponse {
    
    /**
     * 排名
     */
    private Integer rank;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名称
     */
    private String userName;
    
    /**
     * 用户头像
     */
    private String userAvatar;
    
    /**
     * 成员角色
     */
    private Integer role;
    
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 总打卡次数
     */
    private Integer totalCheckins;
    
    /**
     * 连续打卡天数
     */
    private Integer streakDays;
    
    /**
     * 本周打卡次数
     */
    private Integer weeklyCheckins;
    
    /**
     * 本月打卡次数
     */
    private Integer monthlyCheckins;
    
    /**
     * 总学习时长（分钟）
     */
    private Integer totalDuration;
    
    /**
     * 完成率（百分比）
     */
    private Double completionRate;
    
    /**
     * 贡献值/积分
     */
    private Integer contribution;
    
    /**
     * 是否是当前用户
     */
    private Boolean isCurrentUser;

    public Integer getMemberRole() {
        return role;
    }

    public Integer getCheckinCount() {
        return totalCheckins;
    }
}
