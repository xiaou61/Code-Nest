package com.xiaou.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 讨论响应DTO
 * 
 * @author xiaou
 */
@Data
public class DiscussionResponse {
    
    /**
     * 讨论ID
     */
    private Long id;
    
    /**
     * 小组ID
     */
    private Long teamId;
    
    /**
     * 发布者ID
     */
    private Long userId;
    
    /**
     * 发布者名称
     */
    private String userName;
    
    /**
     * 发布者头像
     */
    private String userAvatar;
    
    /**
     * 发布者角色
     */
    private Integer userRole;
    
    /**
     * 角色名称
     */
    private String userRoleName;
    
    /**
     * 分类
     */
    private Integer category;
    
    /**
     * 分类名称
     */
    private String categoryName;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 内容
     */
    private String content;
    
    /**
     * 图片列表
     */
    private List<String> images;
    
    /**
     * 图片JSON字符串（用于MyBatis映射）
     */
    private String imagesJson;
    
    /**
     * 浏览量
     */
    private Integer viewCount;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 评论数
     */
    private Integer commentCount;
    
    /**
     * 是否置顶
     */
    private Integer isTop;
    
    /**
     * 是否精华
     */
    private Integer isEssence;
    
    /**
     * 当前用户是否已点赞
     */
    private Boolean liked;
    
    /**
     * 是否是当前用户发布的
     */
    private Boolean isOwner;
    
    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    
    /**
     * 相对时间
     */
    private String relativeTime;
    
    /**
     * 最后回复时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastReplyTime;

    public Integer getReplyCount() {
        return commentCount;
    }

    public Boolean getIsLiked() {
        return liked;
    }
}
