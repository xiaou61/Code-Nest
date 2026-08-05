package com.xiaou.common.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Raw text persistence for JSON snapshots whose Redis wire format must remain stable.
 */
public interface TextStateStore {

    Optional<String> find(String key);

    void put(String key, String value);

    void put(String key, String value, Duration ttl);

    boolean delete(String key);
}
