package com.xiaou.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request for publishing one immutable suite version.
 *
 * @author xiaou
 */
public record SreRcaEvaluationSuiteVersionPublishRequest(
        @NotEmpty(message = "评测套件至少包含一个用例")
        @Size(max = 100, message = "评测套件用例数量不能超过 100")
        List<@Positive(message = "评测用例 ID 不合法") Long> caseIds,
        @NotNull(message = "最低通过率不能为空")
        @DecimalMin(value = "0", message = "最低通过率不合法")
        @DecimalMax(value = "100", message = "最低通过率不合法")
        BigDecimal minimumPassRate,
        @NotNull(message = "最低平均分不能为空")
        @DecimalMin(value = "0", message = "最低平均分不合法")
        @DecimalMax(value = "100", message = "最低平均分不合法")
        BigDecimal minimumAverageScore,
        @NotNull(message = "必须明确是否要求全部结果安全")
        Boolean requireAllSafety,
        @NotNull(message = "必须明确是否禁止降级结果")
        Boolean requireNoDegraded
) {

    public SreRcaEvaluationSuiteVersionPublishRequest {
        caseIds = caseIds == null ? List.of() : List.copyOf(caseIds);
    }
}
