package com.xiaou.ai.service.impl;

import com.xiaou.ai.dto.interview.AnswerEvaluationResult;
import com.xiaou.ai.dto.interview.GeneratedQuestion;
import com.xiaou.ai.dto.interview.InterviewSummaryResult;
import com.xiaou.ai.graph.interview.InterviewGraphRunner;
import com.xiaou.ai.service.AiInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 面试AI服务实现
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class AiInterviewServiceImpl implements AiInterviewService {
    private final InterviewGraphRunner interviewGraphRunner;

    @Override
    public AnswerEvaluationResult evaluateAnswer(String direction, String level, String style,
                                                 String question, String answer, int followUpCount) {
        return interviewGraphRunner.runEvaluateAnswer(direction, level, style, question, answer, followUpCount);
    }

    @Override
    public InterviewSummaryResult generateSummary(String direction, String level,
                                                  int questionCount, int answeredCount, int skippedCount,
                                                  int totalScore, String qaListJson) {
        return interviewGraphRunner.runGenerateSummary(
                direction,
                level,
                questionCount,
                answeredCount,
                skippedCount,
                totalScore,
                qaListJson
        );
    }

    @Override
    public List<GeneratedQuestion> generateQuestions(String direction, String level, int count) {
        return interviewGraphRunner.runGenerateQuestions(direction, level, count);
    }

    @Override
    public String generateFollowUpQuestion(String direction, String level, String style,
                                           String question, String answer, int followUpCount) {
        return interviewGraphRunner.runGenerateFollowUp(direction, level, style, question, answer, followUpCount);
    }
}
