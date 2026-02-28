package com.xiaou.oj.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OJ题目
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class OjProblem {

    private Long id;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 题目描述 (Markdown)
     */
    private String description;

    /**
     * 难度 (easy/medium/hard)
     */
    private String difficulty;

    /**
     * 时间限制 (ms)
     */
    private Integer timeLimit;

    /**
     * 内存限制 (MB)
     */
    private Integer memoryLimit;

    /**
     * 输入说明
     */
    private String inputDescription;

    /**
     * 输出说明
     */
    private String outputDescription;

    /**
     * 示例输入
     */
    private String sampleInput;

    /**
     * 示例输出
     */
    private String sampleOutput;

    /**
     * 通过数
     */
    private Integer acceptedCount;

    /**
     * 提交数
     */
    private Integer submitCount;

    /**
     * 状态 (0=隐藏, 1=公开)
     */
    private Integer status;

    /**
     * 创建者用户ID
     */
    private Long createUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /**
     * 标签列表 (查询时使用)
     */
    private transient List<OjProblemTag> tags;
}
