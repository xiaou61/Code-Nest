package com.xiaou.team.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 讨论点赞实体类
 *
 * @author xiaou
 */
@Data
public class StudyTeamDiscussionLike {

    /**
     * 点赞ID
     */
    private Long id;

    /**
     * 讨论ID
     */
    private Long discussionId;

    /**
     * 点赞用户ID
     */
    private Long userId;

    /**
     * 点赞时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
