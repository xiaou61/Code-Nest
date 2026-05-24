package com.xiaou.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成员响应DTO
 * 
 * @author xiaou
 */
@Data
public class MemberResponse {
    
    /**
     * 成员记录ID
     */
    private Long id;
    
    /**
     * 小组ID
     */
    private Long teamId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 用户头像
     */
    private String userAvatar;
    
    /**
     * 角色：1组长 2管理员 3成员
     */
    private Integer role;
    
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 组内昵称
     */
    private String nickname;
    
    /**
     * 总打卡次数
     */
    private Integer totalCheckins;
    
    /**
     * 当前连续打卡
     */
    private Integer currentStreak;
    
    /**
     * 最长连续打卡
     */
    private Integer maxStreak;
    
    /**
     * 获得点赞数
     */
    private Integer totalLikesReceived;
    
    /**
     * 贡献积分
     */
    private Integer contributionPoints;
    
    /**
     * 状态：0已退出 1正常 2禁言中
     */
    private Integer status;
    
    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 加入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime joinTime;

    /**
     * 禁言结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime muteEndTime;
    
    /**
     * 最后活跃时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastActiveTime;

    public Integer getMemberRole() {
        return role;
    }

    public Integer getCheckinCount() {
        return totalCheckins;
    }

    public Integer getStreakDays() {
        return currentStreak;
    }

    public Boolean getIsMuted() {
        if (muteEndTime != null) {
            return muteEndTime.isAfter(LocalDateTime.now());
        }
        return status != null && status == 2;
    }
}
