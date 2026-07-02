package com.ty1l.spotify_remake.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置（与 Lettuce 共存，独立连接池）
 *
 * 用于 RedisBloom 布隆过滤器 + 分布式锁操作。
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setConnectionPoolSize(8)
                .setConnectionMinimumIdleSize(1)
                .setConnectTimeout(3000)
                .setRetryAttempts(3);
        return Redisson.create(config);
    }
}
