package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreOutboxEvent;

/**
 * 只读证据采集端口。
 *
 * @author xiaou
 */
public interface SreEvidenceCollectionService {

    void collect(SreOutboxEvent event);
}
