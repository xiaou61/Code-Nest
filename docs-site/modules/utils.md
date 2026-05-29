# 工具类详解

公共底座（`xiaou-common`）提供了 14 个工具类，覆盖并发控制、线程池管理、Redis 操作、JSON 处理、敏感词检测、通知发送、分页查询等基础能力。本文档详细解析每个工具类的功能、核心方法、使用示例和注意事项。

## 工具类总览

| 工具类 | 行数 | 职责 | 核心能力 |
|--------|------|------|---------|
| `ConcurrentUtils` | 1043 | 并发工具集 | 限流器、熔断器、分段锁、计数器、同步屏障 |
| `ThreadPoolUtils` | 1206 | 线程池工具 | 线程池管理、异步任务、批量并行、超时控制、重试机制 |
| `RedisUtil` | 872 | Redis 工具 | String、Hash、List、Set、Sorted Set、分布式锁 |
| `NotificationUtil` | 378 | 通知工具 | 系统公告、个人消息、模板消息、批量发送 |
| `SensitiveWordUtils` | 374 | 敏感词工具 | 敏感词检测、替换、批量检测、缓存 |
| `JsonUtils` | 280 | JSON 工具 | 对象序列化、反序列化、类型转换 |
| `ManualPageHelper` | 162 | 手动分页 | 内存分页、排序、筛选 |
| `PageHelper` | 144 | 分页工具 | PageHelper 分页插件封装 |
| `DateHelper` | 174 | 日期工具 | 日期格式化、解析、计算 |
| `RelativeTimeUtil` | 89 | 相对时间 | "刚刚"、"3分钟前"、"2小时前" |
| `PasswordUtil` | 84 | 密码工具 | BCrypt 加密、验证 |
| `IPUtil` | 47 | IP 工具 | 获取客户端真实 IP |
| `AvatarUtils` | 59 | 头像工具 | 默认头像、头像 URL 处理 |
| `NotificationCacheUtil` | 37 | 通知缓存 | 未读数缓存管理 |

---

## 一、ConcurrentUtils（并发工具集）

**源码**：`ConcurrentUtils.java`（1043 行）

全站最大的工具类，提供限流、熔断、锁、计数器、延迟值等并发原语。

### 1.1 RateLimiter（令牌桶限流）

```java
// 获取或创建限流器（每秒 100 个请求）
RateLimiter rateLimiter = ConcurrentUtils.getRateLimiter("api-limit", 100);

// 非阻塞获取令牌
if (rateLimiter.tryAcquire()) {
    // 执行业务逻辑
} else {
    // 被限流，返回错误
}

// 阻塞获取令牌（等待直到获取成功）
rateLimiter.acquire();

// 带超时的阻塞获取
boolean acquired = rateLimiter.tryAcquire(1, 1000, TimeUnit.MILLISECONDS);

// 带限流执行任务
rateLimiter.executeWithLimit(() -> {
    return doSomething();
});
```

**核心方法**：

| 方法 | 说明 | 阻塞 |
|------|------|------|
| `tryAcquire()` | 非阻塞获取令牌 | 否 |
| `tryAcquire(int permits)` | 非阻塞获取指定数量令牌 | 否 |
| `tryAcquire(int permits, long timeout, TimeUnit unit)` | 带超时的阻塞获取 | 是 |
| `acquire()` | 阻塞获取令牌 | 是 |
| `executeWithLimit(Supplier<T>)` | 带限流执行任务 | 是 |
| `tryExecuteWithLimit(Supplier<T>)` | 非阻塞带限流执行任务 | 否 |

**注意事项**：
- `acquire()` 方法使用 `Thread.sleep(1)` 循环等待，高并发时可能浪费 CPU
- 同一个限流器实例的所有调用会串行化（使用 `synchronized`）

### 1.2 SlidingWindowRateLimiter（滑动窗口限流）

```java
// 创建滑动窗口限流器（每秒 1000 个请求，窗口大小 100ms）
SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(1000, 100, 10);

// 尝试获取令牌
if (limiter.tryAcquire()) {
    // 执行业务逻辑
}

// 获取当前窗口内的请求数
long currentCount = limiter.getCurrentCount();
```

**核心方法**：

