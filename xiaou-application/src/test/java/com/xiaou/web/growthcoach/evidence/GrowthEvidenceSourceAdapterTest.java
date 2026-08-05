package com.xiaou.web.growthcoach.evidence;

import com.xiaou.web.growthcoach.adapter.persistence.evidence.GrowthAutopilotTaskEvidenceAdapter;
import com.xiaou.web.growthcoach.adapter.persistence.evidence.MockInterviewEvidenceAdapter;
import com.xiaou.mockinterview.domain.MockInterviewSession;
import com.xiaou.mockinterview.mapper.MockInterviewSessionMapper;
import com.xiaou.plan.domain.GrowthAutopilotTask;
import com.xiaou.plan.mapper.GrowthAutopilotTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthEvidenceSourceAdapterTest {

    @Mock
    private GrowthAutopilotTaskMapper taskMapper;
    @Mock
    private MockInterviewSessionMapper sessionMapper;

    @Test
    void projectsTaskMetricsWithoutTaskTitleOrDescription() {
        GrowthAutopilotTask task = new GrowthAutopilotTask();
        task.setId(10L);
        task.setUserId(7L);
        task.setModuleKey("java");
        task.setTitle("用户自定义任务正文");
        task.setDescription("不应写入证据索引的任务描述");
        task.setPlannedMinutes(45);
        task.setPlanVersion(3);
        task.setResourceType("question-set");
        task.setStatus("done");
        task.setCompleteTime(LocalDateTime.of(2026, 7, 20, 9, 0));
        task.setUpdateTime(LocalDateTime.of(2026, 7, 20, 9, 1));
        when(taskMapper.selectChangedForEvidence(eq(7L), isNull(), eq(0L), eq(100))).thenReturn(List.of(task));

        GrowthEvidenceProjection projection = new GrowthAutopilotTaskEvidenceAdapter(taskMapper)
                .loadChanges(7L, null, 0L, 100)
                .get(0);

        assertThat(projection.isActive()).isTrue();
        assertThat(projection.getEvidenceType()).isEqualTo("TASK_COMPLETED");
        assertThat(projection.getSummary())
                .containsEntry("plannedMinutes", 45)
                .doesNotContainKeys("title", "description", "selectionReason", "routePath");
    }

    @Test
    void projectsInterviewMetricsWithoutAiNarrative() {
        MockInterviewSession session = new MockInterviewSession();
        session.setId(20L);
        session.setUserId(7L);
        session.setDirection("java");
        session.setStatus(1);
        session.setTotalScore(86);
        session.setQuestionCount(8);
        session.setLevel(2);
        session.setInterviewType(1);
        session.setAiSummary("不应复制到成长证据中的 AI 总结");
        session.setAiSuggestion("不应复制到成长证据中的 AI 建议");
        session.setEndTime(LocalDateTime.of(2026, 7, 20, 10, 0));
        session.setUpdateTime(LocalDateTime.of(2026, 7, 20, 10, 1));
        when(sessionMapper.selectChangedForEvidence(eq(7L), isNull(), eq(0L), eq(100))).thenReturn(List.of(session));

        GrowthEvidenceProjection projection = new MockInterviewEvidenceAdapter(sessionMapper)
                .loadChanges(7L, null, 0L, 100)
                .get(0);

        assertThat(projection.isActive()).isTrue();
        assertThat(projection.getEvidenceType()).isEqualTo("INTERVIEW_SCORE");
        assertThat(projection.getSummary())
                .containsEntry("totalScore", 86)
                .doesNotContainKeys("aiSummary", "aiSuggestion");
    }
}
