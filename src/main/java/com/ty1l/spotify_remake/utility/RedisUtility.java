package com.ty1l.spotify_remake.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

/**
 * Redis 手动序列化/反序列化工���类
 * 使用 Jackson 将对象转为 JSON 字符串存入 Redis，取出时手动反序列化回对象。
 * 配合 StringRedisTemplate 或 RedisTemplate<String, String> 使用。
 */
@Component
public class RedisUtility {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 手动序列化：将 Java 对象转为 JSON 字符串
     *
     * @param obj 要序列化的对象
     * @return JSON 字符串
     * @throws RuntimeException 序列化失败时抛出
     */
    public String serialize(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 手动序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 手动反序列化：将 JSON 字符串转为指定类型的 Java 对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 反序列化后的对象
     * @throws RuntimeException 反序列化失败时抛出
     */
    public <T> T deserialize(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 手动反序列化失败: " + e.getMessage(), e);
        }
    }
}
