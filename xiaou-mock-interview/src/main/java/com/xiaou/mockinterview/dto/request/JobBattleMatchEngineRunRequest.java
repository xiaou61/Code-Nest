package com.xiaou.mockinterview.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 岗位匹配引擎运行请求
 *
 * @author xiaou
 */
@Data
public class JobBattleMatchEngineRunRequest {

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

    /**
     * 待匹配岗位列表
     */
    @NotEmpty(message = "请至少添加一个目标岗位")
    @Valid
    private List<TargetJob> targets;

    @Data
    public static class TargetJob {

        /**
         * 岗位JD原文
         */
        @NotBlank(message = "岗位JD不能为空")
        private String jdText;

        /**
         * 目标岗位名称（可选）
         */
        private String targetRole;

        /**
         * 目标级别（可选）
         */
        private String targetLevel;

        /**
         * 目标城市（可选）
         */
        private String city;
    }
}

