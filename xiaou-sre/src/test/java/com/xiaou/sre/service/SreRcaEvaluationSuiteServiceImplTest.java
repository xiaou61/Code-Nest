package com.xiaou.sre.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.domain.SreRcaEvaluationSuite;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteCase;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersion;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersionSnapshot;
import com.xiaou.sre.dto.request.SreRcaEvaluationSuiteVersionPublishCommand;
import com.xiaou.sre.mapper.SreRcaEvaluationCaseMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationSuiteCaseMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationSuiteMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationSuiteVersionMapper;
import com.xiaou.sre.service.impl.SreRcaEvaluationSuiteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreRcaEvaluationSuiteServiceImplTest {

    @Mock
    private SreRcaEvaluationSuiteMapper suiteMapper;

    @Mock
    private SreRcaEvaluationSuiteVersionMapper suiteVersionMapper;

    @Mock
    private SreRcaEvaluationSuiteCaseMapper suiteCaseMapper;

    @Mock
    private SreRcaEvaluationCaseMapper caseMapper;

    @Test
    @SuppressWarnings("unchecked")
    void publishingCreatesTheNextImmutableVersionWithOrderedManifest() {
        when(suiteMapper.selectByIdForUpdate(601L)).thenReturn(evaluationSuite());
        when(caseMapper.selectById(301L)).thenReturn(evaluationCase(301L));
        when(caseMapper.selectById(302L)).thenReturn(evaluationCase(302L));
        when(suiteVersionMapper.selectLatestVersionNo(601L)).thenReturn(2);
        when(suiteVersionMapper.insert(any(SreRcaEvaluationSuiteVersion.class))).thenAnswer(invocation -> {
            SreRcaEvaluationSuiteVersion version = invocation.getArgument(0);
            version.setId(701L);
            return 1;
        });
        when(suiteCaseMapper.insertBatch(any())).thenReturn(2);

        SreRcaEvaluationSuiteVersionSnapshot snapshot = service().publishVersion(
                new SreRcaEvaluationSuiteVersionPublishCommand(
                601L,
                List.of(302L, 301L),
                new BigDecimal("90"),
                new BigDecimal("80"),
                true,
                true,
                7L
        ));

        assertThat(snapshot.suite().getId()).isEqualTo(601L);
        assertThat(snapshot.version().getId()).isEqualTo(701L);
        assertThat(snapshot.version().getVersionNo()).isEqualTo(3);
        assertThat(snapshot.version().getCaseCount()).isEqualTo(2);
        assertThat(snapshot.version().getManifestSha256()).hasSize(64);
        assertThat(snapshot.version().getMinimumPassRate()).isEqualByComparingTo("90.00");
        assertThat(snapshot.version().getMinimumAverageScore()).isEqualByComparingTo("80.00");
        assertThat(snapshot.cases()).extracting(SreRcaEvaluationCase::getId)
                .containsExactly(301L, 302L);

        ArgumentCaptor<List<SreRcaEvaluationSuiteCase>> members = ArgumentCaptor.forClass(List.class);
        verify(suiteCaseMapper).insertBatch(members.capture());
        assertThat(members.getValue()).extracting(SreRcaEvaluationSuiteCase::getCaseId)
                .containsExactly(301L, 302L);
        assertThat(members.getValue()).extracting(SreRcaEvaluationSuiteCase::getCaseOrdinal)
                .containsExactly(1, 2);
        assertThat(members.getValue()).allSatisfy(
                member -> assertThat(member.getSuiteVersionId()).isEqualTo(701L));
        assertThat(members.getValue()).extracting(SreRcaEvaluationSuiteCase::getCaseContentSha256)
                .allSatisfy(hash -> assertThat(hash).hasSize(64));
    }

    @Test
    void publishingRejectsDuplicateCaseIdsBeforeWritingAnything() {
        assertThatThrownBy(() -> service().publishVersion(new SreRcaEvaluationSuiteVersionPublishCommand(
                601L,
                List.of(301L, 301L),
                new BigDecimal("100"),
                new BigDecimal("70"),
                true,
                true,
                7L
        )))
                .isInstanceOf(SreValidationException.class)
                .hasMessageContaining("重复");

        verify(suiteVersionMapper, never()).insert(any());
        verify(suiteCaseMapper, never()).insertBatch(any());
    }

    private SreRcaEvaluationSuiteServiceImpl service() {
        return new SreRcaEvaluationSuiteServiceImpl(
                suiteMapper, suiteVersionMapper, suiteCaseMapper, caseMapper);
    }

    private SreRcaEvaluationSuite evaluationSuite() {
        SreRcaEvaluationSuite suite = new SreRcaEvaluationSuite();
        suite.setId(601L);
        suite.setSuiteKey("release-readiness");
        suite.setName("发布准入");
        return suite;
    }

    private SreRcaEvaluationCase evaluationCase(Long id) {
        SreRcaEvaluationCase evaluationCase = new SreRcaEvaluationCase();
        String contextJson = "{\"caseId\":" + id + "}";
        evaluationCase.setId(id);
        evaluationCase.setContextSha256(DigestUtil.sha256Hex(contextJson));
        evaluationCase.setContextJson(contextJson);
        evaluationCase.setBaselineReportJson("{\"executiveSummary\":\"case " + id + "\"}");
        evaluationCase.setExpectedConclusion("case " + id);
        return evaluationCase;
    }
}
