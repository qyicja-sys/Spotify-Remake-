package com.ty1l.spotify_remake.interceptor;

import com.ty1l.spotify_remake.utility.BaseContext;
import com.ty1l.spotify_remake.utility.JwtGenerate;
import com.ty1l.spotify_remake.utility.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AdminTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String jwt = request.getHeader("token");

        if (!StringUtils.hasLength(jwt)) {
            log.info("Admin API: token is empty");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401, \"msg\":\"NOT_LOGIN\"}");
            return false;
        }

        try {
            Claims claims = JwtGenerate.parseJwt(jwt);

            // 校验 token 类型必须为 access
            String type = String.valueOf(claims.get("type"));
            if (!"access".equals(type)) {
                log.info("Admin API: token type is not access, type={}", type);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401, \"msg\":\"INVALID_TOKEN_TYPE\"}");
                return false;
            }

            // 校验角色
            String role = String.valueOf(claims.get("role"));
            if (!"admin".equals(role)) {
                log.info("Admin API: role is not admin, role={}", role);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403, \"msg\":\"FORBIDDEN\"}");
                return false;
            }

            // 获取 adminId 和 version
            Long adminId = Long.valueOf(claims.get("userId").toString());
            String version = String.valueOf(claims.get("version"));

            // Redis 双重校验：比对 version 是否与 Redis 中一致
            tokenService.validateAdminVersion(adminId, version);

            BaseContext.setCurrentId(adminId);
            BaseContext.setCurrentVersion(version);
            return true;
        } catch (TokenService.TokenInvalidException e) {
            log.info("Admin API: Token 双重校验失败: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            if ("ACCOUNT_LOGGED_IN_ELSEWHERE".equals(e.getMessage())) {
                response.getWriter().write("{\"code\":401, \"msg\":\"ACCOUNT_LOGGED_IN_ELSEWHERE\"}");
            } else {
                response.getWriter().write("{\"code\":401, \"msg\":\"TOKEN_EXPIRED\"}");
            }
            return false;
        } catch (Exception e) {
            log.error("Admin API: token parse failed", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401, \"msg\":\"INVALID_TOKEN\"}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}