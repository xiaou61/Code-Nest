package com.xiaou.sre.service.model;

import java.util.List;

/**
 * Source-neutral batch accepted by the SRE alert ingestion module.
 *
 * @param source stable source key, for example {@code alertmanager}
 * @param alerts normalized alert records from the transport adapter
 */
public record SreAlertBatch(String source, List<SreAlertRecord> alerts) {
}
