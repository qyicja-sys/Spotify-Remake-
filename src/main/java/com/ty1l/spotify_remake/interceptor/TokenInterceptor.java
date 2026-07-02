package com.ty1l.spotify_remake.interceptor;

import com.ty1l.spotify_remake.utility.BaseContext;
import com.ty1l.spotify_remake.utility.JwtGenerate;
import com.ty1l.spotify_remake.utility.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;


import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception  {
        String url = request.getRequestURI();

        // 路径放行统一由 WebConfig.excludePathPatterns 管理，拦截器内不再重复判断

        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        //1. 获取请求头中的令牌（token）。
        String jwt = request.getHeader("token");

        //2. 判断令牌是否存在，如果不存在，返回错误结果（未登录）。
        if(!StringUtils.hasLength(jwt)){
            log.info("获取到jwt令牌为空, 返回错误结果: {}", url);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0, \"msg\":\"NOT_LOGIN\"}");
            return false;
        }

        //3. 解析token，如果解析失败，返回错误结果（未登录）。
        try {
            Claims claims = JwtGenerate.parseJwt(jwt);

            // 校验 token 类型必须为 access（防止用 refresh token 访问 API）
            String type = String.valueOf(claims.get("type"));
            if (!"access".equals(type)) {
                log.info("令牌类型不是 access, type={}, url={}", type, url);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401, \"msg\":\"INVALID_TOKEN_TYPE\"}");
                return false;
            }

            // 获取 userId（兼容旧的 "id" claim key）
            Object userIdObj = claims.get("userId");
            if (userIdObj == null) {
                userIdObj = claims.get("id");
            }
            Long userId = Long.valueOf(userIdObj.toString());

            // 获取 token version
            String version = String.valueOf(claims.get("version"));

            // Redis 双重校验：比对 version 是否与 Redis 中一致
            // 如果 B 设备登录覆盖了 Redis version，A 设备的请求在此处被拦截
            tokenService.validateUserVersion(userId, version);

            // 存入当前线程，方便后续使用此token识别当前用户
            BaseContext.setCurrentId(userId);
            BaseContext.setCurrentVersion(version);
        } catch (TokenService.TokenInvalidException e) {
            log.info("Token 双重校验失败: {}, url={}", e.getMessage(), url);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            if ("ACCOUNT_LOGGED_IN_ELSEWHERE".equals(e.getMessage())) {
                response.getWriter().write("{\"code\":401, \"msg\":\"ACCOUNT_LOGGED_IN_ELSEWHERE\"}");
            } else {
                response.getWriter().write("{\"code\":401, \"msg\":\"TOKEN_EXPIRED\"}");
            }
            return false;
        } catch (Exception e) {
            log.info("解析令牌失败, 返回错误结果", e);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401, \"msg\":\"INVALID_TOKEN\"}");
            return false;
        }

        //4. 放行。
        log.info("令牌合法, 放行: {}", url);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理 ThreadLocal，防止内存泄漏
        BaseContext.removeCurrentId();
    }
}