| 方法 | 说明 |
|------|------|
| `tryAcquire()` | 尝试获取令牌 |
| `getCurrentCount()` | 获取当前窗口内的请求数 |

**注意事项**：
- 窗口数组使用 `AtomicLong[]`，每个元素是一个独立的计数器
- 当 `windowSizeMs = 1000` 时，数组长度为 1000，内存开销较大

### 1.3 CircuitBreaker（熔断器）

```java
// 获取或创建熔断器（失败阈值 5，重置超时 60 秒）
CircuitBreaker circuitBreaker = ConcurrentUtils.getCircuitBreaker("api-circuit", 5, 60000);

// 执行带熔断保护的任务
try {
    T result = circuitBreaker.execute(() -> {
        return doSomething();
    });
} catch (CircuitBreakerOpenException e) {
    // 熔断器已打开，拒绝请求
}

// 执行带降级的任务
T result = circuitBreaker.executeWithFallback(
    () -> doSomething(),           // 主逻辑
    () -> getDefaultValue()        // 降级逻辑
);

// 获取熔断器状态
CircuitBreaker.State state = circuitBreaker.getState(); // CLOSED, OPEN, HALF_OPEN

// 强制重置熔断器
circuitBreaker.reset();
```

**核心方法**：

| 方法 | 说明 |
|------|------|
| `execute(Supplier<T>)` | 执行带熔断保护的任务 |
| `executeWithFallback(Supplier<T>, Supplier<T>)` | 执行带降级的任务 |
| `allowRequest()` | 是否允许请求 |
| `onSuccess()` | 成功回调 |
| `onFailure()` | 失败回调 |
| `getState()` | 获取当前状态 |
| `reset()` | 强制重置熔断器 |

**状态机**：
```
CLOSED → (失败次数 >= 阈值) → OPEN
OPEN → (超过重置超时) → HALF_OPEN
HALF_OPEN → (请求成功) → CLOSED
HALF_OPEN → (请求失败) → OPEN
```

### 1.4 StripedLock（分段锁）

```java
// 获取或创建分段锁（16 个锁实例）
StripedLock stripedLock = ConcurrentUtils.getStripedLock("user-lock", 16);

// 使用指定 key 的锁执行任务
T result = stripedLock.withLock(userId, () -> {
    return doSomethingWithUser(userId);
});

// 获取指定 key 的锁
Lock lock = stripedLock.getLock(userId);
lock.lock();
try {
    // 执行业务逻辑
} finally {
    lock.unlock();
}
```

**核心方法**：

| 方法 | 说明 |
|------|------|
| `withLock(Object key, Supplier<T>)` | 使用指定 key 的锁执行任务 |
| `getLock(Object key)` | 获取指定 key 的锁 |

**注意事项**：
- 分段锁用 key 的 hashCode 对 2 的幂取模来选择锁实例
- 不同 key 通常会分散到不同锁上，减少锁竞争

### 1.5 其他并发原语

```java
// 高性能计数器
ConcurrentUtils.Counter counter = new ConcurrentUtils.Counter();
counter.increment();
counter.add(10);
long count = counter.get();

// 并发统计器
ConcurrentUtils.Statistics stats = new ConcurrentUtils.Statistics();
stats.record(100);
stats.record(200);
long avg = (long) stats.getAverage();

// 线程安全的懒加载
ConcurrentUtils.Lazy<ExpensiveObject> lazy = ConcurrentUtils.lazy(() -> {
    return new ExpensiveObject();
});
ExpensiveObject obj = lazy.get(); // 首次调用时初始化

// 带过期时间的缓存值
ConcurrentUtils.ExpiringValue<String> expiringValue = ConcurrentUtils.expiringValue(
    () -> fetchFromDb(),
    Duration.ofMinutes(5)
);
String value = expiringValue.get(); // 5 分钟内返回缓存值

// 只执行一次的任务
ConcurrentUtils.Once once = ConcurrentUtils.once();
once.run(() -> {
    initSomething(); // 只会执行一次
});
```

---

## 二、ThreadPoolUtils（线程池工具）

**源码**：`ThreadPoolUtils.java`（1206 行）

线程池管理工具，提供多种预配置线程池、异步任务执行、批量并行处理、超时控制、重试机制等功能。

