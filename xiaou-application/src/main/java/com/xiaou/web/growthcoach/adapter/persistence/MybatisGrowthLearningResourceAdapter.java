package com.xiaou.web.growthcoach.adapter.persistence;

import com.xiaou.codepen.domain.CodePen;
import com.xiaou.codepen.mapper.CodePenMapper;
import com.xiaou.interview.domain.InterviewQuestionSet;
import com.xiaou.interview.mapper.InterviewQuestionSetMapper;
import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.domain.OjProblemTag;
import com.xiaou.oj.mapper.OjProblemMapper;
import com.xiaou.oj.service.OjProblemService;
import com.xiaou.web.growthcoach.port.GrowthLearningResourcePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * 将学习资源模块的持久化对象转换为应用自有读模型。
 */
@Component
@RequiredArgsConstructor
public class MybatisGrowthLearningResourceAdapter implements GrowthLearningResourcePort {

    private final InterviewQuestionSetMapper questionSetMapper;
    private final OjProblemMapper problemMapper;
    private final OjProblemService problemService;
    private final CodePenMapper codePenMapper;

    @Override
    public QuestionSetData questionSetForUser(Long questionSetId, Long userId) {
        if (!questionSetMapper.hasAccessPermission(questionSetId, userId)) {
            return new QuestionSetData(false, null);
        }
        InterviewQuestionSet questionSet = questionSetMapper.selectById(questionSetId);
        return new QuestionSetData(true, questionSet == null ? null : questionSet.getTitle());
    }

    @Override
    public OjProblemData ojProblem(Long problemId) {
        return toProblem(problemMapper.selectById(problemId));
    }

    @Override
    public OjProblemData dailyOjProblem() {
        return toProblem(problemService.getDailyProblem());
    }

    @Override
    public CodePenSourceData ownedCodePen(Long userId, Long codePenId) {
        CodePen pen = codePenMapper.selectById(codePenId);
        if (pen == null || !Objects.equals(userId, pen.getUserId()) || Integer.valueOf(3).equals(pen.getStatus())) {
            return null;
        }
        LocalDateTime updatedAt = pen.getUpdateTime() == null
                ? null
                : LocalDateTime.ofInstant(pen.getUpdateTime().toInstant(), ZoneId.systemDefault());
        return new CodePenSourceData(
                pen.getId(),
                pen.getUserId(),
                pen.getTitle(),
                pen.getHtmlCode(),
                pen.getCssCode(),
                pen.getJsCode(),
                updatedAt
        );
    }

    private OjProblemData toProblem(OjProblem problem) {
        if (problem == null) {
            return null;
        }
        List<String> tags = problem.getTags() == null
                ? List.of()
                : problem.getTags().stream()
                        .filter(Objects::nonNull)
                        .map(OjProblemTag::getName)
                        .filter(Objects::nonNull)
                        .toList();
        return new OjProblemData(
                problem.getId(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getStatus(),
                problem.getAcceptedCount(),
                problem.getSubmitCount(),
                tags
        );
    }
}
