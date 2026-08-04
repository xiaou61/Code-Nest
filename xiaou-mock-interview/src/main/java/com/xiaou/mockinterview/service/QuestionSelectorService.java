package com.xiaou.mockinterview.service;

import com.xiaou.interview.domain.InterviewQuestion;

import java.util.List;

/**
 * 题目选择服务接口
 *
 * @author xiaou
 */
public interface QuestionSelectorService {

    /**
     * 根据配置选择题目
     *
     * @param direction     面试方向
     * @param level         难度级别
     * @param questionCount 题目数量
     * @param userId        用户ID（用于避免重复）
     * @return 题目列表
     */
    List<InterviewQuestion> selectQuestions(String direction, Integer level, Integer questionCount, Long userId);

    /**
     * 获取方向关联的分类ID列表
     *
     * @param direction 面试方向
     * @return 分类ID列表
     */
    List<Long> getCategoryIds(String direction);

    /**
     * AI生成题目
     *
     * @param direction     面试方向
     * @param level         难度级别
     * @param questionCount 题目数量
     * @return 生成的题目列表（包含题目内容和参考答案）
     */
    default List<GeneratedQuestion> generateQuestionsByAI(
            String direction,
            Integer level,
            Integer questionCount
    ) {
        return generateQuestionsByAI(direction, level, questionCount, null);
    }

    /**
     * AI 生成带可选专项关注点的题目。
     */
    List<GeneratedQuestion> generateQuestionsByAI(
            String direction,
            Integer level,
            Integer questionCount,
            String specializedTopic
    );

    /**
     * 从指定题库中选择题目
     *
     * @param questionSetIds 题库ID列表
     * @param questionCount  题目数量
     * @param userId         用户ID（用于避免重复）
     * @return 题目列表
     */
    List<InterviewQuestion> selectQuestionsFromSets(List<Long> questionSetIds, Integer questionCount, Long userId);

    /**
     * AI生成的题目
     */
    @lombok.Data
    class GeneratedQuestion {
        private String questionContent;
        private String referenceAnswer;
        private String knowledgePoints;
    }
}
