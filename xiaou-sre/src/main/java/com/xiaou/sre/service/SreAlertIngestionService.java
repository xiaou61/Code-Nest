package com.xiaou.sre.service;

import com.xiaou.sre.dto.response.SreIngestionResult;
import com.xiaou.sre.service.model.SreAlertBatch;

/**
 * Source-neutral alert ingestion interface.
 *
 * @author xiaou
 */
public interface SreAlertIngestionService {

    SreIngestionResult ingest(SreAlertBatch batch);
}
