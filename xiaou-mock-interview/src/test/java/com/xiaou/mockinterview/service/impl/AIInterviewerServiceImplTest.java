package com.xiaou.mockinterview.service.impl;

import com.xiaou.ai.service.AiInterviewService;
import com.xiaou.mockinterview.domain.MockInterviewSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIInterviewerServiceImplTest {

    @Mock
    private AiInterviewService aiInterviewService;

    @InjectMocks
    private AIInterviewerServiceImpl aiInterviewerService;

    @Test
    void shouldDelegateFollowUpGenerationToAiInterviewService() {
        when(aiInterviewService.generateFollowUpQuestion(eq("java"), eq("中级"), eq("标准型"), anyString(), anyString(), eq(1)))
                .thenReturn("如果线程池队列堆积了，你会优先排查哪些指标？");

        String result = aiInterviewerService.generateFollowUpQuestion(
                buildSession(),
                "什么是线程池？",
                "线程池可以复用线程，提高并发效率。",
                1
        );

        assertEquals("如果线程池队列堆积了，你会优先排查哪些指标？", result);
        verify(aiInterviewService).generateFollowUpQuestion(eq("java"), eq("中级"), eq("标准型"), anyString(), anyString(), eq(1));
    }

    @Test
    void shouldFallbackToLocalFollowUpWhenAiThrowsException() {
        when(aiInterviewService.generateFollowUpQuestion(eq("java"), eq("中级"), eq("标准型"), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("ai down"));

        String result = aiInterviewerService.generateFollowUpQuestion(
                buildSession(),
                "什么是 JVM？",
                "就是 Java 虚拟机",
                0
        );

        assertTrue(result.contains("展开说说") || result.contains("例子") || result.contains("边界情况"));
    }

    private MockInterviewSession buildSession() {
        return new MockInterviewSession()
                .setDirection("java")
                .setLevel(2)
                .setStyle(2)
                .setQuestionCount(5);
    }
}
