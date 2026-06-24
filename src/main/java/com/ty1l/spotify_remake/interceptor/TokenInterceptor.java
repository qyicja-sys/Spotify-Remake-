package com.ty1l.spotify_remake.interceptor;

import com.ty1l.spotify_remake.utility.BaseContext;
import com.ty1l.spotify_remake.utility.JwtGenerate;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;


import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {
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
            Long userId = Long.valueOf(claims.get("id").toString());
            // 存入当前线程，方便后续使用此token识别当前用户
            BaseContext.setCurrentId(userId);
        } catch (Exception e) {
            e.printStackTrace();
            log.info("解析令牌失败, 返回错误结果");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        //4. 放行。
        log.info("令牌合法, 放行: {}", url);
        return true;
    }
}