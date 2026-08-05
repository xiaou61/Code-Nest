package com.xiaou.sre.dto.rca;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request for a stable RCA evaluation suite identity.
 *
 * @author xiaou
 */
public record SreRcaEvaluationSuiteCreateRequest(
        @NotBlank(message = "评测套件 Key 不能为空")
        @Pattern(regexp = "[a-z][a-z0-9-]{2,63}", message = "评测套件 Key 不合法")
        String suiteKey,
        @NotBlank(message = "评测套件名称不能为空")
        @Size(max = 128, message = "评测套件名称过长")
        String name,
        @Size(max = 500, message = "评测套件说明过长")
        String description
) {
}
