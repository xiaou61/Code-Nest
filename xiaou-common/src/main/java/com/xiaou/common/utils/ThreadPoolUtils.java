package com.xiaou.common.utils;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 线程池与并发工具类
 * <p>
 * 提供线程池管理、异步任务执行、批量并行处理、超时控制、重试机制等功能
 * </p>
 * <p>
 * 核心特性：
 * <ul>
 *     <li>多种预配置线程池：通用池、IO密集型、CPU密集型、调度池</li>
 *     <li>支持虚拟线程（Java 21+）</li>
 *     <li>丰富的并行处理方法：parallelMap、parallelMapWithTimeout、parallelMapWithLimit</li>
 *     <li>超时控制与重试机制（支持指数退避）</li>
 *     <li>线程池监控与动态调参</li>
 *     <li>异步回调链式操作</li>
 *     <li>快速失败模式</li>
 * </ul>
 * </p>
 *
 * @author xiaou
 */
@Slf4j
public class ThreadPoolUtils {

    /**
     * 全局共享线程池（用于通用异步任务）
     */
    private static final ExecutorService COMMON_EXECUTOR;

    /**
     * 调度线程池（用于定时任务）
     */
    private static final ScheduledExecutorService SCHEDULER;

    /**
     * IO密集型线程池（用于IO操作）
     */
    private static final ExecutorService IO_EXECUTOR;

    /**
     * CPU密集型线程池（用于计算任务）
     */
    private static final ExecutorService CPU_EXECUTOR;

    /**
     * 自定义线程池缓存
     */
    private static final Map<String, ExecutorService> POOL_CACHE = new ConcurrentHashMap<>();

    /**
     * CPU核心数
     */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * 默认超时时间（秒）
     */
    private static final long DEFAULT_TIMEOUT = 30;

    /**
     * 默认重试次数
     */
    private static final int DEFAULT_RETRY_TIMES = 3;

    /**
     * 默认重试间隔（毫秒）
     */
    private static final long DEFAULT_RETRY_DELAY = 1000;

    /**
     * 虚拟线程执行器（Java 21+）
     */
    private static final ExecutorService VIRTUAL_EXECUTOR;

    /**
     * 是否支持虚拟线程
     */
    private static final boolean VIRTUAL_THREADS_SUPPORTED;

    static {
        // 初始化通用线程池
        COMMON_EXECUTOR = new ThreadPoolExecutor(
                CPU_COUNT,
                CPU_COUNT * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new NamedThreadFactory("common"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 初始化调度线程池
        SCHEDULER = Executors.newScheduledThreadPool(
                Math.max(2, CPU_COUNT / 2),
                new NamedThreadFactory("scheduler")
        );

        // 初始化IO密集型线程池
        IO_EXECUTOR = new ThreadPoolExecutor(
                CPU_COUNT * 2,
                CPU_COUNT * 4,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                new NamedThreadFactory("io"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 初始化CPU密集型线程池
        CPU_EXECUTOR = new ThreadPoolExecutor(
                CPU_COUNT,
                CPU_COUNT,
                0L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                new NamedThreadFactory("cpu"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 初始化虚拟线程执行器（Java 21+）
        ExecutorService virtualExecutor = null;
        boolean virtualSupported = false;
        try {
            // 通过反射检测是否支持虚拟线程
            Class<?> executorsClass = Executors.class;
            java.lang.reflect.Method method = executorsClass.getMethod("newVirtualThreadPerTaskExecutor");
            virtualExecutor = (ExecutorService) method.invoke(null);
            virtualSupported = true;
            log.info("虚拟线程已启用（Java 21+）");
        } catch (Exception e) {
            log.debug("当前JDK版本不支持虚拟线程，将使用普通线程池代替");
            virtualExecutor = IO_EXECUTOR; // 回退到IO线程池
        }
        VIRTUAL_EXECUTOR = virtualExecutor;
        VIRTUAL_THREADS_SUPPORTED = virtualSupported;

        // 注册JVM关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM关闭，开始清理线程池...");
            shutdownAll();
        }));
    }

    // ==================== 线程池获取 ====================

    /**
     * 获取通用线程池
     */
    public static ExecutorService getCommonExecutor() {
        return COMMON_EXECUTOR;
    }

    /**
     * 获取IO密集型线程池
     */
    public static ExecutorService getIoExecutor() {
        return IO_EXECUTOR;
    }

    /**
     * 获取CPU密集型线程池
     */
    public static ExecutorService getCpuExecutor() {
        return CPU_EXECUTOR;
    }

    /**
     * 获取调度线程池
     */
    public static ScheduledExecutorService getScheduler() {
        return SCHEDULER;
    }

    /**
     * 获取虚拟线程执行器（Java 21+，不支持时回退到IO线程池）
     */
    public static ExecutorService getVirtualExecutor() {
        return VIRTUAL_EXECUTOR;
    }

    /**
     * 是否支持虚拟线程
     */
    public static boolean isVirtualThreadsSupported() {
        return VIRTUAL_THREADS_SUPPORTED;
    }

    /**
     * 获取或创建自定义线程池
     *
     * @param name       线程池名称
     * @param coreSize   核心线程数
     * @param maxSize    最大线程数
     * @param queueSize  队列大小
     * @return ExecutorService
     */
    public static ExecutorService getOrCreatePool(String name, int coreSize, int maxSize, int queueSize) {
        return POOL_CACHE.computeIfAbsent(name, k -> new ThreadPoolExecutor(
                coreSize,
                maxSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize),
                new NamedThreadFactory(name),
                new ThreadPoolExecutor.CallerRunsPolicy()
        ));
    }

    // ==================== 异步任务执行 ====================

    /**
     * 异步执行任务（无返回值）
     *
     * @param task 任务
     */
    public static CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, COMMON_EXECUTOR);
    }

