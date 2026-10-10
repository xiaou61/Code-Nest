package io.github.xiaou61.account.internal.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExpiringStoreTest {

    /** 可控时钟：过期是时间行为，用真实等待测不了。 */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    @Test
    @DisplayName("存活期内可读，过期后读不到")
    void expiresAfterTtl() {
        MutableClock clock = new MutableClock();
        ExpiringStore<String> store = new ExpiringStore<>(10, clock);
        store.put("k", "v", Duration.ofMinutes(5));

        assertThat(store.get("k")).contains("v");

        clock.advance(Duration.ofMinutes(5).plusSeconds(1));

        assertThat(store.get("k")).as("过了存活期必须读不到").isEmpty();
    }

    @Test
    @DisplayName("take 取一次即删：验证码这类一次性凭据靠它保证不可重放")
    void takeRemovesEntry() {
        ExpiringStore<String> store = new ExpiringStore<>(10, Clock.systemUTC());
        store.put("k", "v", Duration.ofMinutes(5));

        assertThat(store.take("k")).contains("v");
        assertThat(store.take("k")).as("第二次必须取不到").isEmpty();
    }

    @Test
    @DisplayName("过期项会被清理任务回收")
    void purgeRemovesExpired() {
        MutableClock clock = new MutableClock();
        ExpiringStore<String> store = new ExpiringStore<>(10, clock);
        store.put("old", "v", Duration.ofMinutes(1));
        store.put("fresh", "v", Duration.ofHours(1));

        clock.advance(Duration.ofMinutes(2));

        assertThat(store.purgeExpired()).isEqualTo(1);
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("容量满时拒绝新键，而不是覆盖别人的记录或无限增长")
    void refusesNewKeyAtCapacity() {
        ExpiringStore<String> store = new ExpiringStore<>(2, Clock.systemUTC());

        assertThat(store.put("a", "1", Duration.ofMinutes(1))).isTrue();
        assertThat(store.put("b", "2", Duration.ofMinutes(1))).isTrue();
        assertThat(store.put("c", "3", Duration.ofMinutes(1))).as("超容量必须返回 false").isFalse();
        assertThat(store.get("a")).as("已有记录不能被挤掉").contains("1");
    }
}
