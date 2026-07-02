package com.ty1l.spotify_remake.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置
 *
 * 为不同数据类型提供独立的 Cache 实例，避免类型混淆。
 * L1 TTL 设为 5 分钟（比 Redis L2 TTL 短），减少脏读窗口。
 */
@Configuration
public class CaffeineConfig {

    /** L1 本地缓存 TTL（分钟），应明显短于 Redis TTL */
    static final long L1_TTL_MINUTES = 5;

    /** 每种数据类型最大条目数 */
    static final long MAX_SIZE_SONG = 500;
    static final long MAX_SIZE_ARTIST = 200;
    static final long MAX_SIZE_ALBUM = 200;
    static final long MAX_SIZE_LYRICS = 500;
    static final long MAX_SIZE_EXTERNAL = 300;
    static final long MAX_SIZE_HOME = 200;
    static final long MAX_SIZE_PROFILE = 200;
    static final long MAX_SIZE_STRING = 500;

    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, Object> songCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_SIZE_SONG)
                .expireAfterWrite(L1_TTL_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, Object> artistCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_SIZE_ARTIST)
                .expireAfterWrite(L1_TTL_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, Object> albumCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_SIZE_ALBUM)
                .expireAfterWrite(L1_TTL_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, Object> lyricsCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_SIZE_LYRICS)
                .expireAfterWrite(L1_TTL_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, Object> externalCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_SIZE_EXTERNAL)
                .expireAfterWrite(L1_TTL_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, Object> homeCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_SIZE_HOME)
                .expireAfterWrite(L1_TTL_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, Object> profileCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_SIZE_PROFILE)
                .expireAfterWrite(L1_TTL_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, String> stringCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_SIZE_STRING)
                .expireAfterWrite(L1_TTL_MINUTES, TimeUnit.MINUTES)
                .build();
    }
}
