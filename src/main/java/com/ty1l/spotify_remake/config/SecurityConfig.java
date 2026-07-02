package com.ty1l.spotify_remake.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 完全绕过安全过滤链的路径（仅静态资源）。
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/spotify-cms/**", "/spotify-frontend/**",
                "/static/datas/**", "/uploads/**"
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 公开接口
                        .requestMatchers(
                                "/", "/index.html", "/main.html",
                                "/spotify/login", "/spotify/signup", "/spotify/login/forgetPassword",
                                "/spotify/token/refresh",
                                "/spotify/home", "/spotify/search", "/spotify/profile/**",
                                "/spotify/artists/**", "/spotify/artist/**",
                                "/spotify/external/**",
                                "/spotify/playlist/**",
                                "/spotify/album/**",
                                "/spotify/lyrics/**",
                                "/spotify/playback/**",
                                "/spotify/leaderboard/**",
                                "/stream/**",
                                "/captcha/get", "/captcha/check",
                                "/admin/spotify/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
