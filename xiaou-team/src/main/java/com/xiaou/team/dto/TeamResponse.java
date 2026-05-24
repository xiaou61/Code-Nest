package com.xiaou.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 小组响应DTO
 * 
 * @author xiaou
 */
@Data
public class TeamResponse {
    
    /**
     * 小组ID
     */
    private Long id;
    
    /**
     * 小组名称
     */
    private String teamName;
    
    /**
     * 小组简介
     */
    private String teamDesc;
    
    /**
     * 小组头像
     */
    private String teamAvatar;
    
    /**
     * 类型：1目标型 2学习型 3打卡型
     */
    private Integer teamType;
    
    /**
     * 类型名称
     */
    private String teamTypeName;
    
    /**
     * 类型图标
     */
    private String teamTypeIcon;
    
    /**
     * 标签列表
     */
    private List<String> tagList;
    
    /**
     * 最大成员数
     */
    private Integer maxMembers;
    
    /**
     * 当前成员数
     */
    private Integer currentMembers;
    
    /**
     * 加入方式：1公开 2申请 3邀请
     */
    private Integer joinType;
    
    /**
     * 加入方式名称
     */
    private String joinTypeName;
    
    /**
     * 目标标题
     */
    private String goalTitle;
    
    /**
     * 目标进度（百分比）
     */
    private Integer goalProgress;
    
    /**
     * 目标开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate goalStartDate;
    
    /**
     * 目标结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate goalEndDate;
    
    /**
     * 7日打卡率
     */
    private Integer checkinRate;

    /**
     * 总打卡次数
     */
    private Integer totalCheckins;

    /**
     * 总讨论数
     */
    private Integer totalDiscussions;

    /**
     * 活跃天数
     */
    private Integer activeDays;
    
    /**
     * 创建者ID
     */
    private Long creatorId;
    
    /**
     * 创建者昵称
     */
    private String creatorName;
    
    /**
     * 创建者头像
     */
    private String creatorAvatar;
    
    /**
     * 状态：0已解散 1正常 2已满员
     */
    private Integer status;
    
    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    
    /**
     * 当前用户是否已加入
     */
    private Boolean joined;
    
    /**
     * 当前用户在组内的角色
     */
    private Integer myRole;

    /**
     * 待审批申请数
     */
    private Integer pendingApplications;

    public String getTags() {
        if (tagList == null || tagList.isEmpty()) {
            return null;
        }
        return String.join(",", tagList);
    }

    public Integer getMemberRole() {
        return myRole;
    }

    public Boolean getIsMember() {
        return joined;
    }
}
