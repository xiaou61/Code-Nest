package com.xiaou.mockinterview.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.ai.dto.interview.AnswerEvaluationResult;
import com.xiaou.ai.dto.interview.InterviewSummaryResult;
import com.xiaou.ai.service.AiInterviewService;
import com.xiaou.mockinterview.domain.MockInterviewQA;
import com.xiaou.mockinterview.domain.MockInterviewSession;
import com.xiaou.mockinterview.dto.AIEvaluationResult;
import com.xiaou.mockinterview.enums.InterviewLevelEnum;
import com.xiaou.mockinterview.enums.InterviewStyleEnum;
import com.xiaou.mockinterview.enums.QAStatusEnum;
import com.xiaou.mockinterview.service.AIInterviewerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI面试官服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIInterviewerServiceImpl implements AIInterviewerService {

    private final AiInterviewService aiInterviewService;

    @Override
    public AIEvaluationResult evaluateAnswer(MockInterviewSession session, String question, String answer, int followUpCount) {
        // 获取面试配置
        InterviewStyleEnum style = InterviewStyleEnum.getByCode(session.getStyle());
        InterviewLevelEnum level = InterviewLevelEnum.getByCode(session.getLevel());

        String levelName = level != null ? level.getName() : "中级";
        String styleName = style != null ? style.getName() : "标准型";

        try {
            // 调用AI服务
            AnswerEvaluationResult aiResult = aiInterviewService.evaluateAnswer(
                    session.getDirection(), levelName, styleName, question, answer, followUpCount);

            // 如果是降级结果，使用本地评估（保留业务特有的风格调整逻辑）
            if (aiResult.isFallback()) {
                log.warn("AI服务返回降级结果，使用本地评估");
                return generateLocalEvaluation(answer, style, followUpCount);
            }

            // 转换AI结果为业务DTO
            return convertToAIEvaluationResult(aiResult, style);

        } catch (Exception e) {
            log.error("AI评价失败，使用本地评估", e);
            return generateLocalEvaluation(answer, style, followUpCount);
        }
    }

    /**
     * 将AI结果转换为业务DTO
     */
    private AIEvaluationResult convertToAIEvaluationResult(AnswerEvaluationResult aiResult, InterviewStyleEnum style) {
        AIEvaluationResult result = new AIEvaluationResult();
        
        // 根据风格调整分数
        int score = aiResult.getScore() != null ? aiResult.getScore() : 5;
        if (style != null) {
            score = Math.max(0, Math.min(10, score + style.getScoreAdjustment()));
        }
        result.setScore(score);
        
        // 转换反馈
        if (aiResult.getFeedback() != null) {
            AIEvaluationResult.Feedback feedback = new AIEvaluationResult.Feedback();
            feedback.setStrengths(aiResult.getFeedback().getStrengths());
            feedback.setImprovements(aiResult.getFeedback().getImprovements());
            result.setFeedback(feedback);
        } else {
            result.setFeedback(createDefaultFeedback(score));
        }
        
        result.setNextAction(aiResult.getNextAction());
        result.setFollowUpQuestion(aiResult.getFollowUpQuestion());
        result.setReferencePoints(aiResult.getReferencePoints());
        
        return result;
    }

    @Override
    public SummaryResult generateSummaryAndSuggestions(MockInterviewSession session, List<MockInterviewQA> qaList) {
        InterviewLevelEnum level = InterviewLevelEnum.getByCode(session.getLevel());
        String levelName = level != null ? level.getName() : "中级";

        // 统计答题情况
        int answeredCount = (int) qaList.stream()
                .filter(qa -> qa.getStatus() == QAStatusEnum.ANSWERED.getCode())
                .count();
        int skippedCount = (int) qaList.stream()
                .filter(qa -> qa.getStatus() == QAStatusEnum.SKIPPED.getCode())
                .count();

        // 构建问答记录JSON
        String qaListJson = buildQAListJson(qaList);
        int totalScore = session.getTotalScore() != null ? session.getTotalScore() : 0;

        try {
            // 调用AI服务
            InterviewSummaryResult aiResult = aiInterviewService.generateSummary(
                    session.getDirection(), levelName, session.getQuestionCount(),
                    answeredCount, skippedCount, totalScore, qaListJson);

            // 如果是降级结果，使用本地生成
            if (aiResult.isFallback()) {
                log.warn("AI服务返回降级结果，使用本地生成");
                return generateLocalSummaryResult(session);
            }

            // 转换为业务结果
            SummaryResult result = new SummaryResult();
            result.setSummary(aiResult.getSummary());
            result.setOverallLevel(aiResult.getOverallLevel());
            result.setSuggestions(aiResult.getSuggestions() != null ? 
                    aiResult.getSuggestions() : generateDefaultSuggestions(session));
            return result;

        } catch (Exception e) {
            log.error("生成AI总结失败", e);
            return generateLocalSummaryResult(session);
        }
    }

    // =================== 私有方法 ===================

    /**
     * 构建问答记录JSON
     */
    private String buildQAListJson(List<MockInterviewQA> qaList) {
        List<Map<String, Object>> list = qaList.stream()
                .filter(qa -> qa.getStatus() != QAStatusEnum.PENDING.getCode())
                .map(qa -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("question", qa.getQuestionContent());
                    item.put("answer", qa.getUserAnswer() != null ? qa.getUserAnswer() : "未作答");
                    item.put("score", qa.getScore() != null ? qa.getScore() : 0);
                    item.put("status", qa.getStatus() == QAStatusEnum.ANSWERED.getCode() ? "已答" : "跳过");
                    return item;
                })
                .collect(Collectors.toList());
        return JSONUtil.toJsonStr(list);
    }

    private AIEvaluationResult generateLocalEvaluation(String answer, InterviewStyleEnum style, int followUpCount) {
        AIEvaluationResult result = new AIEvaluationResult();

        // 根据回答长度简单评分
        int baseScore;
        if (StrUtil.isBlank(answer)) {
            baseScore = 0;
        } else if (answer.length() < 50) {
            baseScore = 4;
        } else if (answer.length() < 150) {
            baseScore = 6;
        } else if (answer.length() < 300) {
            baseScore = 7;
        } else {
            baseScore = 8;
        }

        // 根据风格调整
        if (style != null) {
            baseScore = Math.max(0, Math.min(10, baseScore + style.getScoreAdjustment()));
        }

        result.setScore(baseScore);
        result.setFeedback(createDefaultFeedback(baseScore));

        // 决定是否追问
        // 条件：分数>=4，追问次数<2，且有一定概率
        double followUpRate = (style != null) ? style.getFollowUpRate() : 0.5;
        double random = Math.random();
        boolean shouldFollowUp = baseScore >= 4 && followUpCount < 2 && random < followUpRate;
        
        log.info("本地评估 - 分数:{}, 追问次数:{}, 追问概率:{}, 随机数:{}, 是否追问:{}", 
                baseScore, followUpCount, followUpRate, random, shouldFollowUp);
        
        if (shouldFollowUp) {
            result.setNextAction("followUp");
            result.setFollowUpQuestion(generateFollowUpQuestionByScore(baseScore));
            log.info("触发追问: {}", result.getFollowUpQuestion());
        } else {
            result.setNextAction("nextQuestion");
        }

        return result;
    }

    /**
     * 生成追问问题（内部使用，根据分数）
     */
    private String generateFollowUpQuestionByScore(int score) {
        if (score >= 7) {
            return "你的回答很好，请进一步说明一下具体的实现原理是什么？";
        } else if (score >= 5) {
            return "你提到了一些要点，能否举个具体的例子来说明你的理解？";
        } else {
            return "这个问题的核心概念是什么？请再思考一下。";
        }
    }

    @Override
    public String generateFollowUpQuestion(MockInterviewSession session, String question, String answer, int followUpCount) {
        InterviewStyleEnum style = InterviewStyleEnum.getByCode(session.getStyle());
        InterviewLevelEnum level = InterviewLevelEnum.getByCode(session.getLevel());

        String levelName = level != null ? level.getName() : "中级";
        String styleName = style != null ? style.getName() : "标准型";

        try {
            String followUpQuestion = aiInterviewService.generateFollowUpQuestion(
                    session.getDirection(),
                    levelName,
                    styleName,
                    question,
                    answer,
                    followUpCount
            );
            if (StrUtil.isNotBlank(followUpQuestion)) {
                return followUpQuestion;
            }
            log.warn("AI 追问生成返回空结果，使用本地追问逻辑");
        } catch (Exception e) {
            log.error("AI 生成追问失败，使用本地追问逻辑", e);
        }

        return generateLocalFollowUpQuestion(question, answer);
    }

    /**
     * 本地生成追问问题
     */
    private String generateLocalFollowUpQuestion(String question, String answer) {
        // 根据回答长度和内容生成不同的追问
        if (StrUtil.isBlank(answer) || answer.length() < 50) {
            return "你的回答比较简短，能否展开说说具体的实现方式或应用场景？";
        } else if (answer.length() < 150) {
            return "你提到了一些关键点，能否举一个实际项目中的例子来说明？";
        } else {
            return "你的回答很详细，那么如果遇到性能问题或边界情况，你会怎么处理？";
        }
    }

    private AIEvaluationResult.Feedback createDefaultFeedback(int score) {
        AIEvaluationResult.Feedback feedback = new AIEvaluationResult.Feedback();

        if (score >= 8) {
            feedback.setStrengths(Arrays.asList("回答较为完整", "理解基本正确"));
            feedback.setImprovements(Arrays.asList("可以补充更多细节"));
        } else if (score >= 6) {
            feedback.setStrengths(Arrays.asList("有一定的理解"));
            feedback.setImprovements(Arrays.asList("部分细节需要加强", "建议深入学习相关知识"));
        } else if (score >= 4) {
            feedback.setStrengths(Arrays.asList("尝试回答了问题"));
            feedback.setImprovements(Arrays.asList("基础知识需要加强", "建议系统学习"));
        } else {
            feedback.setStrengths(new ArrayList<>());
            feedback.setImprovements(Arrays.asList("需要加强基础学习", "建议从入门开始系统学习"));
        }

        return feedback;
    }

    /**
     * 生成默认建议
     */
    private List<String> generateDefaultSuggestions(MockInterviewSession session) {
        List<String> suggestions = new ArrayList<>();
        Integer score = session.getTotalScore();

        if (score == null || score < 60) {
            suggestions.add("建议系统学习" + session.getDirection() + "相关基础知识");
            suggestions.add("多做练习题，加强知识点理解");
            suggestions.add("可以参考优秀的技术文章和教程");
        } else if (score < 80) {
            suggestions.add("基础知识掌握良好，建议深入学习底层原理");
            suggestions.add("多进行项目实战，积累经验");
            suggestions.add("尝试阅读源码，理解框架设计思想");
        } else {
            suggestions.add("表现优秀！建议继续保持学习习惯");
            suggestions.add("可以尝试更高难度的面试，挑战自我");
            suggestions.add("考虑分享经验，帮助他人成长");
        }

        return suggestions;
    }

    /**
     * 本地生成总结结果
     */
    private SummaryResult generateLocalSummaryResult(MockInterviewSession session) {
        SummaryResult result = new SummaryResult();
        Integer score = session.getTotalScore();
        String direction = session.getDirection();

        if (score == null) {
            result.setSummary("面试已结束，感谢您的参与。");
            result.setOverallLevel("未评估");
        } else if (score >= 80) {
            result.setSummary(String.format("候选人在%s方向表现优秀，对核心知识点掌握扎实，" +
                    "能够清晰准确地表达技术概念。建议继续保持学习热情，挑战更高难度。", direction));
            result.setOverallLevel("优秀");
        } else if (score >= 60) {
            result.setSummary(String.format("候选人对%s基础知识有一定掌握，但部分知识点理解不够深入。" +
                    "建议加强对底层原理的学习，多进行实践项目锻炼。", direction));
            result.setOverallLevel("良好");
        } else {
            result.setSummary(String.format("候选人%s基础知识较为薄弱，需要系统性地学习相关技术。" +
                    "建议制定学习计划，从基础入门开始，循序渐进地提升技能。", direction));
            result.setOverallLevel("需加强");
        }

        result.setSuggestions(generateDefaultSuggestions(session));
        return result;
    }
}
