package com.xiaou.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡任务响应DTO
 * 
 * @author xiaou
 */
@Data
public class TaskResponse {
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 小组ID
     */
    private Long teamId;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 任务描述
     */
    private String taskDesc;
    
    /**
     * 类型：1刷题 2学习时长 3阅读 4自定义
     */
    private Integer taskType;
    
    /**
     * 类型名称
     */
    private String taskTypeName;
    
    /**
     * 目标数量
     */
    private Integer targetValue;
    
    /**
     * 目标单位
     */
    private String targetUnit;
    
    /**
     * 重复：1每日 2工作日 3自定义
     */
    private Integer repeatType;
    
    /**
     * 重复类型名称
     */
    private String repeatTypeName;
    
    /**
     * 自定义重复日
     */
    private String repeatDays;
    
    /**
     * 是否必须附带内容
     */
    private Integer requireContent;
    
    /**
     * 是否必须附带图片
     */
    private Integer requireImage;
    
    /**
     * 开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate endDate;
    
    /**
     * 状态：0禁用 1启用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    
    /**
     * 创建人ID
     */
    private Long createBy;
    
    /**
     * 创建人名称
     */
    private String creatorName;
    
    /**
     * 今日打卡人数
     */
    private Integer todayCheckinCount;
    
    /**
     * 当前用户今日是否已打卡
     */
    private Boolean todayCheckedIn;

    public Long getId() {
        return taskId;
    }

    public Boolean getTodayChecked() {
        return todayCheckedIn;
    }

    public Integer getCheckinCount() {
        return todayCheckinCount;
    }
}
