package com.ty1l.spotify_remake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpotifyRemakeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpotifyRemakeApplication.class, args);
    }


}
