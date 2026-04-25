package com.xiaou.mockinterview.service;

import com.xiaou.mockinterview.domain.MockInterviewQA;
import com.xiaou.mockinterview.domain.MockInterviewSession;
import com.xiaou.mockinterview.dto.AIEvaluationResult;

import java.util.List;

/**
 * AI面试官服务接口
 *
 * @author xiaou
 */
public interface AIInterviewerService {

    /**
     * 评价用户回答
     *
     * @param session  面试会话
     * @param question 问题内容
     * @param answer   用户回答
     * @param followUpCount 当前追问次数
     * @return 评价结果
     */
    AIEvaluationResult evaluateAnswer(MockInterviewSession session, String question, String answer, int followUpCount);

    /**
     * 生成面试总结和建议
     *
     * @param session 面试会话
     * @param qaList  问答记录列表
     * @return 总结结果（包含 summary 和 suggestions）
     */
    SummaryResult generateSummaryAndSuggestions(MockInterviewSession session, List<MockInterviewQA> qaList);

    /**
     * 生成追问问题（用户主动请求追问时调用）
     *
     * @param session  面试会话
     * @param question 原问题
     * @param answer   用户回答
     * @param followUpCount 当前追问次数
     * @return 追问问题内容
     */
    String generateFollowUpQuestion(MockInterviewSession session, String question, String answer, int followUpCount);

    /**
     * 总结结果
     */
    class SummaryResult {
        private String summary;
        private String overallLevel;
        private List<String> suggestions;

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getOverallLevel() { return overallLevel; }
        public void setOverallLevel(String overallLevel) { this.overallLevel = overallLevel; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    }
}
