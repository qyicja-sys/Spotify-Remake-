package com.ty1l.spotify_remake.utility;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocalCaptchaCache {

    // 线程安全的 HashMap，用来在内存中存 Token 的状态
    private static final Map<String, CacheNode> CACHE_MAP = new ConcurrentHashMap<>();

    // 定时线程池，用来每隔 5 秒清理一次过期的 Token，防止内存泄漏
    private static final ScheduledExecutorService CLEANER = Executors.newSingleThreadScheduledExecutor();

    static {
        // 启动定时清理任务
        CLEANER.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            CACHE_MAP.forEach((key, node) -> {
                if (now > node.expireTime) {
                    CACHE_MAP.remove(key); // 过期了，从内存中移除
                }
            });
        }, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * 存入缓存（模拟 Redis 的 setex 命令）
     * @param key Token字符串
     * @param value 状态或坐标
     * @param timeoutInSeconds 过期时间（秒）
     */
    public static void put(String key, String value, long timeoutInSeconds) {
        long expireTime = System.currentTimeMillis() + (timeoutInSeconds * 1000);
        CACHE_MAP.put(key, new CacheNode(value, expireTime));
    }

    /**
     * 获取缓存
     */
    public static String get(String key) {
        CacheNode node = CACHE_MAP.get(key);
        if (node == null) {
            return null;
        }
        // 双重检查：如果取的时候已经过期了，直接删掉并返回空
        if (System.currentTimeMillis() > node.expireTime) {
            CACHE_MAP.remove(key);
            return null;
        }
        return node.value;
    }

    /**
     * 删除缓存（模拟 Redis 的 del 命令）
     */
    public static void delete(String key) {
        CACHE_MAP.remove(key);
    }

    // 内部类：包装存储的数据和过期时间戳
    private static class CacheNode {
        String value;
        long expireTime; // 绝对过期时间戳（毫秒）

        CacheNode(String value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
    }
}