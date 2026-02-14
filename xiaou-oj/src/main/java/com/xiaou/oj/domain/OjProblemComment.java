package com.xiaou.oj.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * OJ题目评论实体
 *
 * @author xiaou
 */
@Data
public class OjProblemComment {

    private Long id;

    /** 题目ID */
    private Long problemId;

    /** 父评论ID，0表示顶级评论 */
    private Long parentId;

    /** 评论内容 */
    private String content;

    /** 评论者用户ID */
    private Long authorId;

    /** 评论者用户名 */
    private String authorName;

    /** 点赞数 */
    private Integer likeCount;

    /** 回复的评论ID */
    private Long replyToId;

    /** 回复的用户ID */
    private Long replyToUserId;

    /** 回复的用户名 */
    private String replyToUserName;

    /** 回复数量（仅一级评论有效） */
    private Integer replyCount;

    /** 状态：1-正常，2-删除 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
