package com.xiaou.ai.eval;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 模拟面试追问评测结果包装。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class InterviewFollowUpEvalResult {

    /**
     * 追问内容。
     */
    private String followUpQuestion;

    /**
     * 是否命中降级追问。
     */
    private boolean fallback;
}
