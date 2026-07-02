package com.ty1l.spotify_remake.Service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁辅助（防缓存击穿/雪崩）
 *
 * 使用 Redisson RLock，确保同一时刻只有一个线程查 DB 并回写缓存，
 * 其他线程等待锁释放后重试读缓存。
 */
@Service
public class CacheLockHelper {

    private static final Logger log = LoggerFactory.getLogger(CacheLockHelper.class);

    private static final String LOCK_PREFIX = "spotify:lock:cache:";
    private static final long DEFAULT_WAIT_MS = 2000;
    private static final long DEFAULT_LEASE_MS = 5000;
    private static final long RETRY_SLEEP_MS = 100;

    private final RedissonClient redisson;

    public CacheLockHelper(RedissonClient redissonClient) {
        this.redisson = redissonClient;
    }

    /**
     * 获取锁→double-check 缓存→查 DB 写缓存→解锁。
     *
     * 拿不到锁的线程每 100ms 重试读缓存，超时后降级直接查 DB（不写缓存）。
     *
     * @param cacheKey    缓存 key，用作锁 key 的推导
     * @param cacheReader 读缓存函数（L1+L2）
     * @param dbLoader    查 DB 函数
     * @param cacheWriter 写缓存函数（L1+L2 同时写）
     * @param <T>         返回值类型
     * @return 缓存或 DB 数据
     */
    public <T> T withLock(String cacheKey,
                          Supplier<T> cacheReader,
                          Supplier<T> dbLoader,
                          java.util.function.Consumer<T> cacheWriter) {
        return withLock(cacheKey, cacheReader, dbLoader, cacheWriter, DEFAULT_WAIT_MS, DEFAULT_LEASE_MS);
    }

    /**
     * @param waitMs  等待锁的最大时间（ms）
     * @param leaseMs 锁自动释放时间（ms），防止死锁
     */
    public <T> T withLock(String cacheKey,
                          Supplier<T> cacheReader,
                          Supplier<T> dbLoader,
                          java.util.function.Consumer<T> cacheWriter,
                          long waitMs,
                          long leaseMs) {
        String lockKey = LOCK_PREFIX + cacheKey;
        RLock lock = redisson.getLock(lockKey);

        try {
            if (lock.tryLock(waitMs, leaseMs, TimeUnit.MILLISECONDS)) {
                try {
                    // Double-check：拿到锁后再读一次缓存，防止前一个线程刚写完
                    T cached = cacheReader.get();
                    if (cached != null) {
                        log.debug("Lock double-check cache hit: {}", cacheKey);
                        return cached;
                    }

                    // 查 DB 并写缓存
                    T data = dbLoader.get();
                    if (data != null) {
                        cacheWriter.accept(data);
                    }
                    return data;
                } finally {
                    try {
                        lock.unlock();
                    } catch (Exception ignored) {
                        // 锁可能已过期被自动释放
                    }
                }
            }

            // 没拿到锁：重试读缓存
            long deadline = System.currentTimeMillis() + waitMs;
            while (System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(RETRY_SLEEP_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                T cached = cacheReader.get();
                if (cached != null) {
                    log.debug("Retry cache hit after lock wait: {}", cacheKey);
                    return cached;
                }
            }

            // 超时降级：直接查 DB 不写缓存
            log.warn("Lock timeout for {}, falling back to direct DB query", cacheKey);
            return dbLoader.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 被中断也降级查 DB
            log.warn("Lock interrupted for {}, falling back to DB", cacheKey);
            return dbLoader.get();
        } catch (Exception e) {
            log.warn("Lock error for {}: {}, falling back to DB", cacheKey, e.getMessage());
            return dbLoader.get();
        }
    }
}
