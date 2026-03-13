package com.xiaou.learningasset.service;

import cn.hutool.json.JSONUtil;
import com.xiaou.common.domain.Notification;
import com.xiaou.common.service.NotificationService;
import com.xiaou.flashcard.dto.request.FlashcardBatchCreateRequest;
import com.xiaou.flashcard.dto.request.FlashcardDeckCreateRequest;
import com.xiaou.flashcard.service.FlashcardDeckService;
import com.xiaou.flashcard.service.FlashcardService;
import com.xiaou.interview.service.InterviewQuestionService;
import com.xiaou.interview.service.InterviewQuestionSetService;
import com.xiaou.knowledge.service.KnowledgeMapService;
import com.xiaou.knowledge.service.KnowledgeNodeService;
import com.xiaou.learningasset.domain.LearningAssetCandidate;
import com.xiaou.learningasset.domain.LearningAssetRecord;
import com.xiaou.learningasset.dto.request.LearningAssetAdminCandidateUpdateRequest;
import com.xiaou.learningasset.dto.request.LearningAssetApproveRequest;
import com.xiaou.learningasset.dto.request.LearningAssetMergeRequest;
import com.xiaou.learningasset.dto.request.LearningAssetReviewQueryRequest;
import com.xiaou.learningasset.dto.response.LearningAssetStatisticsResponse;
import com.xiaou.learningasset.dto.response.LearningAssetPublishResponse;
import com.xiaou.learningasset.enums.LearningAssetCandidateStatus;
import com.xiaou.learningasset.enums.LearningAssetRecordStatus;
import com.xiaou.learningasset.mapper.LearningAssetCandidateMapper;
import com.xiaou.learningasset.mapper.LearningAssetPublishLogMapper;
import com.xiaou.learningasset.mapper.LearningAssetRecordMapper;
import com.xiaou.learningasset.service.impl.LearningAssetPublishServiceImpl;
import com.xiaou.plan.service.PlanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningAssetPublishServiceTest {

    @Mock
    private LearningAssetRecordMapper recordMapper;

    @Mock
    private LearningAssetCandidateMapper candidateMapper;

    @Mock
    private LearningAssetPublishLogMapper publishLogMapper;

    @Mock
    private FlashcardDeckService flashcardDeckService;

    @Mock
    private FlashcardService flashcardService;

    @Mock
    private PlanService planService;

    @Mock
    private KnowledgeMapService knowledgeMapService;

    @Mock
    private KnowledgeNodeService knowledgeNodeService;

    @Mock
    private InterviewQuestionService interviewQuestionService;

    @Mock
    private InterviewQuestionSetService interviewQuestionSetService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LearningAssetPublishServiceImpl publishService;

    @Test
    void publishShouldCreateDeckAndSubmitReviewCandidates() {
        LearningAssetRecord record = new LearningAssetRecord()
                .setId(101L)
                .setUserId(1001L)
                .setSourceTitle("Redis 缓存击穿实战")
                .setStatus(LearningAssetRecordStatus.PENDING_CONFIRM.name())
                .setTotalCandidates(2)
                .setPublishedCandidates(0);

        LearningAssetCandidate flashcardCandidate = new LearningAssetCandidate()
                .setId(201L)
                .setRecordId(101L)
                .setUserId(1001L)
                .setAssetType("flashcard")
                .setTitle("Redis 记忆卡")
                .setContentJson(JSONUtil.toJsonStr(JSONUtil.createObj()
                        .set("frontContent", "什么是缓存击穿")
                        .set("backContent", "热点 key 失效导致并发请求打到数据库")
                        .set("contentType", 1)))
                .setTags("Redis,缓存")
                .setStatus(LearningAssetCandidateStatus.SELECTED.name())
                .setTargetModule("xiaou-flashcard");

        LearningAssetCandidate knowledgeCandidate = new LearningAssetCandidate()
                .setId(202L)
                .setRecordId(101L)
                .setUserId(1001L)
                .setAssetType("knowledge_node")
                .setTitle("缓存击穿")
                .setContentJson(JSONUtil.toJsonStr(JSONUtil.createObj()
                        .set("title", "缓存击穿")
                        .set("summary", "热点 key 失效后需要限流与重建")))
                .setTags("Redis,缓存")
                .setStatus(LearningAssetCandidateStatus.SELECTED.name())
                .setTargetModule("xiaou-knowledge");

        when(recordMapper.selectByUserIdAndId(1001L, 101L)).thenReturn(record);
        when(candidateMapper.selectByRecordId(101L)).thenReturn(List.of(flashcardCandidate, knowledgeCandidate));
        when(flashcardDeckService.createDeck(any(FlashcardDeckCreateRequest.class), eq(1001L))).thenReturn(301L);
        when(flashcardService.batchCreateCards(any(FlashcardBatchCreateRequest.class), eq(1001L))).thenReturn(1);

        LearningAssetPublishResponse response = publishService.publish(1001L, 101L, List.of(201L, 202L));

        assertNotNull(response);
        assertEquals(1, response.getPublishedCount());
        assertEquals(1, response.getReviewingCount());
        assertEquals(301L, response.getFlashcardDeckId());
        assertEquals(LearningAssetCandidateStatus.PUBLISHED.name(), flashcardCandidate.getStatus());
        assertEquals(LearningAssetCandidateStatus.REVIEWING.name(), knowledgeCandidate.getStatus());

        ArgumentCaptor<FlashcardBatchCreateRequest> batchCaptor = ArgumentCaptor.forClass(FlashcardBatchCreateRequest.class);
        verify(flashcardService).batchCreateCards(batchCaptor.capture(), eq(1001L));
        assertEquals(1, batchCaptor.getValue().getCards().size());
        verify(recordMapper).update(any(LearningAssetRecord.class));
        verify(notificationService).sendNotification(argThat((Notification notification) ->
                notification != null
                        && Long.valueOf(1001L).equals(notification.getReceiverId())
                        && "学习资产已发布，部分内容进入审核".equals(notification.getTitle())
        ));
    }

    @Test
    void approveKnowledgeCandidateShouldCreateKnowledgeNode() {
        LearningAssetRecord record = new LearningAssetRecord()
                .setId(101L)
                .setUserId(1001L)
                .setSourceType("blog")
                .setSourceId(1L)
                .setSourceTitle("Redis 缓存击穿实战")
                .setStatus(LearningAssetRecordStatus.REVIEWING.name())
                .setTotalCandidates(1)
                .setPublishedCandidates(0);

        LearningAssetCandidate candidate = new LearningAssetCandidate()
                .setId(202L)
                .setRecordId(101L)
                .setUserId(1001L)
                .setAssetType("knowledge_node")
                .setTitle("缓存击穿")
                .setContentJson(JSONUtil.toJsonStr(JSONUtil.createObj()
                        .set("title", "缓存击穿")
                        .set("summary", "热点 key 失效后需要限流与重建")))
                .setStatus(LearningAssetCandidateStatus.REVIEWING.name())
                .setTargetModule("xiaou-knowledge");

        LearningAssetApproveRequest request = new LearningAssetApproveRequest();
        request.setMapId(88L);
        request.setParentId(0L);
        request.setNote("内容可发布");

        when(candidateMapper.selectById(202L)).thenReturn(candidate);
        when(recordMapper.selectById(101L)).thenReturn(record);
        when(candidateMapper.selectByRecordId(101L)).thenReturn(List.of(candidate));
        when(knowledgeMapService.getById(88L)).thenReturn(new com.xiaou.knowledge.domain.KnowledgeMap());
        when(knowledgeNodeService.createNode(eq(88L), any())).thenReturn(701L);

        Long targetId = publishService.approve(9001L, 202L, request);

        assertEquals(701L, targetId);
        assertEquals(LearningAssetCandidateStatus.PUBLISHED.name(), candidate.getStatus());
        verify(candidateMapper).updateStatus(202L, LearningAssetCandidateStatus.PUBLISHED.name(), 701L, "内容可发布");
        verify(recordMapper).update(any(LearningAssetRecord.class));
        verify(notificationService).sendNotification(argThat((Notification notification) ->
                notification != null
                        && Long.valueOf(1001L).equals(notification.getReceiverId())
                        && "学习资产审核通过".equals(notification.getTitle())
        ));
    }

    @Test
    void shouldListReviewingCandidates() {
        LearningAssetRecord record = new LearningAssetRecord()
                .setId(101L)
                .setSourceType("blog")
                .setSourceId(1L)
                .setSourceTitle("Redis 缓存击穿实战")
                .setSourceSnapshot("{\"title\":\"Redis 缓存击穿实战\"}");
        LearningAssetCandidate candidate = new LearningAssetCandidate()
                .setId(202L)
                .setRecordId(101L)
                .setUserId(1001L)
                .setAssetType("knowledge_node")
                .setTitle("缓存击穿")
                .setContentJson("{\"summary\":\"热点 key 失效后需要限流与重建\"}")
                .setStatus(LearningAssetCandidateStatus.REVIEWING.name())
                .setTargetModule("xiaou-knowledge");

        when(candidateMapper.selectReviewingCandidates("knowledge_node")).thenReturn(List.of(candidate));
        when(recordMapper.selectById(101L)).thenReturn(record);

        LearningAssetReviewQueryRequest request = new LearningAssetReviewQueryRequest();
        request.setAssetType("knowledge_node");

        var result = publishService.getReviewList(request);

        assertEquals(1L, result.getTotal());
        assertEquals("Redis 缓存击穿实战", result.getRecords().get(0).getSourceTitle());
    }

    @Test
    void mergeKnowledgeCandidateShouldBindExistingNode() {
        LearningAssetRecord record = new LearningAssetRecord()
                .setId(101L)
                .setStatus(LearningAssetRecordStatus.REVIEWING.name())
                .setTotalCandidates(1)
                .setPublishedCandidates(0);
        LearningAssetCandidate candidate = new LearningAssetCandidate()
                .setId(202L)
                .setRecordId(101L)
                .setUserId(1001L)
                .setAssetType("knowledge_node")
                .setTitle("缓存击穿")
                .setContentJson("{\"title\":\"缓存击穿\"}")
                .setStatus(LearningAssetCandidateStatus.REVIEWING.name())
                .setTargetModule("xiaou-knowledge");
        LearningAssetMergeRequest request = new LearningAssetMergeRequest();
        request.setExistingTargetId(9901L);
        request.setNote("合并到既有知识节点");

        when(candidateMapper.selectById(202L)).thenReturn(candidate);
        when(recordMapper.selectById(101L)).thenReturn(record);
        when(candidateMapper.selectByRecordId(101L)).thenReturn(List.of(candidate));
        when(knowledgeNodeService.getById(9901L)).thenReturn(new com.xiaou.knowledge.domain.KnowledgeNode());

        Long targetId = publishService.merge(9001L, 202L, request);

        assertEquals(9901L, targetId);
        assertEquals(LearningAssetCandidateStatus.PUBLISHED.name(), candidate.getStatus());
        verify(candidateMapper).updateStatus(202L, LearningAssetCandidateStatus.PUBLISHED.name(), 9901L, "合并到既有知识节点");
        verify(knowledgeNodeService, never()).createNode(any(), any());
        verify(recordMapper).update(any(LearningAssetRecord.class));
    }

    @Test
    void updateReviewCandidateShouldPersistAdminEdits() {
        LearningAssetCandidate candidate = new LearningAssetCandidate()
                .setId(202L)
                .setRecordId(101L)
                .setUserId(1001L)
                .setAssetType("knowledge_node")
                .setTitle("旧标题")
                .setContentJson("{\"title\":\"旧标题\"}")
                .setTags("Redis")
                .setStatus(LearningAssetCandidateStatus.REVIEWING.name());
        LearningAssetAdminCandidateUpdateRequest request = new LearningAssetAdminCandidateUpdateRequest();
        request.setTitle("新标题");
        request.setContentJson("{\"title\":\"新标题\"}");
        request.setTags("Redis,缓存");
        request.setDifficulty("中级");

        when(candidateMapper.selectById(202L)).thenReturn(candidate);

        publishService.updateReviewCandidate(9001L, 202L, request);

        assertEquals("新标题", candidate.getTitle());
        assertEquals("{\"title\":\"新标题\"}", candidate.getContentJson());
        assertEquals("Redis,缓存", candidate.getTags());
        assertEquals("中级", candidate.getDifficulty());
        verify(candidateMapper).updateEditable(candidate);
    }

    @Test
    void shouldBuildStatisticsSummary() {
        when(recordMapper.countAll()).thenReturn(10L);
        when(recordMapper.countSuccess()).thenReturn(8L);
        when(recordMapper.countStatus(LearningAssetRecordStatus.REVIEWING.name())).thenReturn(2L);
        when(recordMapper.countStatus(LearningAssetRecordStatus.PUBLISHED.name())).thenReturn(3L);
        when(recordMapper.countStatus(LearningAssetRecordStatus.PARTIAL_PUBLISHED.name())).thenReturn(2L);
        when(candidateMapper.countAll()).thenReturn(20L);
        when(candidateMapper.countEdited()).thenReturn(5L);
        when(candidateMapper.countByStatuses(List.of(LearningAssetCandidateStatus.REVIEWING.name(), LearningAssetCandidateStatus.PUBLISHED.name()))).thenReturn(12L);
        when(candidateMapper.countByStatuses(List.of(LearningAssetCandidateStatus.REJECTED.name()))).thenReturn(2L);
        when(recordMapper.selectSourceStatistics()).thenReturn(List.of(
                new LearningAssetStatisticsResponse.SourceStat().setSourceType("blog").setTotalCount(4L).setSuccessCount(3L)
        ));
        when(candidateMapper.selectAssetStatistics()).thenReturn(List.of(
                new LearningAssetStatisticsResponse.AssetStat()
                        .setAssetType("knowledge_node")
                        .setTotalCount(6L)
                        .setPublishedCount(2L)
                        .setReviewingCount(2L)
                        .setRejectedCount(1L)
        ));
        when(recordMapper.selectFailReasonStatistics()).thenReturn(List.of(
                new LearningAssetStatisticsResponse.FailReasonStat().setFailReason("候选资产未通过审核").setCount(2L)
        ));
        when(recordMapper.selectTopSourceStatistics()).thenReturn(List.of(
                new LearningAssetStatisticsResponse.TopSourceStat()
                        .setSourceType("blog")
                        .setSourceId(1L)
                        .setSourceTitle("Redis 缓存击穿实战")
                        .setPublishedCount(4L)
        ));

        LearningAssetStatisticsResponse response = publishService.getStatistics();

        assertEquals(10L, response.getOverview().getTotalTransforms());
        assertEquals(80.0D, response.getOverview().getTransformSuccessRate());
        assertEquals(25.0D, response.getOverview().getEditRate());
        assertEquals(16.67D, response.getOverview().getRejectRate());
        assertEquals("blog", response.getSourceStats().get(0).getSourceType());
        assertEquals("knowledge_node", response.getAssetStats().get(0).getAssetType());
        assertEquals("候选资产未通过审核", response.getFailReasonStats().get(0).getFailReason());
    }
}
