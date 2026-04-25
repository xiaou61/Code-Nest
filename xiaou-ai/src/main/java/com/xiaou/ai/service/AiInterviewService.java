package com.xiaou.ai.service;

import com.xiaou.ai.dto.interview.AnswerEvaluationResult;
import com.xiaou.ai.dto.interview.GeneratedQuestion;
import com.xiaou.ai.dto.interview.InterviewSummaryResult;

import java.util.List;

/**
 * 面试AI服务接口
 *
 * @author xiaou
 */
public interface AiInterviewService {

    /**
     * 评价面试答案
     *
     * @param direction     面试方向
     * @param level         难度级别
     * @param style         面试风格
     * @param question      面试问题
     * @param answer        用户答案
     * @param followUpCount 当前追问次数
     * @return 评价结果
     */
    AnswerEvaluationResult evaluateAnswer(String direction, String level, String style,
                                          String question, String answer, int followUpCount);

    /**
     * 生成面试总结
     *
     * @param direction     面试方向
     * @param level         难度级别
     * @param questionCount 题目总数
     * @param answeredCount 已回答数
     * @param skippedCount  跳过数
     * @param totalScore    总得分
     * @param qaListJson    问答记录JSON
     * @return 总结结果
     */
    InterviewSummaryResult generateSummary(String direction, String level,
                                           int questionCount, int answeredCount, int skippedCount,
                                           int totalScore, String qaListJson);

    /**
     * AI生成面试题目
     *
     * @param direction 面试方向
     * @param level     难度级别
     * @param count     题目数量
     * @return 生成的题目列表
     */
    List<GeneratedQuestion> generateQuestions(String direction, String level, int count);

    /**
     * 生成追问问题
     *
     * @param direction     面试方向
     * @param level         难度级别
     * @param style         面试风格
     * @param question      原始问题
     * @param answer        用户回答
     * @param followUpCount 当前追问次数
     * @return 追问内容
     */
    String generateFollowUpQuestion(String direction, String level, String style,
                                    String question, String answer, int followUpCount);
}
