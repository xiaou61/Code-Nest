package com.xiaou.learningasset.service;

import com.xiaou.learningasset.dto.source.LearningAssetSourceSnapshot;
import com.xiaou.learningasset.dto.transform.TransformCandidateDraft;
import com.xiaou.learningasset.dto.transform.TransformResult;
import com.xiaou.learningasset.enums.TargetAssetType;
import com.xiaou.learningasset.enums.TransformMode;
import com.xiaou.learningasset.service.impl.LearningAssetTransformEngineImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningAssetTransformEngineTest {

    private final LearningAssetTransformEngine engine = new LearningAssetTransformEngineImpl();

    @Test
    void shouldGenerateFlashcardAndPracticeCandidatesFromBlogSource() {
        LearningAssetSourceSnapshot snapshot = LearningAssetSourceSnapshot.builder()
                .sourceType("blog")
                .title("Redis 缓存击穿实战")
                .summary("用互斥锁和逻辑过期来降低热点 key 失效时的数据库压力。")
                .content("缓存击穿是热点 key 在过期瞬间大量并发请求打到数据库。可以使用互斥锁、逻辑过期、热点永不过期等策略。")
                .tags(List.of("Redis", "缓存", "高并发"))
                .build();

        TransformResult result = engine.transform(snapshot, TransformMode.STUDY,
                List.of(TargetAssetType.FLASHCARD, TargetAssetType.PRACTICE_PLAN));

        assertFalse(result.getCandidates().isEmpty());
        assertTrue(result.getCandidates().stream()
                .map(TransformCandidateDraft::getAssetType)
                .anyMatch(TargetAssetType.FLASHCARD::equals));
        assertTrue(result.getCandidates().stream()
                .map(TransformCandidateDraft::getAssetType)
                .anyMatch(TargetAssetType.PRACTICE_PLAN::equals));
    }
}
