package com.xiaou.moyu.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminCalendarEventRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptMonthDayEventDateFromAdminForm() throws Exception {
        String json = """
                {
                  "eventName": "程序员节",
                  "eventDate": "10-24",
                  "eventType": 1,
                  "description": "纪念程序员",
                  "isMajor": 1,
                  "sortOrder": 8,
                  "status": 1
                }
                """;

        AdminCalendarEventRequest request = objectMapper.readValue(json, AdminCalendarEventRequest.class);

        assertEquals("10-24", request.getEventDate());
        assertTrue(validator.validate(request).isEmpty());
    }
}