### 2.1 预配置线程池

```java
// 获取通用线程池
ExecutorService commonExecutor = ThreadPoolUtils.getCommonExecutor();

// 获取 IO 密集型线程池
ExecutorService ioExecutor = ThreadPoolUtils.getIoExecutor();

// 获取 CPU 密集型线程池
ExecutorService cpuExecutor = ThreadPoolUtils.getCpuExecutor();

// 获取调度线程池
ScheduledExecutorService scheduler = ThreadPoolUtils.getScheduler();

// 获取虚拟线程执行器（Java 21+）
ExecutorService virtualExecutor = ThreadPoolUtils.getVirtualExecutor();
```

**线程池配置**：

| 线程池 | 核心线程 | 最大线程 | 队列大小 | 适用场景 |
|--------|---------|---------|---------|---------|
| COMMON | CPU 核数 | CPU×2 | 1000 | 通用计算 |
| IO | CPU×2 | CPU×4 | 2000 | IO 密集 |
| CPU | CPU 核数 | CPU 核数 | 500 | CPU 密集 |
| SCHEDULER | 2 | 4 | 延迟队列 | 定时任务 |

### 2.2 异步任务执行

```java
// 异步执行任务（无返回值）
CompletableFuture<Void> future = ThreadPoolUtils.runAsync(() -> {
    doSomething();
});

// 异步执行任务（有返回值）
CompletableFuture<String> future = ThreadPoolUtils.supplyAsync(() -> {
    return "result";
});

// 异步执行 IO 密集型任务
CompletableFuture<String> future = ThreadPoolUtils.supplyAsyncIO(() -> {
    return fetchFromDb();
});

// 异步执行 CPU 密集型任务
CompletableFuture<Integer> future = ThreadPoolUtils.supplyAsyncCPU(() -> {
    return calculate();
});

// 使用虚拟线程异步执行任务
CompletableFuture<String> future = ThreadPoolUtils.supplyAsyncVirtual(() -> {
    return fetchData();
});
```

### 2.3 批量并行执行

```java
// 并行执行多个任务
List<String> results = ThreadPoolUtils.parallel(
    () -> fetchFromDb1(),
    () -> fetchFromDb2(),
    () -> fetchFromDb3()
);

// 并行处理集合
List<User> users = getUsers();
List<UserDTO> dtos = ThreadPoolUtils.parallelMap(users, user -> {
    return convertToDTO(user);
});

// 并行处理集合（使用 IO 线程池）
List<UserDTO> dtos = ThreadPoolUtils.parallelMapIO(users, user -> {
    return fetchUserDetail(user.getId());
});

// 并行处理集合（带超时）
List<UserDTO> dtos = ThreadPoolUtils.parallelMapWithTimeout(users, user -> {
    return fetchUserDetail(user.getId());
}, 5, TimeUnit.SECONDS);

// 并行处理集合（限制并发数）
List<UserDTO> dtos = ThreadPoolUtils.parallelMapWithLimit(users, user -> {
    return fetchUserDetail(user.getId());
}, 10); // 最多 10 个并发

// 并行处理集合（快速失败模式）
List<UserDTO> dtos = ThreadPoolUtils.parallelMapFailFast(users, user -> {
    return fetchUserDetail(user.getId());
});
```

### 2.4 超时控制

```java
// 带超时的异步执行
CompletableFuture<String> future = ThreadPoolUtils.supplyAsyncWithTimeout(
    () -> fetchData(),
    5, TimeUnit.SECONDS
);

// 带超时和默认值的异步执行
CompletableFuture<String> future = ThreadPoolUtils.supplyAsyncWithDefault(
    () -> fetchData(),
    5, TimeUnit.SECONDS,
    "default-value"
);

// 同步执行任务（带超时）
String result = ThreadPoolUtils.executeWithTimeout(
    () -> fetchData(),
    5, TimeUnit.SECONDS
);
```

### 2.5 重试机制

