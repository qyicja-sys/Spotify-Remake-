package com.ty1l.spotify_remake.config;

import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.BlockPuzzleCaptchaServiceImpl;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.anji.captcha.service.impl.ClickWordCaptchaServiceImpl;
import com.ty1l.spotify_remake.utility.CaptchaCacheServiceRedisImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Properties;

@Configuration
public class CaptchaConfig {

    @Value("${aj.captcha.type:blockPuzzle}")
    private String captchaType;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Bean
    public CaptchaService captchaService() {
        // 1. 创建 Redis 缓存实现，注入 StringRedisTemplate
        CaptchaCacheServiceRedisImpl cacheService = new CaptchaCacheServiceRedisImpl(stringRedisTemplate);

        // 2. 预注入到 CaptchaServiceFactory 的静态 Map 中
        //    "new BlockPuzzleCaptchaServiceImpl()" 触发 CaptchaServiceFactory 静态初始化，
        //    加载 SPI 实现（只有 MemImpl），我们在 init() 之前注入 Redis 实现。
        BlockPuzzleCaptchaServiceImpl service = new BlockPuzzleCaptchaServiceImpl();
        CaptchaServiceFactory.cacheService.put("redis", cacheService);

        // 3. 配置 Properties
        Properties props = new Properties();
        props.setProperty("aj.captcha.cache-type", "redis");
        props.setProperty("aj.captcha.type", captchaType);

        // 4. 根据类型创建
        if ("clickWord".equals(captchaType)) {
            ClickWordCaptchaServiceImpl clickService = new ClickWordCaptchaServiceImpl();
            CaptchaServiceFactory.cacheService.put("redis", cacheService);
            clickService.init(props);
            return clickService;
        } else {
            service.init(props);
            return service;
        }
    }
}
