package com.xiaou.ai.eval;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI 回归评测用例。
 *
 * <p>采用资源文件驱动，便于持续补充黄金样例而不需要频繁修改测试代码。</p>
 *
 * @author xiaou
 */
@Data
public class AiEvalCase {

    /**
     * 用例唯一标识。
     */
    private String id;

    /**
     * 场景键。
     */
    private String scenario;

    /**
     * 用例说明。
     */
    private String description;

    /**
     * 测试输入。
     */
    private Map<String, Object> input;

    /**
     * 模型返回内容。
     */
    private String response;

    /**
     * 多步图编排场景下的模型返回序列。
     *
     * <p>若存在，则按调用顺序依次消费；未提供时回退到单个 response。</p>
     */
    private List<String> responses;

    /**
     * 是否直接走统一 AI 降级结果。
     */
    private boolean useFallback;

    /**
     * 多步图编排场景下的降级序列。
     *
     * <p>若存在，则按调用顺序决定每一步是否直接走 fallback；未提供时回退到全局 useFallback。</p>
     */
    private List<Boolean> fallbackSequence;

    /**
     * 断言预期。
     */
    private AiEvalExpectation expect;

    @Override
    public String toString() {
        if (id == null && description == null) {
            return "unnamed-case";
        }
        if (description == null) {
            return id;
        }
        return id + " - " + description;
    }
}
