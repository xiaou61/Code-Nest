package com.xiaou.sre.controller.internal;

import com.xiaou.common.core.domain.Result;
import com.xiaou.common.utils.JsonUtils;
import com.xiaou.sre.dto.request.AlertmanagerAlert;
import com.xiaou.sre.dto.request.AlertmanagerWebhookRequest;
import com.xiaou.sre.dto.response.SreIngestionResult;
import com.xiaou.sre.service.SreAlertIngestionService;
import com.xiaou.sre.service.SreValidationException;
import com.xiaou.sre.service.model.SreAlertBatch;
import com.xiaou.sre.service.model.SreAlertRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Alertmanager 机器 webhook 入口。
 *
 * @author xiaou
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/internal/sre/alertmanager/v1")
@RequiredArgsConstructor
public class AlertmanagerWebhookController {

    private static final String SOURCE = "alertmanager";

    private final SreAlertIngestionService alertIngestionService;

    @PostMapping("/alerts")
    public ResponseEntity<Result<SreIngestionResult>> receive(
            @Valid @RequestBody AlertmanagerWebhookRequest request) {
        SreIngestionResult result = alertIngestionService.ingest(toBatch(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.success(result));
    }

    private SreAlertBatch toBatch(AlertmanagerWebhookRequest request) {
        List<SreAlertRecord> alerts = request.getAlerts().stream()
                .map(this::toRecord)
                .toList();
        return new SreAlertBatch(SOURCE, alerts);
    }

    private SreAlertRecord toRecord(AlertmanagerAlert alert) {
        return new SreAlertRecord(
                alert.getStatus(),
                alert.getLabels(),
                alert.getAnnotations(),
                alert.getStartsAt(),
                alert.getEndsAt(),
                alert.getGeneratorUrl(),
                alert.getFingerprint(),
                JsonUtils.toJsonString(alert)
        );
    }

    @ExceptionHandler(SreValidationException.class)
    public ResponseEntity<Result<Void>> handleValidation(SreValidationException exception) {
        return ResponseEntity.badRequest().body(Result.error(400, exception.getMessage()));
    }
}
