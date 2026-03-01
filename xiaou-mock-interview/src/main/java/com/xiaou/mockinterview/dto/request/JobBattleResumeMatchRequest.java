package com.xiaou.mockinterview.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 求职作战台-简历匹配请求
 *
 * @author xiaou
 */
@Data
public class JobBattleResumeMatchRequest {

    /**
     * 结构化JD JSON
     */
    @NotBlank(message = "结构化JD不能为空")
    private String parsedJdJson;

    /**
     * 简历文本
     */
    @NotBlank(message = "简历内容不能为空")
    private String resumeText;

    /**
     * 项目亮点（可选）
     */
    private String projectHighlights;

    /**
     * 目标公司类型（可选）
     */
    private String targetCompanyType;
}

