package com.ty1l.spotify_remake.utility;

import com.anji.captcha.service.CaptchaCacheService;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * AJ-Captcha Redis 缓存实现
 * 将验证码坐标数据存入 Redis，1分钟过期，校验时取出比对并用完即焚。
 */
public class CaptchaCacheServiceRedisImpl implements CaptchaCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    public CaptchaCacheServiceRedisImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String type() {
        return "redis";
    }

    @Override
    public void set(String key, String value, long timeoutInSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, timeoutInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }
}