    /**
     * 异步执行任务（有返回值）
     *
     * @param supplier 任务
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, COMMON_EXECUTOR);
    }

    /**
     * 异步执行IO密集型任务
     *
     * @param supplier 任务
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> supplyAsyncIO(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, IO_EXECUTOR);
    }

    /**
     * 异步执行CPU密集型任务
     *
     * @param supplier 任务
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> supplyAsyncCPU(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, CPU_EXECUTOR);
    }

    /**
     * 使用虚拟线程异步执行任务（Java 21+，不支持时回退到IO线程池）
     *
     * @param supplier 任务
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> supplyAsyncVirtual(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, VIRTUAL_EXECUTOR);
    }

    // ==================== 批量并行执行 ====================

    /**
     * 并行执行多个任务并等待全部完成
     *
     * @param tasks 任务列表
     * @return 结果列表
     */
    @SafeVarargs
    public static <T> List<T> parallel(Supplier<T>... tasks) {
        List<CompletableFuture<T>> futures = Arrays.stream(tasks)
                .map(task -> CompletableFuture.supplyAsync(task, COMMON_EXECUTOR))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * 并行处理集合中的每个元素
     *
     * @param items     数据集合
     * @param processor 处理函数
     * @return 处理结果列表
     */
    public static <T, R> List<R> parallelMap(Collection<T> items, Function<T, R> processor) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<R>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> processor.apply(item), COMMON_EXECUTOR))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * 并行处理集合（使用IO线程池）
     *
     * @param items     数据集合
     * @param processor 处理函数
     * @return 处理结果列表
     */
    public static <T, R> List<R> parallelMapIO(Collection<T> items, Function<T, R> processor) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<R>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> processor.apply(item), IO_EXECUTOR))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * 并行处理集合，支持超时
     *
     * @param items     数据集合
     * @param processor 处理函数
     * @param timeout   超时时间
     * @param unit      时间单位
     * @return 处理结果列表（超时的元素返回null）
     */
    public static <T, R> List<R> parallelMapWithTimeout(Collection<T> items, Function<T, R> processor,
                                                         long timeout, TimeUnit unit) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<R>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> processor.apply(item), COMMON_EXECUTOR)
                        .orTimeout(timeout, unit)
                        .exceptionally(ex -> {
                            log.warn("并行任务超时或异常: {}", ex.getMessage());
                            return null;
                        }))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * 执行所有任务并等待完成（忽略返回值）
     *
     * @param tasks 任务列表
     */
    public static void runAll(Runnable... tasks) {
        CompletableFuture<?>[] futures = Arrays.stream(tasks)
                .map(task -> CompletableFuture.runAsync(task, COMMON_EXECUTOR))
                .toArray(CompletableFuture<?>[]::new);
        CompletableFuture.allOf(futures).join();
    }

    /**
     * 执行任务列表（限制并发数）
     *
     * @param items       数据集合
     * @param processor   处理函数
     * @param concurrency 最大并发数
     * @return 处理结果列表
     */
    public static <T, R> List<R> parallelMapWithLimit(Collection<T> items, Function<T, R> processor,
                                                       int concurrency) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        Semaphore semaphore = new Semaphore(concurrency);
        List<CompletableFuture<R>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> {
                    try {
                        semaphore.acquire();
                        return processor.apply(item);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } finally {
                        semaphore.release();
                    }
                }, COMMON_EXECUTOR))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * 并行处理集合（快速失败模式：任一任务失败立即终止）
     *
     * @param items     数据集合
     * @param processor 处理函数
     * @return 处理结果列表
     * @throws RuntimeException 如果任一任务失败
     */
    public static <T, R> List<R> parallelMapFailFast(Collection<T> items, Function<T, R> processor) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<R>> futures = new ArrayList<>();
        AtomicInteger failedFlag = new AtomicInteger(0);

        for (T item : items) {
            CompletableFuture<R> future = CompletableFuture.supplyAsync(() -> {
                // 检查是否已有任务失败
                if (failedFlag.get() > 0) {
                    throw new CancellationException("其他任务已失败，取消执行");
                }
                try {
                    return processor.apply(item);
                } catch (Exception e) {
                    failedFlag.incrementAndGet();
                    throw e;
                }
            }, COMMON_EXECUTOR);
            futures.add(future);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        } catch (CompletionException e) {
            // 取消所有未完成的任务
            futures.forEach(f -> f.cancel(true));
            throw new RuntimeException("并行任务执行失败", e.getCause());
        }
    }

    /**
     * 并行处理集合（使用虚拟线程，适合大量IO密集型任务）
     *
     * @param items     数据集合
     * @param processor 处理函数
     * @return 处理结果列表
     */
    public static <T, R> List<R> parallelMapVirtual(Collection<T> items, Function<T, R> processor) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<R>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> processor.apply(item), VIRTUAL_EXECUTOR))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    // ==================== 超时控制 ====================

    /**
     * 带超时的异步执行
     *
     * @param supplier 任务
     * @param timeout  超时时间
     * @param unit     时间单位
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> supplyAsyncWithTimeout(Supplier<T> supplier,
                                                                   long timeout, TimeUnit unit) {
        return CompletableFuture.supplyAsync(supplier, COMMON_EXECUTOR)
                .orTimeout(timeout, unit);
    }

    /**
     * 带超时和默认值的异步执行
     *
     * @param supplier     任务
     * @param timeout      超时时间
     * @param unit         时间单位
     * @param defaultValue 超时默认值
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> supplyAsyncWithDefault(Supplier<T> supplier,
                                                                   long timeout, TimeUnit unit,
                                                                   T defaultValue) {
        return CompletableFuture.supplyAsync(supplier, COMMON_EXECUTOR)
                .orTimeout(timeout, unit)
                .exceptionally(ex -> {
                    log.warn("任务执行超时或异常，返回默认值: {}", ex.getMessage());
                    return defaultValue;
                });
    }

    /**
     * 同步执行任务（带超时）
     *
     * @param supplier 任务
     * @param timeout  超时时间
     * @param unit     时间单位
     * @return 执行结果
     */
    public static <T> T executeWithTimeout(Supplier<T> supplier, long timeout, TimeUnit unit) {
        try {
            return CompletableFuture.supplyAsync(supplier, COMMON_EXECUTOR)
                    .get(timeout, unit);
        } catch (TimeoutException e) {
            log.warn("任务执行超时");
            throw new RuntimeException("任务执行超时", e);
        } catch (Exception e) {
            log.error("任务执行异常", e);
            throw new RuntimeException("任务执行异常", e);
        }
    }

    // ==================== 重试机制 ====================

    /**
     * 带重试的任务执行
     *
     * @param supplier   任务
     * @param retryTimes 重试次数
     * @param delayMs    重试间隔（毫秒）
     * @return 执行结果
     */
    public static <T> T executeWithRetry(Supplier<T> supplier, int retryTimes, long delayMs) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts <= retryTimes) {
            try {
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                attempts++;
                if (attempts <= retryTimes) {
                    log.warn("任务执行失败，第{}次重试，错误: {}", attempts, e.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                }
            }
        }

        log.error("任务重试{}次后仍然失败", retryTimes);
        throw new RuntimeException("任务执行失败", lastException);
    }

    /**
     * 带指数退避重试的任务执行
     * <p>
     * 重试间隔按指数增长：initialDelayMs, initialDelayMs*2, initialDelayMs*4...
     * 适合网络请求等可能因临时过载失败的场景
     * </p>
     *
     * @param supplier       任务
     * @param retryTimes     重试次数
     * @param initialDelayMs 初始重试间隔（毫秒）
     * @param maxDelayMs     最大重试间隔（毫秒）
     * @return 执行结果
     */
    public static <T> T executeWithExponentialBackoff(Supplier<T> supplier, int retryTimes,
                                                       long initialDelayMs, long maxDelayMs) {
        int attempts = 0;
        Exception lastException = null;
        long currentDelay = initialDelayMs;

        while (attempts <= retryTimes) {
            try {
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                attempts++;
                if (attempts <= retryTimes) {
                    // 添加随机抖动，避免雷同重试
                    long jitter = (long) (currentDelay * 0.2 * Math.random());
                    long sleepTime = Math.min(currentDelay + jitter, maxDelayMs);
                    log.warn("任务执行失败，第{}次重试，等待{}ms，错误: {}", attempts, sleepTime, e.getMessage());
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                    currentDelay = Math.min(currentDelay * 2, maxDelayMs);
                }
            }
        }

        log.error("任务重试{}次后仍然失败", retryTimes);
        throw new RuntimeException("任务执行失败", lastException);
    }

    /**
     * 带指数退避重试的任务执行（使用默认参数）
     *
     * @param supplier 任务
     * @return 执行结果
     */
    public static <T> T executeWithExponentialBackoff(Supplier<T> supplier) {
        return executeWithExponentialBackoff(supplier, DEFAULT_RETRY_TIMES, 500, 10000);
    }

    /**
     * 带条件重试的任务执行
     * <p>
     * 可以指定哪些异常需要重试，哪些异常直接抛出
     * </p>
     *
     * @param supplier        任务
     * @param retryTimes      重试次数
     * @param delayMs         重试间隔（毫秒）
     * @param retryCondition  重试条件（返回true表示需要重试）
     * @return 执行结果
     */
    public static <T> T executeWithConditionalRetry(Supplier<T> supplier, int retryTimes,
                                                     long delayMs, Predicate<Exception> retryCondition) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts <= retryTimes) {
            try {
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                // 检查是否需要重试
                if (!retryCondition.test(e)) {
                    throw new RuntimeException("不可重试的异常", e);
                }
                attempts++;
                if (attempts <= retryTimes) {
                    log.warn("任务执行失败，第{}次重试，错误: {}", attempts, e.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                }
            }
        }

        log.error("任务重试{}次后仍然失败", retryTimes);
        throw new RuntimeException("任务执行失败", lastException);
    }

    /**
     * 带重试的任务执行（使用默认参数）
     *
     * @param supplier 任务
     * @return 执行结果
     */
    public static <T> T executeWithRetry(Supplier<T> supplier) {
        return executeWithRetry(supplier, DEFAULT_RETRY_TIMES, DEFAULT_RETRY_DELAY);
    }

    /**
     * 带重试的异步任务执行
     *
     * @param supplier   任务
     * @param retryTimes 重试次数
     * @param delayMs    重试间隔（毫秒）
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> supplyAsyncWithRetry(Supplier<T> supplier,
                                                                 int retryTimes, long delayMs) {
        return CompletableFuture.supplyAsync(() -> executeWithRetry(supplier, retryTimes, delayMs), COMMON_EXECUTOR);
    }

    // ==================== 定时调度 ====================

    /**
     * 延迟执行任务
     *
     * @param task  任务
     * @param delay 延迟时间
     * @param unit  时间单位
     * @return ScheduledFuture
     */
    public static ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return SCHEDULER.schedule(task, delay, unit);
    }

    /**
     * 延迟执行任务（带返回值）
     *
     * @param callable 任务
     * @param delay    延迟时间
     * @param unit     时间单位
     * @return ScheduledFuture
     */
    public static <T> ScheduledFuture<T> schedule(Callable<T> callable, long delay, TimeUnit unit) {
        return SCHEDULER.schedule(callable, delay, unit);
    }

    /**
     * 固定频率执行任务
     *
     * @param task         任务
     * @param initialDelay 初始延迟
     * @param period       执行间隔
     * @param unit         时间单位
     * @return ScheduledFuture
     */
    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay,
                                                          long period, TimeUnit unit) {
        return SCHEDULER.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    /**
     * 固定延迟执行任务
     *
     * @param task         任务
     * @param initialDelay 初始延迟
     * @param delay        延迟时间
     * @param unit         时间单位
     * @return ScheduledFuture
     */
    public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay,
                                                             long delay, TimeUnit unit) {
        return SCHEDULER.scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }

    // ==================== 竞态任务 ====================

    /**
     * 执行多个任务，返回最先完成的结果
     *
     * @param tasks 任务列表
     * @return 最先完成的结果
     */
    @SafeVarargs
    public static <T> T anyOf(Supplier<T>... tasks) {
        if (tasks == null || tasks.length == 0) {
            throw new IllegalArgumentException("任务列表不能为空");
        }

        CompletableFuture<?>[] futures = Arrays.stream(tasks)
                .map(task -> CompletableFuture.supplyAsync(task, COMMON_EXECUTOR))
                .toArray(CompletableFuture<?>[]::new);

        try {
            @SuppressWarnings("unchecked")
            T result = (T) CompletableFuture.anyOf(futures).get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("获取最快任务结果失败", e);
        }
    }

    // ==================== 线程池状态监控 ====================

    /**
     * 获取线程池状态信息
     *
     * @param executor 线程池
     * @return 状态信息Map
     */
    public static Map<String, Object> getPoolStats(ExecutorService executor) {
        Map<String, Object> stats = new LinkedHashMap<>();

        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;
            stats.put("corePoolSize", pool.getCorePoolSize());
            stats.put("maximumPoolSize", pool.getMaximumPoolSize());
            stats.put("activeCount", pool.getActiveCount());
            stats.put("poolSize", pool.getPoolSize());
            stats.put("largestPoolSize", pool.getLargestPoolSize());
            stats.put("taskCount", pool.getTaskCount());
            stats.put("completedTaskCount", pool.getCompletedTaskCount());
            stats.put("queueSize", pool.getQueue().size());
            stats.put("queueRemainingCapacity", pool.getQueue().remainingCapacity());
            stats.put("isShutdown", pool.isShutdown());
            stats.put("isTerminated", pool.isTerminated());
        }

        return stats;
    }

    /**
     * 获取通用线程池状态
     */
    public static Map<String, Object> getCommonPoolStats() {
        return getPoolStats(COMMON_EXECUTOR);
    }

    /**
     * 获取IO线程池状态
     */
    public static Map<String, Object> getIoPoolStats() {
        return getPoolStats(IO_EXECUTOR);
    }

    /**
     * 获取CPU线程池状态
     */
    public static Map<String, Object> getCpuPoolStats() {
        return getPoolStats(CPU_EXECUTOR);
    }

    /**
     * 获取所有线程池状态
     */
    public static Map<String, Map<String, Object>> getAllPoolStats() {
        Map<String, Map<String, Object>> allStats = new LinkedHashMap<>();
        allStats.put("common", getCommonPoolStats());
        allStats.put("io", getIoPoolStats());
        allStats.put("cpu", getCpuPoolStats());
        allStats.put("virtualThreadsSupported", Collections.singletonMap("supported", VIRTUAL_THREADS_SUPPORTED));

        // 添加自定义线程池状态
        POOL_CACHE.forEach((name, pool) -> allStats.put("custom_" + name, getPoolStats(pool)));

        return allStats;
    }

    // ==================== 动态调整线程池参数 ====================

    /**
     * 动态调整线程池核心线程数
     *
     * @param executor 线程池
     * @param coreSize 新的核心线程数
     */
    public static void setCorePoolSize(ExecutorService executor, int coreSize) {
        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;
            pool.setCorePoolSize(coreSize);
            log.info("线程池核心线程数已调整为: {}", coreSize);
        }
    }

    /**
     * 动态调整线程池最大线程数
     *
     * @param executor 线程池
     * @param maxSize  新的最大线程数
     */
    public static void setMaxPoolSize(ExecutorService executor, int maxSize) {
        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;
            pool.setMaximumPoolSize(maxSize);
            log.info("线程池最大线程数已调整为: {}", maxSize);
        }
    }

    /**
     * 预热线程池（提前创建核心线程）
     *
     * @param executor 线程池
     */
    public static void prestartAllCoreThreads(ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;
            int count = pool.prestartAllCoreThreads();
            log.info("线程池已预热，创建核心线程数: {}", count);
        }
    }

    /**
     * 预热所有内置线程池
     */
    public static void prestartAllPools() {
        prestartAllCoreThreads(COMMON_EXECUTOR);
        prestartAllCoreThreads(IO_EXECUTOR);
        prestartAllCoreThreads(CPU_EXECUTOR);
        log.info("所有内置线程池已预热");
    }

    // ==================== 异步回调链 ====================

    /**
     * 创建异步任务构建器
     *
     * @param supplier 初始任务
     * @return AsyncChain
     */
    public static <T> AsyncChain<T> asyncChain(Supplier<T> supplier) {
        return new AsyncChain<>(CompletableFuture.supplyAsync(supplier, COMMON_EXECUTOR));
    }

    /**
     * 异步任务链式构建器
     */
    public static class AsyncChain<T> {
        private final CompletableFuture<T> future;

        private AsyncChain(CompletableFuture<T> future) {
            this.future = future;
        }

        /**
         * 链式转换
         */
        public <R> AsyncChain<R> thenApply(Function<T, R> fn) {
            return new AsyncChain<>(future.thenApplyAsync(fn, COMMON_EXECUTOR));
        }

        /**
         * 链式异步转换
         */
        public <R> AsyncChain<R> thenCompose(Function<T, CompletableFuture<R>> fn) {
            return new AsyncChain<>(future.thenComposeAsync(fn, COMMON_EXECUTOR));
        }

        /**
         * 链式消费
         */
        public AsyncChain<T> thenAccept(Consumer<T> action) {
            return new AsyncChain<>(future.thenApplyAsync(t -> {
                action.accept(t);
                return t;
            }, COMMON_EXECUTOR));
        }

        /**
         * 异常处理
         */
        public AsyncChain<T> onError(Function<Throwable, T> handler) {
            return new AsyncChain<>(future.exceptionally(handler));
        }

        /**
         * 设置超时
         */
        public AsyncChain<T> timeout(long timeout, TimeUnit unit) {
            return new AsyncChain<>(future.orTimeout(timeout, unit));
        }

        /**
         * 获取Future
         */
        public CompletableFuture<T> toFuture() {
            return future;
        }

        /**
         * 阻塞获取结果
         */
        public T get() {
            return future.join();
        }

        /**
         * 带超时获取结果
         */
        public T get(long timeout, TimeUnit unit) throws TimeoutException {
            try {
                return future.get(timeout, unit);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ==================== 线程池管理 ====================

    /**
     * 关闭指定线程池
     *
     * @param executor     线程池
     * @param awaitSeconds 等待时间（秒）
     */
    public static void shutdown(ExecutorService executor, long awaitSeconds) {
        if (executor == null || executor.isShutdown()) {
            return;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(awaitSeconds, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("线程池未能正常关闭");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 关闭所有线程池
     */
    public static void shutdownAll() {
        log.info("开始关闭所有线程池...");

        // 关闭自定义线程池
        POOL_CACHE.values().forEach(pool -> shutdown(pool, 10));
        POOL_CACHE.clear();

        // 关闭内置线程池
        shutdown(COMMON_EXECUTOR, 30);
        shutdown(IO_EXECUTOR, 30);
        shutdown(CPU_EXECUTOR, 30);
        shutdown(SCHEDULER, 10);

        // 关闭虚拟线程执行器（如果是独立的）
        if (VIRTUAL_THREADS_SUPPORTED && VIRTUAL_EXECUTOR != IO_EXECUTOR) {
            shutdown(VIRTUAL_EXECUTOR, 10);
        }

        log.info("所有线程池已关闭");
    }

    /**
     * 移除并关闭自定义线程池
     *
     * @param name 线程池名称
     */
    public static void removePool(String name) {
        ExecutorService pool = POOL_CACHE.remove(name);
        if (pool != null) {
            shutdown(pool, 10);
            log.info("自定义线程池已移除: {}", name);
        }
    }

    // ==================== 辅助类 ====================

    /**
     * 命名线程工厂
     */
    public static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final boolean daemon;

        public NamedThreadFactory(String name) {
            this(name, false);
        }

        public NamedThreadFactory(String name, boolean daemon) {
            this.namePrefix = "pool-" + name + "-thread-";
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(daemon);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    /**
     * 任务执行结果包装器
     */
    public static class TaskResult<T> {
        private final boolean success;
        private final T data;
        private final String errorMessage;
        private final long executionTime;

        private TaskResult(boolean success, T data, String errorMessage, long executionTime) {
            this.success = success;
            this.data = data;
            this.errorMessage = errorMessage;
            this.executionTime = executionTime;
        }

        public static <T> TaskResult<T> success(T data, long executionTime) {
            return new TaskResult<>(true, data, null, executionTime);
        }

        public static <T> TaskResult<T> failure(String errorMessage, long executionTime) {
            return new TaskResult<>(false, null, errorMessage, executionTime);
        }

        public boolean isSuccess() {
            return success;
        }

        public T getData() {
            return data;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public long getExecutionTime() {
            return executionTime;
        }
    }

    /**
     * 带执行时间统计的任务执行
     *
     * @param supplier 任务
     * @return TaskResult
     */
    public static <T> TaskResult<T> executeWithStats(Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();
        try {
            T result = supplier.get();
            return TaskResult.success(result, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            return TaskResult.failure(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    // ==================== 配置类 ====================

    /**
     * 线程池配置
     */
    @Data
    @Builder
    public static class PoolConfig {
        /** 核心线程数 */
        @Builder.Default
        private int coreSize = CPU_COUNT;
        /** 最大线程数 */
        @Builder.Default
        private int maxSize = CPU_COUNT * 2;
        /** 队列大小 */
        @Builder.Default
        private int queueSize = 1000;
        /** 线程存活时间（秒） */
        @Builder.Default
        private long keepAliveSeconds = 60;
        /** 线程名前缀 */
        @Builder.Default
        private String threadNamePrefix = "custom";
        /** 是否守护线程 */
        @Builder.Default
        private boolean daemon = false;

        /**
         * 根据配置创建线程池
         */
        public ThreadPoolExecutor build() {
            return new ThreadPoolExecutor(
                    coreSize,
                    maxSize,
                    keepAliveSeconds, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueSize),
                    new NamedThreadFactory(threadNamePrefix, daemon),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
        }
    }

    /**
     * 重试配置
     */
    @Data
    @Builder
    public static class RetryConfig {
        /** 重试次数 */
        @Builder.Default
        private int retryTimes = 3;
        /** 初始延迟（毫秒） */
        @Builder.Default
        private long initialDelayMs = 1000;
        /** 最大延迟（毫秒） */
        @Builder.Default
        private long maxDelayMs = 30000;
        /** 是否使用指数退避 */
        @Builder.Default
        private boolean exponentialBackoff = false;
        /** 重试条件 */
        private Predicate<Exception> retryCondition;

        /**
         * 执行带重试的任务
         */
        public <T> T execute(Supplier<T> supplier) {
            if (exponentialBackoff) {
                return executeWithExponentialBackoff(supplier, retryTimes, initialDelayMs, maxDelayMs);
            } else if (retryCondition != null) {
                return executeWithConditionalRetry(supplier, retryTimes, initialDelayMs, retryCondition);
            } else {
                return executeWithRetry(supplier, retryTimes, initialDelayMs);
            }
        }
    }

    /**
     * 私有构造函数，防止实例化
     */
    private ThreadPoolUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }
}
