package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreIncidentEvidence;
import com.xiaou.sre.mapper.SreIncidentEvidenceMapper;
import com.xiaou.sre.service.impl.SreIncidentEvidenceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreIncidentEvidenceServiceImplTest {

    @Mock
    private SreIncidentEvidenceMapper evidenceMapper;

    @Test
    void invalidIncidentIdReturnsEmptyWithoutQuery() {
        SreIncidentEvidenceServiceImpl service = new SreIncidentEvidenceServiceImpl(evidenceMapper);

        assertThat(service.listByIncidentId(0L)).isEmpty();

        verify(evidenceMapper, never()).selectByIncidentId(0L);
    }

    @Test
    void nullMapperResultIsNormalizedToEmptyList() {
        SreIncidentEvidenceServiceImpl service = new SreIncidentEvidenceServiceImpl(evidenceMapper);
        when(evidenceMapper.selectByIncidentId(11L)).thenReturn(null);

        assertThat(service.listByIncidentId(11L)).isEmpty();
    }

    @Test
    void evidenceListIsReturnedAsIs() {
        SreIncidentEvidence item = new SreIncidentEvidence();
        when(evidenceMapper.selectByIncidentId(11L)).thenReturn(List.of(item));

        assertThat(new SreIncidentEvidenceServiceImpl(evidenceMapper).listByIncidentId(11L))
                .containsExactly(item);
    }
}
