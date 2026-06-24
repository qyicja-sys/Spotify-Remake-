package com.ty1l.spotify_remake.config;

import com.ty1l.spotify_remake.interceptor.AdminTokenInterceptor;
import com.ty1l.spotify_remake.interceptor.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private TokenInterceptor tokenInterceptor;

    @Autowired
    private AdminTokenInterceptor adminTokenInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 所有的接口都支持跨域
                .allowedOriginPatterns("*") // 允许的来源地址
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的方法
                .allowedHeaders("*") // 允许的请求头
                .allowCredentials(false) // 是否允许携带 Cookie
                .maxAge(3600); // 跨域检测的有效期（秒）
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",
                        "/index.html",
                        "/main.html",
                        "/spotify/login",
                        "/spotify/signup",
                        "/spotify/login/forgetPassword",
                        "/spotify/external/**",
                        "/captcha/get",
                        "/captcha/check",
                        "/spotify",
                        "/spotify-frontend/**",
                        "/spotify-cms/**",
                        "/static/datas/**",
                        "/error",
                        "/uploads/**",
                        "/songs/**",
                        "/admin/**"
                );

        registry.addInterceptor(adminTokenInterceptor)
                .addPathPatterns("/admin/spotify/**")
                .excludePathPatterns("/admin/spotify/login");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 资源映射已指向 dist，所以直接转发到 /spotify-frontend/index.html
        registry.addViewController("/spotify-frontend/").setViewName("forward:/spotify-frontend/index.html");
        registry.addViewController("/spotify-frontend").setViewName("forward:/spotify-frontend/index.html");
        registry.addViewController("/").setViewName("forward:/spotify-frontend/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 前端静态资源映射 — 全部指向 dist 目录
        registry.addResourceHandler("/spotify-frontend/**")
                .addResourceLocations("classpath:/static/spotify-frontend/dist/");
        registry.addResourceHandler("/spotify-cms/**")
                .addResourceLocations("classpath:/static/spotify-cms/");

        // 1. 前端访问的虚拟路径：/songs/**
        // 2. 本地实际的物理路径：file:D:/music_test/ （注意最后有一个斜杠）
        registry.addResourceHandler("/songs/**")
                .addResourceLocations("file:D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/music/musics/");
        /*
         * 1. /uploads/** 是前端访问的 URL 路径占位符
         * 2. file:D:/spotify_resources/ 或者是 Linux 路径 file:/var/spotify/
         * 是本地图片实际存放的根目录（注意：末尾一定要加斜杠 '/'）
         */
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/datas/");
        // 数据库中 coverUrl 存的是 /static/datas/... 路径，需要映射
        registry.addResourceHandler("/static/datas/**")
                .addResourceLocations("file:D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/datas/");
    }
}