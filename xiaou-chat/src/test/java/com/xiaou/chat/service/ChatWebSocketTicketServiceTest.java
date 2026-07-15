package com.xiaou.chat.service;

import com.xiaou.common.cache.RedisValueStore;
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
        RedisValueStore redisValueStore = mock(RedisValueStore.class);
        when(redisValueStore.take("xiaou:chat:ws-ticket:valid_ticket", String.class))
                .thenReturn(Optional.of("42"));
        ChatWebSocketTicketService service = new ChatWebSocketTicketService(redisValueStore);

        assertThat(service.consumeTicket("valid_ticket")).isEqualTo(42L);

        verify(redisValueStore).take("xiaou:chat:ws-ticket:valid_ticket", String.class);
        verify(redisValueStore, never()).delete("xiaou:chat:ws-ticket:valid_ticket");
    }

    @Test
    void shouldRejectMalformedTicketWithoutReadingRedis() {
        RedisValueStore redisValueStore = mock(RedisValueStore.class);
        ChatWebSocketTicketService service = new ChatWebSocketTicketService(redisValueStore);

        assertThat(service.consumeTicket("../invalid")).isNull();

        verify(redisValueStore, never()).take("xiaou:chat:ws-ticket:../invalid", String.class);
    }
}
