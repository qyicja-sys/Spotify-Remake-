package com.ty1l.spotify_remake.utility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token 服务：封装 JWT + Redis 双重Token机制的 Redis 操作
 *
 * Redis Key 规范：{业务}:{模块}:{key}
 * - 用户端: login:user:{userId}
 * - CMS端: login:admin:{adminId}
 *
 * Value: 当前有效的 version (UUID)，用于判断是否被其他设备登录覆盖
 */
@Service
public class TokenService {

    /** Refresh Token 过期时间：7天（秒） */
    private static final long REFRESH_TTL_SECONDS = 7 * 24 * 60 * 60;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ==================== 用户端 ====================

    /** 生成新的 version UUID */
    public String generateVersion() {
        return UUID.randomUUID().toString();
    }

    /** 存储用户 token version 到 Redis */
    public void storeUserVersion(Long userId, String version) {
        String key = "login:user:" + userId;
        stringRedisTemplate.opsForValue().set(key, version, REFRESH_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 校验用户 token version 是否与 Redis 中一致
     * @throws TokenInvalidException 如果 Redis 中 version 不存在或不匹配
     */
    public void validateUserVersion(Long userId, String version) {
        String key = "login:user:" + userId;
        String storedVersion = stringRedisTemplate.opsForValue().get(key);
        if (storedVersion == null) {
            throw new TokenInvalidException("TOKEN_EXPIRED");
        }
        if (!storedVersion.equals(version)) {
            throw new TokenInvalidException("ACCOUNT_LOGGED_IN_ELSEWHERE");
        }
    }

    /**
     * 刷新用户 token version：校验旧 version → 生成新 version → 更新 Redis → 返回新 version
     * @throws TokenInvalidException 如果旧 version 不匹配
     */
    public String refreshUserVersion(Long userId, String oldVersion) {
        String key = "login:user:" + userId;
        String storedVersion = stringRedisTemplate.opsForValue().get(key);
        if (storedVersion == null) {
            throw new TokenInvalidException("TOKEN_EXPIRED");
        }
        if (!storedVersion.equals(oldVersion)) {
            throw new TokenInvalidException("ACCOUNT_LOGGED_IN_ELSEWHERE");
        }
        String newVersion = generateVersion();
        stringRedisTemplate.opsForValue().set(key, newVersion, REFRESH_TTL_SECONDS, TimeUnit.SECONDS);
        return newVersion;
    }

    // ==================== CMS端 ====================

    /** 存储管理员 token version 到 Redis */
    public void storeAdminVersion(Long adminId, String version) {
        String key = "login:admin:" + adminId;
        stringRedisTemplate.opsForValue().set(key, version, REFRESH_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 校验管理员 token version 是否与 Redis 中一致
     * @throws TokenInvalidException 如果 Redis 中 version 不存在或不匹配
     */
    public void validateAdminVersion(Long adminId, String version) {
        String key = "login:admin:" + adminId;
        String storedVersion = stringRedisTemplate.opsForValue().get(key);
        if (storedVersion == null) {
            throw new TokenInvalidException("TOKEN_EXPIRED");
        }
        if (!storedVersion.equals(version)) {
            throw new TokenInvalidException("ACCOUNT_LOGGED_IN_ELSEWHERE");
        }
    }

    /**
     * 刷新管理员 token version
     * @throws TokenInvalidException 如果旧 version 不匹配
     */
    public String refreshAdminVersion(Long adminId, String oldVersion) {
        String key = "login:admin:" + adminId;
        String storedVersion = stringRedisTemplate.opsForValue().get(key);
        if (storedVersion == null) {
            throw new TokenInvalidException("TOKEN_EXPIRED");
        }
        if (!storedVersion.equals(oldVersion)) {
            throw new TokenInvalidException("ACCOUNT_LOGGED_IN_ELSEWHERE");
        }
        String newVersion = generateVersion();
        stringRedisTemplate.opsForValue().set(key, newVersion, REFRESH_TTL_SECONDS, TimeUnit.SECONDS);
        return newVersion;
    }

    /**
     * Token 校验失败异常（非运行时登录失败，用于拦截器区分处理）
     */
    public static class TokenInvalidException extends RuntimeException {
        public TokenInvalidException(String message) {
            super(message);
        }
    }
}
