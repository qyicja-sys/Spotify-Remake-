package com.ty1l.spotify_remake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

// 排除 Redis 自动配置，为了本地使用aj captcha
@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
public class SpotifyRemakeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpotifyRemakeApplication.class, args);
    }


}
