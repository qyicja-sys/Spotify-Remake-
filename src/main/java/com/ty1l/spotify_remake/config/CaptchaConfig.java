package com.ty1l.spotify_remake.config;

import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.BlockPuzzleCaptchaServiceImpl;
import com.anji.captcha.service.impl.ClickWordCaptchaServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Properties;

@Configuration
public class CaptchaConfig {

    // 读取你 application.yml 里的配置，如果读不到默认用滑块(blockPuzzle)
    @Value("${aj.captcha.type:blockPuzzle}")
    private String captchaType;

    @Bean
    public CaptchaService captchaService() {
        Properties properties = new Properties();

        // 关键点：手动告诉底层的缓存机制使用本地内存 local
        properties.setProperty("aj.captcha.cache-type", "local");
        properties.setProperty("aj.captcha.type", captchaType);

        // 根据配置的类型，手动创建对应的实现类实例
        if ("clickWord".equals(captchaType)) {
            ClickWordCaptchaServiceImpl service = new ClickWordCaptchaServiceImpl();
            service.init(properties);
            return service;
        } else {
            BlockPuzzleCaptchaServiceImpl service = new BlockPuzzleCaptchaServiceImpl();
            service.init(properties);
            return service;
        }
    }
}