```java
// 带重试的任务执行
String result = ThreadPoolUtils.executeWithRetry(
    () -> fetchData(),
    3,      // 重试次数
    1000    // 重试间隔（毫秒）
);

// 带指数退避重试的任务执行
String result = ThreadPoolUtils.executeWithExponentialBackoff(
    () -> fetchData(),
    3,      // 重试次数
    500,    // 初始延迟（毫秒）
    10000   // 最大延迟（毫秒）
);

// 带条件重试的任务执行
String result = ThreadPoolUtils.executeWithConditionalRetry(
    () -> fetchData(),
    3,      // 重试次数
    1000,   // 重试间隔（毫秒）
    e -> e instanceof IOException  // 只重试 IOException
);
```

### 2.6 定时调度

```java
// 延迟执行任务
ThreadPoolUtils.schedule(() -> {
    doSomething();
}, 5, TimeUnit.SECONDS);

// 固定频率执行任务
ThreadPoolUtils.scheduleAtFixedRate(() -> {
    doSomething();
}, 0, 1, TimeUnit.MINUTES); // 每分钟执行一次

// 固定延迟执行任务
ThreadPoolUtils.scheduleWithFixedDelay(() -> {
    doSomething();
}, 0, 1, TimeUnit.MINUTES); // 每次执行后延迟 1 分钟
```

### 2.7 线程池监控

```java
// 获取线程池状态
Map<String, Object> stats = ThreadPoolUtils.getPoolStats(commonExecutor);
// 返回: corePoolSize, maximumPoolSize, activeCount, poolSize, taskCount, etc.

// 获取所有线程池状态
Map<String, Map<String, Object>> allStats = ThreadPoolUtils.getAllPoolStats();

// 动态调整线程池参数
ThreadPoolUtils.setCorePoolSize(commonExecutor, 10);
ThreadPoolUtils.setMaxPoolSize(commonExecutor, 20);

// 预热线程池
ThreadPoolUtils.prestartAllCoreThreads(commonExecutor);
```

### 2.8 异步回调链

```java
// 创建异步任务链
String result = ThreadPoolUtils.asyncChain(() -> fetchData())
    .thenApply(data -> transform(data))
    .thenAccept(data -> log.info("Result: {}", data))
    .timeout(5, TimeUnit.SECONDS)
    .get();
```

---

## 三、RedisUtil（Redis 工具）

**源码**：`RedisUtil.java`（872 行）

基于 Redisson 客户端的 Redis 工具类，封装了 String、Hash、List、Set、Sorted Set、分布式锁等操作。

### 3.1 基础操作

```java
// 设置过期时间
redisUtil.expire("key", 3600); // 1 小时

// 获取过期时间
long ttl = redisUtil.getExpire("key");

// 判断 key 是否存在
boolean exists = redisUtil.hasKey("key");

// 删除缓存
redisUtil.del("key");
redisUtil.del("key1", "key2", "key3"); // 批量删除
```

### 3.2 String 操作

```java
// 获取缓存
Object value = redisUtil.get("key");
String str = redisUtil.get("key", String.class);

// 设置缓存
redisUtil.set("key", "value");
redisUtil.set("key", "value", 3600); // 带过期时间

// 递增/递减
long count = redisUtil.incr("counter", 1);
long count = redisUtil.decr("counter", 1);
```

### 3.3 Hash 操作

```java
// 获取 Hash 中的值
Object value = redisUtil.hget("hash-key", "field");

// 设置 Hash 中的值
redisUtil.hset("hash-key", "field", "value", 3600);

// 删除 Hash 中的值
redisUtil.hdel("hash-key", "field1", "field2");

// 判断 Hash 中是否存在字段
boolean exists = redisUtil.hHasKey("hash-key", "field");

// 获取 Hash 中的所有值
Map<Object, Object> map = redisUtil.hmget("hash-key");
```

### 3.4 List 操作

```java
// 获取 List 中的元素
List<Object> list = redisUtil.lGet("list-key", 0, -1); // 获取所有

// 获取 List 的长度
long size = redisUtil.lGetListSize("list-key");

// 从左侧推入元素
redisUtil.lSet("list-key", "value");
redisUtil.lSet("list-key", "value", 3600); // 带过期时间

// 从右侧弹出元素
Object value = redisUtil.rightPop("list-key");

// 移除元素
long removed = redisUtil.lRemove("list-key", 1, "value");
```

