package com.xiaou.web.growthcoach.adapter.persistence;

import com.xiaou.codepen.domain.CodePen;
import com.xiaou.codepen.mapper.CodePenMapper;
import com.xiaou.interview.mapper.InterviewQuestionSetMapper;
import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.domain.OjProblemTag;
import com.xiaou.oj.mapper.OjProblemMapper;
import com.xiaou.oj.service.OjProblemService;
import com.xiaou.web.growthcoach.port.GrowthLearningResourcePort.OjProblemData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MybatisGrowthLearningResourceAdapterTest {

    @Mock private InterviewQuestionSetMapper questionSetMapper;
    @Mock private OjProblemMapper problemMapper;
    @Mock private OjProblemService problemService;
    @Mock private CodePenMapper codePenMapper;

    private MybatisGrowthLearningResourceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MybatisGrowthLearningResourceAdapter(
                questionSetMapper,
                problemMapper,
                problemService,
                codePenMapper
        );
    }

    @Test
    void preservesAccessWhenQuestionSetMetadataIsMissing() {
        when(questionSetMapper.hasAccessPermission(3L, 7L)).thenReturn(true);
        when(questionSetMapper.selectById(3L)).thenReturn(null);

        assertThat(adapter.questionSetForUser(3L, 7L).accessible()).isTrue();
        assertThat(adapter.questionSetForUser(3L, 7L).title()).isNull();
    }

    @Test
    void mapsOjTagsAndRejectsForeignCodePenSources() {
        OjProblemTag tag = new OjProblemTag();
        tag.setName("动态规划");
        OjProblem problem = new OjProblem();
        problem.setId(21L);
        problem.setTitle("最长递增子序列");
        problem.setStatus(1);
        problem.setTags(List.of(tag));
        when(problemMapper.selectById(21L)).thenReturn(problem);

        CodePen pen = new CodePen();
        pen.setId(9L);
        pen.setUserId(8L);
        when(codePenMapper.selectById(9L)).thenReturn(pen);

        OjProblemData mapped = adapter.ojProblem(21L);
        assertThat(mapped.tags()).containsExactly("动态规划");
        assertThat(adapter.ownedCodePen(7L, 9L)).isNull();
    }
}
