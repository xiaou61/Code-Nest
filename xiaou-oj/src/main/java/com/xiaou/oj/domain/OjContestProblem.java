package com.xiaou.oj.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * OJ 赛事题目关联
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class OjContestProblem {

    private Long id;

    /**
     * 赛事ID
     */
    private Long contestId;

    /**
     * 题目ID
     */
    private Long problemId;

    /**
     * 排序号
     */
    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