### 3.5 Set 操作

```java
// 获取 Set 中的所有成员
Set<Object> members = redisUtil.sGet("set-key");

// 获取 Set 的大小
long size = redisUtil.sGetSetSize("set-key");

// 判断是否是 Set 的成员
boolean isMember = redisUtil.sHasKey("set-key", "value");

// 添加元素到 Set
redisUtil.sSet("set-key", "value1", "value2");
redisUtil.sSet("set-key", "value", 3600); // 带过期时间

// 从 Set 中移除元素
redisUtil.setRemove("set-key", "value1", "value2");
```

### 3.6 Sorted Set 操作

```java
// 获取 Sorted Set 中的元素（按分数排序）
Set<Object> range = redisUtil.zRange("zset-key", 0, -1);

// 获取指定分数范围内的元素
Set<Object> rangeByScore = redisUtil.zRangeByScore("zset-key", 0, 100);

// 获取元素的分数
double score = redisUtil.zScore("zset-key", "value");

// 获取元素的排名
long rank = redisUtil.zRank("zset-key", "value");

// 添加元素
redisUtil.zAdd("zset-key", "value", 100.0);
redisUtil.zAdd("zset-key", "value", 100.0, 3600); // 带过期时间

// 递增元素的分数
double newScore = redisUtil.zIncrementScore("zset-key", "value", 10);

// 移除元素
redisUtil.zRemove("zset-key", "value1", "value2");

// 获取 Sorted Set 的大小
long size = redisUtil.zGetSetSize("zset-key");
```

### 3.7 分布式锁

```java
// 获取分布式锁
RLock lock = redisUtil.getLock("lock-key");

// 尝试获取锁（非阻塞）
boolean acquired = lock.tryLock();

// 尝试获取锁（带超时）
boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);

// 获取锁（阻塞）
lock.lock();

// 释放锁
lock.unlock();

// 使用锁执行任务
T result = redisUtil.withLock("lock-key", 3, 10, () -> {
    return doSomething();
});
```

### 3.8 管道操作

```java
// 使用管道批量执行操作
redisUtil.executePipelined(items, (item, rbucket) -> {
    rbucket.set(item);
    rbucket.expire(Duration.ofMinutes(5));
});
```

---

## 四、NotificationUtil（通知工具）

**源码**：`NotificationUtil.java`（378 行）

消息通知工具类，封装了系统公告、个人消息、模板消息、批量发送等操作。

### 4.1 系统公告

```java
// 发送系统公告（全站广播）
NotificationUtil.sendAnnouncement(
    "系统维护通知",
    "系统将于今晚 22:00 进行维护，预计耗时 2 小时。",
    "HIGH" // 优先级：HIGH/MEDIUM/LOW
);
```

### 4.2 个人消息

```java
// 发送个人消息
NotificationUtil.sendPersonalMessage(
    userId,
    "欢迎加入 Code Nest",
    "感谢您注册 Code Nest 学习社区！"
);

// 发送个人消息（带类型）
NotificationUtil.sendPersonalMessage(
    userId,
    "评论通知",
    "用户 xxx 评论了您的帖子",
    "COMMENT" // 消息类型
);
```

### 4.3 模板消息

```java
// 发送模板消息
Map<String, Object> params = new HashMap<>();
params.put("username", "张三");
params.put("score", 95);

NotificationUtil.sendTemplateMessage(
    userId,
    "EXAM_RESULT", // 模板代码
    params
);
```

### 4.4 批量发送

```java
// 批量发送消息
List<Long> userIds = Arrays.asList(1L, 2L, 3L);
NotificationUtil.sendBatchMessage(
    userIds,
    "系统通知",
    "您的积分已到账！"
);
```

### 4.5 异步发送

```java
// 异步发送消息（推荐使用）
NotificationUtil.sendMessageAsync(
    userId,
    "通知标题",
    "通知内容"
);
```

### 4.6 业务场景消息

