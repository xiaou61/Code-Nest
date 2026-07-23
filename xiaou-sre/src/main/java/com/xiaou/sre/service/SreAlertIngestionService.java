package com.xiaou.sre.service;

import com.xiaou.sre.dto.request.AlertmanagerWebhookRequest;
import com.xiaou.sre.dto.response.SreIngestionResult;

/**
 * Alertmanager 事件接收服务。
 *
 * @author xiaou
 */
public interface SreAlertIngestionService {

    SreIngestionResult ingest(AlertmanagerWebhookRequest request);
}
