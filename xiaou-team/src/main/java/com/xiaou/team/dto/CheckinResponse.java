package com.xiaou.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 打卡响应DTO
 * 
 * @author xiaou
 */
@Data
public class CheckinResponse {
    
    /**
     * 打卡ID
     */
    private Long id;
    
    /**
     * 小组ID
     */
    private Long teamId;
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户昵称
     */
    private String userName;
    
    /**
     * 用户头像
     */
    private String userAvatar;
    
    /**
     * 打卡日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate checkinDate;
    
    /**
     * 完成数量
     */
    private Integer completeValue;
    
    /**
     * 目标单位
     */
    private String targetUnit;
    
    /**
     * 学习内容
     */
    private String content;
    
    /**
     * 图片列表
     */
    private List<String> imageList;
    
    /**
     * 图片JSON字符串（用于MyBatis映射）
     */
    private String images;

    /**
     * 学习时长（分钟）
     */
    private Integer duration;
    
    /**
     * 心得感悟
     */
    private String feeling;
    
    /**
     * 是否补卡
     */
    private Boolean isSupplement;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 评论数
     */
    private Integer commentCount;
    
    /**
     * 当前用户是否已点赞
     */
    private Boolean liked;
    
    /**
     * 打卡时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    
    /**
     * 相对时间（如：刚刚、5分钟前）
     */
    private String relativeTime;

    public String getCheckinContent() {
        return content;
    }

    public Boolean getIsLiked() {
        return liked;
    }

    public String getImages() {
        if (imageList != null && !imageList.isEmpty()) {
            return String.join(",", imageList);
        }
        return images;
    }
}
