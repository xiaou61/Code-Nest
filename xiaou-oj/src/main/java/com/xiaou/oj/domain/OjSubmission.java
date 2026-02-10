package com.xiaou.oj.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * OJ提交记录
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class OjSubmission {

    private Long id;

    /**
     * 题目ID
     */
    private Long problemId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 提交的源代码
     */
    private String code;

    /**
     * 判题状态
     */
    private String status;

    /**
     * 耗时 (ms)
     */
    private Long timeUsed;

    /**
     * 内存使用 (KB)
     */
    private Long memoryUsed;

    /**
     * 错误信息 (编译/运行错误)
     */
    private String errorMessage;

    /**
     * 通过用例数
     */
    private Integer passCount;

    /**
     * 总用例数
     */
    private Integer totalCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 题目标题 (查询时使用)
     */
    private transient String problemTitle;
}
