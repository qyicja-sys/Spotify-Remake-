package com.ty1l.spotify_remake.interceptor;

import com.ty1l.spotify_remake.utility.BaseContext;
import com.ty1l.spotify_remake.utility.JwtGenerate;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AdminTokenInterceptor implements HandlerInterceptor {

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
            String role = String.valueOf(claims.get("role"));
            if (!"admin".equals(role)) {
                log.info("Admin API: role is not admin, role={}", role);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403, \"msg\":\"FORBIDDEN\"}");
                return false;
            }
            Long adminId = Long.valueOf(claims.get("userId").toString());
            BaseContext.setCurrentId(adminId);
            return true;
        } catch (Exception e) {
            log.error("Admin API: token parse failed", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401, \"msg\":\"INVALID_TOKEN\"}");
            return false;
        }
    }
}
