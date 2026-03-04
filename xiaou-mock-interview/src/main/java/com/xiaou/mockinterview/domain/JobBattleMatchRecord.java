package com.xiaou.mockinterview.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 岗位匹配引擎分析记录
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class JobBattleMatchRecord {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 分析名称
     */
    private String analysisName;

    /**
     * 目标岗位数量
     */
    private Integer targetCount;

    /**
     * 最佳匹配分
     */
    private Integer bestScore;

    /**
     * 平均匹配分
     */
    private Integer averageScore;

    /**
     * 降级次数（目标岗位维度）
     */
    private Integer fallbackCount;

    /**
     * 最佳岗位名称
     */
    private String bestTargetRole;

    /**
     * 简历正文快照
     */
    private String resumeText;

    /**
     * 项目亮点快照
     */
    private String projectHighlights;

    /**
     * 目标公司类型
     */
    private String targetCompanyType;

    /**
     * 分析结果JSON
     */
    private String resultJson;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}

