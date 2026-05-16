package com.xiaou.common.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis工具类
 * 基于Redisson客户端实现
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedissonClient redissonClient;

    // =============================常用操作=============================

    /**
     * 指定缓存失效时间
     *
     * @param key  键
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redissonClient.getBucket(key).expire(Duration.ofSeconds(time));
            }
            return true;
        } catch (Exception e) {
            log.error("设置过期时间失败", e);
            return false;
        }
    }

    /**
     * 根据key 获取过期时间
     *
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {
        try {
            RBucket<Object> bucket = redissonClient.getBucket(key);
            return bucket.remainTimeToLive() / 1000;
        } catch (Exception e) {
            log.error("获取过期时间失败", e);
            return 0;
        }
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        try {
            return redissonClient.getBucket(key).isExists();
        } catch (Exception e) {
            log.error("判断key是否存在失败", e);
            return false;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 可以传一个值 或多个
     */
    public boolean del(String... key) {
        try {
            if (key != null && key.length > 0) {
                if (key.length == 1) {
                    return redissonClient.getBucket(key[0]).delete();
                } else {
                    return redissonClient.getKeys().delete(key) > 0;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("删除缓存失败", e);
            return false;
        }
    }

    // ============================String=============================

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        try {
            return key == null ? null : redissonClient.getBucket(key).get();
        } catch (Exception e) {
            log.error("获取缓存失败", e);
            return null;
        }
    }

    /**
     * 获取缓存并指定返回类型
     *
     * @param key 键
     * @param clazz 返回类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = get(key);
            if (value == null) {
                return null;
            }
            return (T) value;
        } catch (Exception e) {
            log.error("获取缓存失败", e);
            return null;
        }
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redissonClient.getBucket(key).set(value);
            return true;
        } catch (Exception e) {
            log.error("设置缓存失败", e);
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redissonClient.getBucket(key).set(value, Duration.ofSeconds(time));
            } else {
                redissonClient.getBucket(key).set(value);
            }
            return true;
        } catch (Exception e) {
            log.error("设置缓存失败", e);
            return false;
        }
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return 增加后的值
     */
    public long incr(String key, long delta) {
        try {
            if (delta < 0) {
                throw new RuntimeException("递增因子必须大于0");
            }
            return redissonClient.getAtomicLong(key).addAndGet(delta);
        } catch (Exception e) {
            log.error("递增失败", e);
            return 0;
        }
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几(大于0)
     * @return 减少后的值
     */
    public long decr(String key, long delta) {
        try {
            if (delta < 0) {
                throw new RuntimeException("递减因子必须大于0");
            }
            return redissonClient.getAtomicLong(key).addAndGet(-delta);
        } catch (Exception e) {
            log.error("递减失败", e);
            return 0;
        }
    }

    // ================================Map=================================

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hget(String key, String item) {
        try {
            return redissonClient.getMap(key).get(item);
        } catch (Exception e) {
            log.error("Hash获取失败", e);
            return null;
        }
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        try {
            return redissonClient.getMap(key).readAllMap();
        } catch (Exception e) {
            log.error("Hash获取所有键值失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redissonClient.getMap(key).putAll(map);
            return true;
        } catch (Exception e) {
            log.error("Hash设置多个键值失败", e);
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            RMap<Object, Object> rMap = redissonClient.getMap(key);
            rMap.putAll(map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("Hash设置多个键值并设置时间失败", e);
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redissonClient.getMap(key).put(item, value);
            return true;
        } catch (Exception e) {
            log.error("Hash设置单个键值失败", e);
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redissonClient.getMap(key).put(item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("Hash设置单个键值并设置时间失败", e);
            return false;
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public boolean hdel(String key, Object... item) {
        try {
            return redissonClient.getMap(key).fastRemove(item) > 0;
        } catch (Exception e) {
            log.error("Hash删除失败", e);
            return false;
        }
    }

    /**
     * 判断hash表中是否有该项的值
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {
        try {
            return redissonClient.getMap(key).containsKey(item);
        } catch (Exception e) {
            log.error("判断Hash是否存在键失败", e);
            return false;
        }
    }

    // ============================Set=============================

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return Set集合
     */
    public Set<Object> sGet(String key) {
        try {
            return redissonClient.getSet(key).readAll();
        } catch (Exception e) {
            log.error("Set获取所有值失败", e);
            return Collections.emptySet();
        }
    }

    /**
     * 根据value从一个set中查询,是否存在
     *
     * @param key   键
     * @param value 值
     * @return true 存在 false不存在
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return redissonClient.getSet(key).contains(value);
        } catch (Exception e) {
            log.error("Set判断是否存在值失败", e);
            return false;
        }
    }

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            if (values == null || values.length == 0) {
                return 0;
            }
            RSet<Object> set = redissonClient.getSet(key);
            return addSetValues(set, values);
        } catch (Exception e) {
            log.error("Set添加值失败", e);
            return 0;
        }
    }

    /**
     * 将set数据放入缓存
     *
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            if (values == null || values.length == 0) {
                return 0;
            }
            RSet<Object> set = redissonClient.getSet(key);
            long count = addSetValues(set, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            log.error("Set添加值并设置时间失败", e);
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     *
     * @param key 键
     * @return 长度
     */
    public long sGetSetSize(String key) {
        try {
            return redissonClient.getSet(key).size();
        } catch (Exception e) {
            log.error("Set获取长度失败", e);
            return 0;
        }
    }

    /**
     * 移除值为value的
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long setRemove(String key, Object... values) {
        try {
            if (values == null || values.length == 0) {
                return 0;
            }
            RSet<Object> set = redissonClient.getSet(key);
            long removed = 0;
            for (Object value : values) {
                if (set.remove(value)) {
                    removed++;
                }
            }
            return removed;
        } catch (Exception e) {
            log.error("Set移除值失败", e);
            return 0;
        }
    }

    // ===============================List=================================

    /**
     * 获取list缓存的内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束 0 到 -1代表所有值
     * @return List集合
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redissonClient.getList(key).range((int) start, (int) end);
        } catch (Exception e) {
            log.error("List获取范围值失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取list缓存的长度
     *
     * @param key 键
     * @return 长度
     */
    public long lGetListSize(String key) {
        try {
            return redissonClient.getList(key).size();
        } catch (Exception e) {
            log.error("List获取长度失败", e);
            return 0;
        }
    }

    /**
     * 通过索引 获取list中的值
     *
     * @param key   键
     * @param index 索引 index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return 值
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redissonClient.getList(key).get((int) index);
        } catch (Exception e) {
            log.error("List获取索引值失败", e);
            return null;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean lSet(String key, Object value) {
        try {
            redissonClient.getList(key).add(value);
            return true;
        } catch (Exception e) {
            log.error("List添加值失败", e);
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return true成功 false失败
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redissonClient.getList(key).add(value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("List添加值并设置时间失败", e);
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redissonClient.getList(key).addAll(value);
            return true;
        } catch (Exception e) {
            log.error("List添加集合失败", e);
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return true成功 false失败
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redissonClient.getList(key).addAll(value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("List添加集合并设置时间失败", e);
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return true成功 false失败
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redissonClient.getList(key).set((int) index, value);
            return true;
        } catch (Exception e) {
            log.error("List更新索引值失败", e);
            return false;
        }
    }


    // ===============================分布式锁=================================

    /**
     * 获取分布式锁
     *
     * @param lockKey 锁的key
     * @return 锁对象
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 获取公平锁
     *
     * @param lockKey 锁的key
     * @return 公平锁对象
     */
    public RLock getFairLock(String lockKey) {
        return redissonClient.getFairLock(lockKey);
    }

    /**
     * 获取读写锁
     *
     * @param lockKey 锁的key
     * @return 读写锁对象
     */
    public RReadWriteLock getReadWriteLock(String lockKey) {
        return redissonClient.getReadWriteLock(lockKey);
    }

    // ===============================其他工具方法=================================

    /**
     * 获取Redisson客户端
     *
     * @return RedissonClient
     */
    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    /**
     * 清空所有缓存
     */
    public void flushAll() {
        try {
            redissonClient.getKeys().flushall();
        } catch (Exception e) {
            log.error("清空所有缓存失败", e);
        }
    }

    /**
     * 获取所有key
     *
     * @param pattern 匹配模式
     * @return key集合
     */
    public Iterable<String> getKeys(String pattern) {
        try {
            return redissonClient.getKeys().getKeysByPattern(pattern);
        } catch (Exception e) {
            log.error("获取所有key失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据模式删除key
     *
     * @param pattern 匹配模式
     * @return 删除的key数量
     */
    public long deleteByPattern(String pattern) {
        try {
            return redissonClient.getKeys().deleteByPattern(pattern);
        } catch (Exception e) {
            log.error("根据模式删除key失败", e);
            return 0;
        }
    }

    // ================================ZSet（有序集合）=================================

    /**
     * ZSet添加元素
     *
     * @param key   键
     * @param value 值
     * @param score 分数
     * @return true成功 false失败
     */
    public boolean zAdd(String key, Object value, double score) {
        try {
            RScoredSortedSet<Object> sortedSet = redissonClient.getScoredSortedSet(key);
            return sortedSet.add(score, value);
        } catch (Exception e) {
            log.error("ZSet添加元素失败", e);
            return false;
        }
    }

    /**
     * ZSet获取指定范围的元素（按分数从小到大）
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @return 元素集合
     */
    public Collection<Object> zRange(String key, int start, int end) {
        try {
            RScoredSortedSet<Object> sortedSet = redissonClient.getScoredSortedSet(key);
            return sortedSet.valueRange(start, end);
        } catch (Exception e) {
            log.error("ZSet获取范围元素失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * ZSet获取指定范围的元素（按分数从大到小）
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @return 元素集合
     */
    public Collection<Object> zRevRange(String key, int start, int end) {
        try {
            RScoredSortedSet<Object> sortedSet = redissonClient.getScoredSortedSet(key);
            return sortedSet.valueRangeReversed(start, end);
        } catch (Exception e) {
            log.error("ZSet倒序获取范围元素失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * ZSet获取指定范围的元素及分数（按分数从大到小）
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @return 元素和分数的Map
     */
    public Map<Object, Double> zRevRangeWithScores(String key, int start, int end) {
        try {
            RScoredSortedSet<Object> sortedSet = redissonClient.getScoredSortedSet(key);
            Collection<Object> values = sortedSet.valueRangeReversed(start, end);
            Map<Object, Double> result = new java.util.LinkedHashMap<>();
            for (Object value : values) {
                Double score = sortedSet.getScore(value);
                result.put(value, score);
            }
            return result;
        } catch (Exception e) {
            log.error("ZSet倒序获取元素和分数失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * ZSet获取元素的分数
     *
     * @param key   键
     * @param value 值
     * @return 分数
     */
    public Double zScore(String key, Object value) {
        try {
            RScoredSortedSet<Object> sortedSet = redissonClient.getScoredSortedSet(key);
            return sortedSet.getScore(value);
        } catch (Exception e) {
            log.error("ZSet获取分数失败", e);
            return null;
        }
    }

    /**
     * ZSet获取集合大小
     *
     * @param key 键
     * @return 大小
     */
    public long zSize(String key) {
        try {
            return redissonClient.getScoredSortedSet(key).size();
        } catch (Exception e) {
            log.error("ZSet获取大小失败", e);
            return 0;
        }
    }

    /**
     * ZSet移除元素
     *
     * @param key    键
     * @param values 值
     * @return 移除的个数
     */
    public long zRemove(String key, Object... values) {
        try {
            if (values == null || values.length == 0) {
                return 0;
            }
            RScoredSortedSet<Object> sortedSet = redissonClient.getScoredSortedSet(key);
            long removed = 0;
            for (Object value : values) {
                if (sortedSet.remove(value)) {
                    removed++;
                }
            }
            return removed;
        } catch (Exception e) {
            log.error("ZSet移除元素失败", e);
            return 0;
        }
    }

    /**
     * ZSet移除指定排名范围的元素
     *
     * @param key   键
     * @param start 开始排名
     * @param end   结束排名
     * @return 移除的个数
     */
    public long zRemoveRange(String key, int start, int end) {
        try {
            RScoredSortedSet<Object> sortedSet = redissonClient.getScoredSortedSet(key);
            return sortedSet.removeRangeByRank(start, end);
        } catch (Exception e) {
            log.error("ZSet移除范围元素失败", e);
            return 0;
        }
    }

    private long addSetValues(RSet<Object> set, Object... values) {
        long added = 0;
        for (Object value : values) {
            if (set.add(value)) {
                added++;
            }
        }
        return added;
    }
}
