package com.xiaou.mockinterview.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 求职作战台-JD解析请求
 *
 * @author xiaou
 */
@Data
public class JobBattleParseJdRequest {

    /**
     * JD原文
     */
    @NotBlank(message = "JD内容不能为空")
    private String jdText;

    /**
     * 目标岗位（可选）
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

