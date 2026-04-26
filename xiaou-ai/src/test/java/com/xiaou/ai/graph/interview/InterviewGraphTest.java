package com.xiaou.ai.graph.interview;

import com.xiaou.ai.dto.interview.AnswerEvaluationResult;
import com.xiaou.ai.dto.interview.GeneratedQuestion;
import com.xiaou.ai.dto.interview.InterviewSummaryResult;
import com.xiaou.ai.rag.LlamaIndexClient;
import com.xiaou.ai.rag.LlamaIndexRetrieveResponse;
import com.xiaou.ai.scene.interview.InterviewSceneSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewGraphTest {

    @Mock
    private InterviewSceneSupport sceneSupport;

    @Mock
    private LlamaIndexClient llamaIndexClient;

    @InjectMocks
    private InterviewGraphRunner graphRunner;

    @Test
    void shouldGenerateQuestionsWithRagContext() {
        when(llamaIndexClient.isAvailable()).thenReturn(true);
        when(llamaIndexClient.retrieve(any())).thenReturn(new LlamaIndexRetrieveResponse()
                .setQuery("java")
                .setNodes(List.of(new LlamaIndexRetrieveResponse.Node()
                        .setId("doc-1")
                        .setScore(0.92)
                        .setText("Java 面试高频考点包括 HashMap、并发容器、Spring 生命周期"))));

        when(sceneSupport.generateQuestions(eq("java"), eq("中级"), eq(2), anyString()))
                .thenReturn(List.of(new GeneratedQuestion()
                        .setQuestion("请介绍一下 HashMap 的底层实现")
                        .setAnswer("数组 + 链表/红黑树")
                        .setKnowledgePoints("HashMap,数据结构")));

        List<GeneratedQuestion> result = graphRunner.runGenerateQuestions("java", "中级", 2);

        assertEquals(1, result.size());
        assertEquals("请介绍一下 HashMap 的底层实现", result.get(0).getQuestion());

        ArgumentCaptor<String> ragContextCaptor = ArgumentCaptor.forClass(String.class);
        verify(sceneSupport).generateQuestions(eq("java"), eq("中级"), eq(2), ragContextCaptor.capture());
        assertTrue(ragContextCaptor.getValue().contains("HashMap"));
    }

    @Test
    void shouldGenerateFollowUpWhenEvaluationNeedsIt() {
        when(llamaIndexClient.isAvailable()).thenReturn(false);

        AnswerEvaluationResult.Feedback feedback = new AnswerEvaluationResult.Feedback();
        feedback.setStrengths(List.of("抓住了索引命中这个重点"));
        feedback.setImprovements(List.of("缺少对索引失效场景的说明"));

        when(sceneSupport.evaluateAnswer(eq("java"), eq("中级"), eq("压力型"), anyString(), anyString(), eq(0), anyString()))
                .thenReturn(new AnswerEvaluationResult()
                        .setScore(7)
                        .setFeedback(feedback)
                        .setNextAction("followUp")
                        .setFollowUpQuestion("")
                        .setReferencePoints(List.of("索引失效"))
                        .setFallback(false));
        when(sceneSupport.generateFollowUpQuestion(eq("java"), eq("中级"), eq("压力型"), anyString(), anyString(), eq(0), anyString()))
                .thenReturn("如果联合索引没有命中最左前缀，你会怎么排查？");

        AnswerEvaluationResult result = graphRunner.runEvaluateAnswer(
                "java",
                "中级",
                "压力型",
                "你怎么看待联合索引？",
                "可以提高查询效率",
                0
        );

        assertFalse(result.isFallback());
        assertEquals("followUp", result.getNextAction());
        assertEquals("如果联合索引没有命中最左前缀，你会怎么排查？", result.getFollowUpQuestion());
        verify(sceneSupport).generateFollowUpQuestion(eq("java"), eq("中级"), eq("压力型"), anyString(), anyString(), eq(0), anyString());
    }

    @Test
    void shouldNotGenerateExtraFollowUpWhenEvaluationAlreadyContainsQuestion() {
        when(llamaIndexClient.isAvailable()).thenReturn(false);

        AnswerEvaluationResult.Feedback feedback = new AnswerEvaluationResult.Feedback();
        feedback.setStrengths(List.of("回答提到了缓存一致性核心矛盾"));
        feedback.setImprovements(List.of("还可以补充消息补偿细节"));

        when(sceneSupport.evaluateAnswer(eq("java"), eq("高级"), eq("深挖型"), anyString(), anyString(), eq(1), anyString()))
                .thenReturn(new AnswerEvaluationResult()
                        .setScore(8)
                        .setFeedback(feedback)
                        .setNextAction("followUp")
                        .setFollowUpQuestion("如果消息补偿重复消费了，你会怎么保证幂等？")
                        .setReferencePoints(List.of("消息补偿", "幂等"))
                        .setFallback(false));

        AnswerEvaluationResult result = graphRunner.runEvaluateAnswer(
                "java",
                "高级",
                "深挖型",
                "缓存一致性为什么难？",
                "因为缓存和数据库不在同一事务里，需要考虑删除失败和并发写。",
                1
        );

        assertEquals("如果消息补偿重复消费了，你会怎么保证幂等？", result.getFollowUpQuestion());
        verify(sceneSupport, never()).generateFollowUpQuestion(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void shouldGenerateSummaryWithoutRagRetrievalWhenUnavailable() {
        when(llamaIndexClient.isAvailable()).thenReturn(false);
        when(sceneSupport.generateSummary(eq("java"), eq("高级"), eq(5), eq(4), eq(1), eq(82), anyString(), anyString()))
                .thenReturn(new InterviewSummaryResult()
                        .setSummary("整体表现较强，系统设计和并发基础都不错。")
                        .setOverallLevel("优秀")
                        .setSuggestions(List.of("继续补充高并发案例", "加强性能调优总结"))
                        .setFallback(false));

        InterviewSummaryResult result = graphRunner.runGenerateSummary(
                "java",
                "高级",
                5,
                4,
                1,
                82,
                "[{\"question\":\"线程池\",\"answer\":\"...\"}]"
        );

        assertEquals("优秀", result.getOverallLevel());
        assertEquals(2, result.getSuggestions().size());
        verify(llamaIndexClient, never()).retrieve(any());
        verify(sceneSupport).generateSummary(eq("java"), eq("高级"), eq(5), eq(4), eq(1), eq(82), anyString(), anyString());
    }

    @Test
    void shouldRunDirectFollowUpGraphWithRagContext() {
        when(llamaIndexClient.isAvailable()).thenReturn(true);
        when(llamaIndexClient.retrieve(any())).thenReturn(new LlamaIndexRetrieveResponse()
                .setQuery("follow-up")
                .setNodes(List.of(new LlamaIndexRetrieveResponse.Node()
                        .setId("doc-3")
                        .setScore(0.86)
                        .setText("追问时优先围绕候选人提到的关键点、边界条件和真实案例继续深挖"))));
        when(sceneSupport.generateFollowUpQuestion(eq("java"), eq("中级"), eq("引导型"), anyString(), anyString(), eq(0), anyString()))
                .thenReturn("你刚才提到线程池复用线程，那么拒绝策略在流量突增时应该怎么选？");

        String result = graphRunner.runGenerateFollowUp(
                "java",
                "中级",
                "引导型",
                "什么是线程池？",
                "线程池可以复用线程并控制资源。",
                0
        );

        assertTrue(result.contains("拒绝策略"));
        ArgumentCaptor<String> ragContextCaptor = ArgumentCaptor.forClass(String.class);
        verify(sceneSupport).generateFollowUpQuestion(eq("java"), eq("中级"), eq("引导型"), anyString(), anyString(), eq(0), ragContextCaptor.capture());
        assertTrue(ragContextCaptor.getValue().contains("边界条件"));
    }
}