```java
// 发送系统消息（注册欢迎、密码重置等）
NotificationUtil.sendSystemMessage(
    userId,
    "密码重置成功",
    "您的密码已重置成功，请使用新密码登录。"
);

// 发送社区消息（评论、点赞等）
NotificationUtil.sendCommunityMessage(
    userId,
    "评论通知",
    "用户 xxx 评论了您的帖子",
    "123" // 帖子 ID
);

// 发送审核消息（内容审核结果）
NotificationUtil.sendAuditMessage(
    userId,
    "审核结果",
    "您的帖子已通过审核",
    "456" // 内容 ID
);

// 发送积分消息（积分变动通知）
NotificationUtil.sendPointsMessage(
    userId,
    "积分到账",
    "您获得 10 积分奖励"
);
```

---

## 五、SensitiveWordUtils（敏感词工具）

**源码**：`SensitiveWordUtils.java`（374 行）

敏感词检测工具类，封装了敏感词检测、替换、批量检测等操作，支持缓存。

### 5.1 敏感词检测

```java
// 检测文本中的敏感词
SensitiveWordUtils.SensitiveCheckResult result = SensitiveWordUtils.checkText(
    "这是一段测试文本",
    "community",  // 模块名称
    123L,         // 业务 ID
    456L          // 用户 ID
);

if (result.getHit()) {
    // 包含敏感词
    String processedText = result.getProcessedText(); // 处理后的文本
    boolean allowed = result.getAllowed(); // 是否允许发布
    int riskLevel = result.getRiskLevel(); // 风险等级
    String action = result.getAction(); // 处理动作
}
```

### 5.2 敏感词替换

```java
// 替换文本中的敏感词
String processedText = SensitiveWordUtils.replaceSensitiveWords(
    "这是一段包含敏感词的文本",
    "community"
);
```

### 5.3 批量检测

```java
// 批量检测敏感词
List<String> texts = Arrays.asList("文本1", "文本2", "文本3");
Map<String, SensitiveWordUtils.SensitiveCheckResult> results =
    SensitiveWordUtils.checkTextBatch(texts, "community");

// 遍历结果
results.forEach((text, result) -> {
    if (result.getHit()) {
        // 处理包含敏感词的文本
    }
});
```

### 5.4 快速检测

```java
// 检测是否包含敏感词（快速）
boolean contains = SensitiveWordUtils.containsSensitiveWords(
    "这是一段测试文本",
    "community"
);

// 检测是否允许发布
boolean allowed = SensitiveWordUtils.isAllowed(
    "这是一段测试文本",
    "community",
    123L,  // 业务 ID
    456L   // 用户 ID
);
```

### 5.5 缓存机制

```java
// 清除缓存
SensitiveWordUtils.clearCache();

// 清除指定模块的缓存
SensitiveWordUtils.clearCache("community");

// 获取缓存统计信息
Map<String, Object> stats = SensitiveWordUtils.getCacheStats();
```

---

## 六、JsonUtils（JSON 工具）

**源码**：`JsonUtils.java`（280 行）

JSON 工具类，封装了 Jackson 和 FastJson2 的序列化、反序列化操作。

### 6.1 序列化

```java
// 对象转 JSON 字符串
String json = JsonUtils.toJsonString(user);

// List 转 JSON 字符串
String json = JsonUtils.listToJson(Arrays.asList("a", "b", "c"));

// Integer List 转 JSON 字符串
String json = JsonUtils.integerListToJson(Arrays.asList(1, 2, 3));
```

### 6.2 反序列化

```java
// JSON 字符串转对象
User user = JsonUtils.parseObject(json, User.class);

// JSON 字符串转 List
List<User> users = JsonUtils.parseArray(json, User.class);

// JSON 字符串转 Map
Map<String, Object> map = JsonUtils.parseMap(json);

// JSON 字符串转 String Map
Map<String, String> map = JsonUtils.parseStringMap(json);

// JSON 字符串转 String List
List<String> list = JsonUtils.jsonToStringList(json);

// JSON 字符串转 Integer List
List<Integer> list = JsonUtils.jsonToIntegerList(json);
```

### 6.3 JSON 处理

```java
// 格式化 JSON
String formatted = JsonUtils.formatJson(json);

// 压缩 JSON
String compressed = JsonUtils.compressJson(json);

// 验证 JSON 格式
boolean valid = JsonUtils.isValidJson(json);
```

---

## 七、PageHelper（分页工具）

**源码**：`PageHelper.java`（144 行）

