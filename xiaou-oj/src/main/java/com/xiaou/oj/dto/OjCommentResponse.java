package com.xiaou.oj.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * OJ评论响应DTO
 *
 * @author xiaou
 */
@Data
public class OjCommentResponse {

    private Long id;

    /** 题目ID */
    private Long problemId;

    /** 父评论ID */
    private Long parentId;

    /** 评论内容 */
    private String content;

    /** 评论者用户ID */
    private Long authorId;

    /** 评论者用户名 */
    private String authorName;

    /** 点赞数 */
    private Integer likeCount;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /** 当前用户是否点赞 */
    private Boolean isLiked;

    /** 回复的评论ID */
    private Long replyToId;

    /** 回复的用户ID */
    private Long replyToUserId;

    /** 回复的用户名 */
    private String replyToUserName;

    /** 回复数量 */
    private Integer replyCount;

    /** 二级回复列表（最多2条） */
    private List<OjCommentResponse> replies;

    /** 是否有更多回复 */
    private Boolean hasMoreReplies;
}
