package com.xiaou.ai.eval;

import com.xiaou.ai.dto.interview.GeneratedQuestion;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 模拟面试题生成评测结果包装。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class InterviewGeneratedQuestionsEvalResult {

    /**
     * 生成的题目列表。
     */
    private List<GeneratedQuestion> questions;

    /**
     * 题目数量。
     */
    private int questionCount;

    /**
     * 是否命中降级结果。
     */
    private boolean fallback;
}
