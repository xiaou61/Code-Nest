package com.xiaou.oj.domain;

import lombok.Data;

import java.util.Date;

/**
 * OJ题目评论点赞记录
 *
 * @author xiaou
 */
@Data
public class OjProblemCommentLike {

    private Long id;

    /** 评论ID */
    private Long commentId;

    /** 用户ID */
    private Long userId;

    /** 创建时间 */
    private Date createTime;
}
