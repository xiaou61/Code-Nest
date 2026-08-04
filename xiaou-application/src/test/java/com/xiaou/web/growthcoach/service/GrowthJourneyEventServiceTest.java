package com.xiaou.web.growthcoach.service;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.domain.GrowthJourneyEvent;
import com.xiaou.web.growthcoach.dto.GrowthJourneyEventRequest;
import com.xiaou.web.growthcoach.dto.GrowthJourneyEventResponse;
import com.xiaou.web.growthcoach.mapper.GrowthJourneyEventMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthJourneyEventServiceTest {

    @Mock
    private GrowthJourneyEventMapper eventMapper;

    @Test
    void acceptsDuplicateDeliveryAsAnIdempotentSuccess() {
        GrowthJourneyEventRequest request = request("PRIMARY_ACTION_SHOWN");
        when(eventMapper.insert(any()))
                .thenReturn(1)
                .thenThrow(new DuplicateKeyException("duplicate journey event"));
        GrowthJourneyEventService service = new GrowthJourneyEventService(eventMapper);

        GrowthJourneyEventResponse first = service.record(7L, request);
        GrowthJourneyEventResponse duplicate = service.record(7L, request);

        assertThat(first.isRecorded()).isTrue();
        assertThat(duplicate.isRecorded()).isFalse();
        assertThat(duplicate.getTrackingId()).isEqualTo("gpa-20260729-a1b2c3");

        ArgumentCaptor<GrowthJourneyEvent> captor = ArgumentCaptor.forClass(GrowthJourneyEvent.class);
        org.mockito.Mockito.verify(eventMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getSource()).isEqualTo("application_outcomes");
    }

    @Test
    void rejectsEventsOutsideTheBusinessFunnel() {
        GrowthJourneyEventService service = new GrowthJourneyEventService(eventMapper);

        assertThatThrownBy(() -> service.record(7L, request("ARBITRARY_EVENT")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("事件类型");
    }

    @Test
    void rejectsUnknownActionSource() {
        GrowthJourneyEventService service = new GrowthJourneyEventService(eventMapper);
        GrowthJourneyEventRequest request = request("PRIMARY_ACTION_STARTED");
        request.setActionType("ARBITRARY_ACTION");

        assertThatThrownBy(() -> service.record(7L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("行动类型");
    }

    private GrowthJourneyEventRequest request(String eventType) {
        GrowthJourneyEventRequest request = new GrowthJourneyEventRequest();
        request.setEventType(eventType);
        request.setTrackingId("gpa-20260729-a1b2c3");
        request.setActionType("APPLICATION_FOLLOW_UP");
        request.setSource("application_outcomes");
        return request;
    }
}
