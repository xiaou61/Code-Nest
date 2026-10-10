package io.github.xiaou61.account.internal.support;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 带存活期的进程内存储。图形验证码答案、邮箱验证码、限流计数都用它。
 *
 * <p><b>天花板（已知并接受）</b>：数据在进程内存里，因此
 * ①后端重启后全部丢失（影响只是用户需要重新获取验证码）；
 * ②**多实例部署时不可用**——同一个用户的请求打到另一个实例就找不到记录，
 * 表现为"验证码明明是对的却说错"。届时必须换成共享存储（MySQL 带 TTL 的表或 Redis）。
 *
 * <p>容量有上限，且只做惰性清理（读取时顺带删过期项）+ 由定时任务周期性清理，
 * 因此不会无界增长。
 */
public final class ExpiringStore<V> {

    private final ConcurrentHashMap<String, Entry<V>> entries = new ConcurrentHashMap<>();
    private final int maxEntries;
    private final Clock clock;

    public ExpiringStore(int maxEntries, Clock clock) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("容量上限必须为正数");
        }
        this.maxEntries = maxEntries;
        this.clock = clock;
    }

    /**
     * 写入一项。
     *
     * @return 容量已满且 key 是新的时候返回 false（调用方应把这种情况当作暂时不可用，
     *         而不是悄悄覆盖掉别人的记录）
     */
    public boolean put(String key, V value, Duration ttl) {
        if (!entries.containsKey(key) && entries.size() >= maxEntries) {
            purgeExpired();
            if (entries.size() >= maxEntries) {
                return false;
            }
        }
        entries.put(key, new Entry<>(value, clock.millis() + ttl.toMillis()));
        return true;
    }

    /** 读取一项；已过期视为不存在并顺手删除。 */
    public Optional<V> get(String key) {
        Entry<V> entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAtMillis <= clock.millis()) {
            entries.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.value);
    }

    /** 取出并删除（一次性凭据的标准用法）。 */
    public Optional<V> take(String key) {
        Entry<V> entry = entries.remove(key);
        if (entry == null || entry.expiresAtMillis <= clock.millis()) {
            return Optional.empty();
        }
        return Optional.of(entry.value);
    }

    public void remove(String key) {
        entries.remove(key);
    }

    public int purgeExpired() {
        long now = clock.millis();
        int removed = 0;
        for (var iterator = entries.entrySet().iterator(); iterator.hasNext(); ) {
            if (iterator.next().getValue().expiresAtMillis <= now) {
                iterator.remove();
                removed += 1;
            }
        }
        return removed;
    }

    public int size() {
        return entries.size();
    }

    private record Entry<V>(V value, long expiresAtMillis) {
    }
}
