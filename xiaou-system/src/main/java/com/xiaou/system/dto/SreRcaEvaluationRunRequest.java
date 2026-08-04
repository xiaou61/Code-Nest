package com.xiaou.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;

/**
 * RCA 手动评测运行请求；caseId 为空时回放当前全部已提升用例。
 *
 * @author xiaou
 */
public record SreRcaEvaluationRunRequest(
        @Min(value = 1, message = "评测用例 ID 不合法")
        Long caseId,
        @Min(value = 1, message = "评测套件版本 ID 不合法")
        Long suiteVersionId
) {

    public SreRcaEvaluationRunRequest(Long caseId) {
        this(caseId, null);
    }

    @AssertTrue(message = "评测用例与套件版本不能同时指定")
    public boolean isSelectionValid() {
        return caseId == null || suiteVersionId == null;
    }
}
