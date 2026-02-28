package com.xiaou.oj.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OJ 赛事
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class OjContest {

    private Long id;

    /**
     * 赛事标题
     */
    private String title;

    /**
     * 赛事描述
     */
    private String description;

    /**
     * 赛事类型 (weekly/challenge)
     */
    private String contestType;

    /**
     * 状态 (0=草稿, 1=即将开始, 2=进行中, 3=已结束)
     */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    /**
     * 创建者
     */
    private Long createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /**
     * 题目数（查询回填）
     */
    private transient Integer problemCount;

    /**
     * 参赛人数（查询回填）
     */
    private transient Integer participantCount;

    /**
     * 赛事题目ID列表（详情回填）
     */
    private transient List<Long> problemIds;
}
