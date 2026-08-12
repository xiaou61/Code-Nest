package com.xiaou.moment.service.impl;

import com.xiaou.common.cache.CacheStore;
import com.xiaou.moment.mapper.MomentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.OptionalLong;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MomentViewServiceImplTest {

    @Mock
    private CacheStore cacheStore;

    @Mock
    private MomentMapper momentMapper;

    private MomentViewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MomentViewServiceImpl(cacheStore, momentMapper);
    }

    @Test
    void shouldFlushCounterWithSingleDeltaUpdate() {
        when(cacheStore.keys("moment:view:*")).thenReturn(List.of("moment:view:7"));
        when(cacheStore.counter("moment:view:7")).thenReturn(OptionalLong.of(25));

        service.syncViewCountToDatabase();

        verify(momentMapper).incrementViewCountByDelta(7L, 25);
        verify(cacheStore).setCounter("moment:view:7", 0);
        verifyNoMoreInteractions(momentMapper);
    }
}
