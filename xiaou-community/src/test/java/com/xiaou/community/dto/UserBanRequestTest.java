package com.xiaou.community.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserBanRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldRejectInvalidBanDurationAndOverlongReason() {
        UserBanRequest zeroDuration = request("灌水", 0);
        assertTrue(hasViolation(zeroDuration, "duration"));

        UserBanRequest tooLongDuration = request("灌水", 8761);
        assertTrue(hasViolation(tooLongDuration, "duration"));

        UserBanRequest overlongReason = request("a".repeat(201), 24);
        assertTrue(hasViolation(overlongReason, "reason"));
    }

    private boolean hasViolation(UserBanRequest request, String property) {
        return validator.validate(request).stream()
                .anyMatch(violation -> property.equals(violation.getPropertyPath().toString()));
    }

    private UserBanRequest request(String reason, Integer duration) {
        UserBanRequest request = new UserBanRequest();
        request.setReason(reason);
        request.setDuration(duration);
        return request;
    }
}
