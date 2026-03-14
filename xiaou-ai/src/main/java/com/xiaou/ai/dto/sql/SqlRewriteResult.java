package com.xiaou.ai.dto.sql;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL重写建议结果
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class SqlRewriteResult {

    /**
     * 优化后的SQL
     */
    private String optimizedSql;

    /**
     * 建议索引DDL
     */
    private List<String> indexDdls;

    /**
     * 重写原因
     */
    private String rewriteReason;

    /**
     * 风险提示
     */
    private List<String> riskWarnings;

    /**
     * 预期收益
     */
    private String expectedImprovement;

    /**
     * 是否为降级结果
     */
    private boolean fallback;

    public static SqlRewriteResult fallbackResult(String originalSql) {
        return new SqlRewriteResult()
                .setOptimizedSql(originalSql)
                .setIndexDdls(new ArrayList<>())
                .setRewriteReason("当前为降级重写结果，建议稍后重试获取完整AI重写建议")
                .setRiskWarnings(List.of("Coze重写工作流不可用，未生成高置信索引建议"))
                .setExpectedImprovement("请在工作流恢复后重新生成预计收益")
                .setFallback(true);
    }
}