基于 PageHelper 分页插件的统一分页工具。

### 7.1 基本分页

```java
// 分页查询
PageResult<User> result = PageHelper.doPage(1, 10, () -> {
    return userMapper.selectAll();
});

// 获取分页信息
int pageNum = result.getPageNum();
int pageSize = result.getPageSize();
long total = result.getTotal();
List<User> records = result.getRecords();
boolean hasNext = result.isHasNext();
```

### 7.2 带排序的分页

```java
// 带排序的分页查询
PageResult<User> result = PageHelper.doPage(1, 10, "create_time desc", () -> {
    return userMapper.selectAll();
});
```

### 7.3 分页结果转换

```java
// 分页结果转换
PageResult<UserDTO> result = PageHelper.doPage(1, 10, () -> {
    return userMapper.selectAll();
}).map(user -> convertToDTO(user));
```

---

## 八、ManualPageHelper（手动分页）

**源码**：`ManualPageHelper.java`（162 行）

内存分页工具，适用于数据量较小、需要在内存中分页的场景。

### 8.1 基本分页

```java
// 内存分页
List<User> allUsers = getAllUsers();
PageResult<User> result = ManualPageHelper.doPage(1, 10, allUsers);
```

### 8.2 带筛选的分页

```java
// 带筛选的内存分页
List<User> allUsers = getAllUsers();
PageResult<User> result = ManualPageHelper.doPage(1, 10, allUsers, user -> {
    return user.getStatus() == 1; // 只返回状态为 1 的用户
});
```

### 8.3 带排序的分页

```java
// 带排序的内存分页
List<User> allUsers = getAllUsers();
PageResult<User> result = ManualPageHelper.doPage(1, 10, allUsers, null,
    Comparator.comparing(User::getCreateTime).reversed()
);
```

---

## 九、DateHelper（日期工具）

**源码**：`DateHelper.java`（174 行）

日期工具类，封装了日期格式化、解析、计算等操作。

### 9.1 格式化

```java
// 格式化日期时间
String dateTimeStr = DateHelper.formatDateTime(new Date()); // "2024-01-01 12:00:00"

// 格式化日期
String dateStr = DateHelper.formatDate(new Date()); // "2024-01-01"

// 格式化当前时间
String now = DateHelper.formatCurrentDateTime();
```

### 9.2 解析

```java
// 解析日期时间
Date dateTime = DateHelper.parseDateTime("2024-01-01 12:00:00");

// 解析日期
Date date = DateHelper.parseDate("2024-01-01");
```

### 9.3 计算

```java
// 计算两个日期之间的天数
long days = DateHelper.betweenDays(startDate, endDate);

// 获取 N 天后的日期
Date futureDate = DateHelper.addDays(new Date(), 7);

// 获取 N 小时后的日期
Date futureDate = DateHelper.addHours(new Date(), 24);

// 判断是否过期
boolean expired = DateHelper.isExpired(expireDate);
```

---

## 十、RelativeTimeUtil（相对时间工具）

**源码**：`RelativeTimeUtil.java`（89 行）

相对时间工具类，将时间转换为"刚刚"、"3分钟前"、"2小时前"等格式。

### 10.1 基本使用

```java
// LocalDateTime 转相对时间
LocalDateTime dateTime = LocalDateTime.now().minusMinutes(5);
String relative = RelativeTimeUtil.format(dateTime); // "5分钟前"

// 字符串转相对时间
String relative = RelativeTimeUtil.format("2024-01-01 12:00:00");
```

### 10.2 时间范围

```
- 1 分钟内：刚刚
- 60 分钟内：X 分钟前
- 24 小时内：X 小时前
- 7 天内：X 天前
- 同一年：MM-dd
- 超过 1 年：yyyy-MM-dd
```

---

## 十一、PasswordUtil（密码工具）

**源码**：`PasswordUtil.java`（84 行)

密码工具类，基于 BCrypt 实现密码加密和验证。

### 11.1 加密

```java
// 加密密码（默认 10 轮）
String hashed = PasswordUtil.encode("123456");

// 加密密码（自定义轮数）
String hashed = PasswordUtil.encode("123456", 12);
```

### 11.2 验证

