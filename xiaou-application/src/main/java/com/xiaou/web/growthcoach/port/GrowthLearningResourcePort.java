package com.xiaou.web.growthcoach.port;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 为应用编排提供最小化的学习资源读模型。
 */
public interface GrowthLearningResourcePort {

    QuestionSetData questionSetForUser(Long questionSetId, Long userId);

    OjProblemData ojProblem(Long problemId);

    OjProblemData dailyOjProblem();

    CodePenSourceData ownedCodePen(Long userId, Long codePenId);

    record QuestionSetData(boolean accessible, String title) {
    }

    record OjProblemData(
            Long id,
            String title,
            String difficulty,
            Integer status,
            Integer acceptedCount,
            Integer submitCount,
            List<String> tags
    ) {
        public OjProblemData {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }

    record CodePenSourceData(
            Long id,
            Long userId,
            String title,
            String html,
            String css,
            String javascript,
            LocalDateTime updatedAt
    ) {
    }
}
