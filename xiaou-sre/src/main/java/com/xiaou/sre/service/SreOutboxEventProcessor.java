package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreOutboxEvent;

/**
 * 在一个事务中处理单个 Outbox 事件。
 *
 * @author xiaou
 */
public interface SreOutboxEventProcessor {

    void process(SreOutboxEvent event);
}
