package com.xiaou.chat.service;

import com.xiaou.common.cache.CacheStore;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatWebSocketTicketServiceTest {

    @Test
    void shouldConsumeTicketWithAtomicTake() {
        CacheStore cacheStore = mock(CacheStore.class);
        when(cacheStore.take("xiaou:chat:ws-ticket:valid_ticket", String.class))
                .thenReturn(Optional.of("42"));
        ChatWebSocketTicketService service = new ChatWebSocketTicketService(cacheStore);

        assertThat(service.consumeTicket("valid_ticket")).isEqualTo(42L);

        verify(cacheStore).take("xiaou:chat:ws-ticket:valid_ticket", String.class);
        verify(cacheStore, never()).delete("xiaou:chat:ws-ticket:valid_ticket");
    }

    @Test
    void shouldRejectMalformedTicketWithoutReadingRedis() {
        CacheStore cacheStore = mock(CacheStore.class);
        ChatWebSocketTicketService service = new ChatWebSocketTicketService(cacheStore);

        assertThat(service.consumeTicket("../invalid")).isNull();

        verify(cacheStore, never()).take("xiaou:chat:ws-ticket:../invalid", String.class);
    }
}
