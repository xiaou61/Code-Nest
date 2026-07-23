package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.mapper.SreIncidentMapper;
import com.xiaou.sre.service.impl.SreIncidentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreIncidentServiceImplTest {

    @Mock
    private SreIncidentMapper incidentMapper;

    @InjectMocks
    private SreIncidentServiceImpl service;

    @Test
    void acknowledgeMovesOpenIncidentToAcknowledged() {
        SreIncident incident = incident("OPEN");
        when(incidentMapper.selectById(11L)).thenReturn(incident);
        when(incidentMapper.acknowledge(any())).thenReturn(1);

        boolean result = service.acknowledge(11L, 7L);

        assertThat(result).isTrue();
        verify(incidentMapper).acknowledge(incident);
        assertThat(incident.getState()).isEqualTo("ACKNOWLEDGED");
        assertThat(incident.getAcknowledgedBy()).isEqualTo(7L);
    }

    @Test
    void resolvingMissingIncidentReturnsFalse() {
        when(incidentMapper.selectById(11L)).thenReturn(null);

        assertThat(service.resolve(11L, 7L)).isFalse();
    }

    @Test
    void resolvingAlreadyResolvedIncidentIsIdempotent() {
        when(incidentMapper.selectById(11L)).thenReturn(incident("RESOLVED"));

        assertThat(service.resolve(11L, 7L)).isTrue();
    }

    private SreIncident incident(String state) {
        SreIncident incident = new SreIncident();
        incident.setId(11L);
        incident.setState(state);
        return incident;
    }
}
