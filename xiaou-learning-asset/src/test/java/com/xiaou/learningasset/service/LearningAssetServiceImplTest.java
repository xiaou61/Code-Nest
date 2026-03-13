package com.xiaou.learningasset.service;

import com.xiaou.learningasset.domain.LearningAssetCandidate;
import com.xiaou.learningasset.domain.LearningAssetRecord;
import com.xiaou.learningasset.dto.request.LearningAssetConvertRequest;
import com.xiaou.learningasset.dto.response.LearningAssetRecordDetailResponse;
import com.xiaou.learningasset.dto.source.LearningAssetSourceSnapshot;
import com.xiaou.learningasset.dto.transform.TransformCandidateDraft;
import com.xiaou.learningasset.dto.transform.TransformResult;
import com.xiaou.learningasset.enums.LearningAssetCandidateStatus;
import com.xiaou.learningasset.enums.TargetAssetType;
import com.xiaou.learningasset.enums.TransformMode;
import com.xiaou.learningasset.mapper.LearningAssetCandidateMapper;
import com.xiaou.learningasset.mapper.LearningAssetPublishLogMapper;
import com.xiaou.learningasset.mapper.LearningAssetRecordMapper;
import com.xiaou.learningasset.service.impl.LearningAssetServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningAssetServiceImplTest {

    @Mock
    private LearningAssetRecordMapper recordMapper;

    @Mock
    private LearningAssetCandidateMapper candidateMapper;

    @Mock
    private LearningAssetPublishLogMapper publishLogMapper;

    @Mock
    private LearningAssetSourceService sourceService;

    @Mock
    private LearningAssetTransformEngine transformEngine;

    @InjectMocks
    private LearningAssetServiceImpl service;

    @Test
    void convertShouldCreateRecordAndCandidates() {
        LearningAssetConvertRequest request = new LearningAssetConvertRequest();
        request.setSourceType("blog");
        request.setSourceId(1L);
        request.setTransformMode("study");
        request.setTargetTypes(List.of("flashcard", "practice_plan"));

        LearningAssetSourceSnapshot snapshot = LearningAssetSourceSnapshot.builder()
                .sourceType("blog")
                .sourceId(1L)
                .title("Redis 缓存击穿实战")
                .summary("互斥锁和逻辑过期是常见策略")
                .content("缓存击穿的本质是热点 key 失效时大量请求打到数据库。")
                .tags(List.of("Redis", "缓存"))
                .build();

        TransformCandidateDraft candidateDraft = TransformCandidateDraft.builder()
                .assetType(TargetAssetType.FLASHCARD)
                .title("Redis 缓存击穿 · 记忆卡 1")
                .contentJson("{\"frontContent\":\"缓存击穿\",\"backContent\":\"互斥锁和逻辑过期是常见策略\"}")
                .tags("Redis,缓存")
                .confidenceScore(0.88D)
                .targetModule("xiaou-flashcard")
                .build();

        when(sourceService.loadSnapshot(eq(1001L), any(LearningAssetConvertRequest.class))).thenReturn(snapshot);
        when(transformEngine.transform(eq(snapshot), eq(TransformMode.STUDY), any())).thenReturn(
                TransformResult.builder()
                        .summary("互斥锁和逻辑过期是常见策略")
                        .candidates(List.of(candidateDraft))
                        .build()
        );

        doAnswer(invocation -> {
            LearningAssetRecord record = invocation.getArgument(0);
            record.setId(99L);
            return 1;
        }).when(recordMapper).insert(any(LearningAssetRecord.class));

        when(recordMapper.selectByUserIdAndId(1001L, 99L)).thenReturn(
                new LearningAssetRecord()
                        .setId(99L)
                        .setUserId(1001L)
                        .setSourceType("blog")
                        .setSourceId(1L)
                        .setSourceTitle("Redis 缓存击穿实战")
                        .setTransformMode("STUDY")
                        .setStatus("PENDING_CONFIRM")
                        .setSummaryText("互斥锁和逻辑过期是常见策略")
                        .setTotalCandidates(1)
                        .setPublishedCandidates(0)
        );

        doAnswer(invocation -> {
            LearningAssetCandidate candidate = invocation.getArgument(0);
            candidate.setId(199L);
            return 1;
        }).when(candidateMapper).insert(any(LearningAssetCandidate.class));

        when(candidateMapper.selectByRecordId(99L)).thenReturn(List.of(
                new LearningAssetCandidate().setId(199L).setRecordId(99L).setUserId(1001L)
                        .setAssetType("flashcard").setTitle("Redis 缓存击穿 · 记忆卡 1")
                        .setContentJson(candidateDraft.getContentJson()).setTags("Redis,缓存")
                        .setConfidenceScore(0.88D).setTargetModule("xiaou-flashcard").setStatus("DRAFT")
        ));

        LearningAssetRecordDetailResponse response = service.convert(1001L, request);

        assertNotNull(response);
        assertEquals(99L, response.getRecordId());
        assertEquals("PENDING_CONFIRM", response.getStatus());
        assertEquals(1, response.getCandidates().size());
        verify(recordMapper).insert(any(LearningAssetRecord.class));
        verify(candidateMapper).insert(any(LearningAssetCandidate.class));
    }

    @Test
    void discardCandidateShouldMarkCandidateAsDiscarded() {
        LearningAssetCandidate candidate = new LearningAssetCandidate()
                .setId(199L)
                .setRecordId(99L)
                .setUserId(1001L)
                .setAssetType("flashcard")
                .setTitle("Redis 缓存击穿 · 记忆卡 1")
                .setContentJson("{\"frontContent\":\"缓存击穿\",\"backContent\":\"互斥锁和逻辑过期\"}")
                .setStatus(LearningAssetCandidateStatus.SELECTED.name());

        LearningAssetRecord record = new LearningAssetRecord()
                .setId(99L)
                .setUserId(1001L)
                .setSourceType("blog")
                .setSourceId(1L)
                .setSourceTitle("Redis 缓存击穿实战")
                .setTransformMode("STUDY")
                .setStatus("PENDING_CONFIRM")
                .setSummaryText("互斥锁和逻辑过期是常见策略")
                .setTotalCandidates(1)
                .setPublishedCandidates(0);

        when(candidateMapper.selectById(199L)).thenReturn(candidate);
        when(recordMapper.selectByUserIdAndId(1001L, 99L)).thenReturn(record);
        when(candidateMapper.selectByRecordId(99L)).thenReturn(List.of(
                new LearningAssetCandidate().setId(199L).setRecordId(99L).setUserId(1001L)
                        .setAssetType("flashcard").setTitle("Redis 缓存击穿 · 记忆卡 1")
                        .setContentJson(candidate.getContentJson()).setStatus(LearningAssetCandidateStatus.DISCARDED.name())
        ));

        LearningAssetRecordDetailResponse response = service.discardCandidate(1001L, 199L);

        assertEquals(99L, response.getRecordId());
        assertEquals(LearningAssetCandidateStatus.DISCARDED.name(), response.getCandidates().get(0).getStatus());
        verify(candidateMapper).updateStatus(199L, LearningAssetCandidateStatus.DISCARDED.name(), null, null);
        verify(recordMapper).update(any(LearningAssetRecord.class));
    }
}