```java
// 验证密码
boolean matches = PasswordUtil.matches("123456", hashed);
```

---

## 十二、IPUtil（IP 工具）

**源码**：`IPUtil.java`（47 行）

IP 工具类，用于获取客户端真实 IP 地址。

### 12.1 获取真实 IP

```java
// 获取客户端真实 IP
String ip = IPUtil.getIpAddress(request);
```

**IP 获取优先级**：
1. `X-Forwarded-For`（取第一个）
2. `Proxy-Client-IP`
3. `WL-Proxy-Client-IP`
4. `HTTP_CLIENT_IP`
5. `HTTP_X_FORWARDED_FOR`
6. `request.getRemoteAddr()`

**注意事项**：
- `X-Forwarded-For` 可被客户端伪造
- 生产环境应配置 Nginx `set_real_ip_from` 只信任代理添加的值

---

## 十三、AvatarUtils（头像工具）

**源码**：`AvatarUtils.java`（59 行）

头像工具类，处理默认头像和头像 URL。

### 13.1 获取头像

```java
// 获取用户头像（如果为空返回默认头像）
String avatar = AvatarUtils.getUserAvatar(user.getAvatar(), user.getId());

// 获取默认头像
String defaultAvatar = AvatarUtils.getDefaultAvatar(userId);

// 判断是否为默认头像
boolean isDefault = AvatarUtils.isDefaultAvatar(avatar);
```

---

## 十四、NotificationCacheUtil（通知缓存工具）

**源码**：`NotificationCacheUtil.java`（37 行)

通知缓存工具类，管理未读通知数缓存。

### 14.1 未读数管理

```java
// 获取未读通知数
int unreadCount = notificationCacheUtil.getUnreadCount(userId);

// 清除未读数缓存
notificationCacheUtil.clearUnreadCountCache(userId);

// 增加未读数
notificationCacheUtil.incrementUnreadCount(userId);

// 减少未读数
notificationCacheUtil.decrementUnreadCount(userId);
```

---

## 使用建议

### 1. 选择合适的工具类

| 场景 | 推荐工具类 |
|------|-----------|
| 限流 | `ConcurrentUtils.RateLimiter` 或 `SlidingWindowRateLimiter` |
| 熔断 | `ConcurrentUtils.CircuitBreaker` |
| 分布式锁 | `RedisUtil.getLock()` 或 `ConcurrentUtils.StripedLock` |
| 异步任务 | `ThreadPoolUtils.supplyAsync()` |
| 批量并行 | `ThreadPoolUtils.parallelMap()` |
| Redis 操作 | `RedisUtil` |
| 敏感词检测 | `SensitiveWordUtils.checkText()` |
| 发送通知 | `NotificationUtil.sendXxx()` |
| 分页查询 | `PageHelper.doPage()` |
| 密码加密 | `PasswordUtil.encode()` |

### 2. 注意事项

- **ConcurrentUtils.RateLimiter**：`acquire()` 方法使用忙等待，高并发时可能浪费 CPU
- **RedisUtil**：所有操作都捕获异常并返回默认值，不会抛出异常
- **ThreadPoolUtils**：所有线程池在 JVM 关闭时自动清理
- **SensitiveWordUtils**：检测结果会缓存 5 分钟，避免重复检测
- **NotificationUtil**：发送失败只记录日志，不抛出异常

### 3. 性能优化

- 使用 `ThreadPoolUtils.parallelMap()` 替代循环中的串行调用
- 使用 `RedisUtil` 的批量操作替代循环中的单个操作
- 使用 `SensitiveWordUtils.checkTextBatch()` 替代循环中的单个检测
- 使用 `ConcurrentUtils.StripedLock` 替代全局锁，减少锁竞争

---

## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | 所有工具类都在公共底座模块中 |
| [鉴权与用户体系](/modules/auth) | 被依赖 | 密码工具、IP 工具被鉴权模块使用 |
| [敏感词风控](/modules/sensitive) | 被依赖 | 敏感词工具被多个内容模块使用 |
| [通知中心](/modules/notification) | 被依赖 | 通知工具被多个模块使用 |
| [积分与抽奖](/modules/points) | 被依赖 | Redis 工具、并发工具被积分模块使用 |
