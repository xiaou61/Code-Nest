package com.xiaou.ai.dto.sql;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * SQL优化前后对比结果
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class SqlCompareResult {

    /**
     * 收益评分
     */
    private Integer improvementScore;

    /**
     * 扫描行数变化
     */
    private String deltaRows;

    /**
     * 访问类型变化
     */
    private String deltaType;

    /**
     * Extra变化
     */
    private String deltaExtra;

    /**
     * 总结
     */
    private String summary;

    /**
     * 注意事项
     */
    private List<String> attention;

    /**
     * 是否降级结果
     */
    private boolean fallback;

    public static SqlCompareResult fallbackResult() {
        return new SqlCompareResult()
                .setImprovementScore(0)
                .setDeltaRows("待评估")
                .setDeltaType("待评估")
                .setDeltaExtra("待评估")
                .setSummary("当前为降级对比结果，请稍后重试获取完整收益评估")
                .setAttention(List.of("Coze对比工作流不可用，未生成完整对比结论"))
                .setFallback(true);
    }
}
