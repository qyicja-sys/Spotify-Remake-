package com.ty1l.spotify_remake.interceptor;

import com.ty1l.spotify_remake.utility.SignedUrlUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 签名 URL 验证过滤器
 *
 * 拦截歌曲文件请求，验证 URL 中的签名参数。
 * 封面图不受影响（保持公开访问）。
 *
 * 请求流程：
 *   前端请求 /static/datas/musicResouces/musics/song.mp3?expires=xxx&sig=yyy
 *         ↓
 *   本 Filter 拦截 → 验证 expires + sig
 *         ↓ 合法
 *   Spring 静态资源处理器返回文件
 *         ↓ 不合法
 *   返回 403
 */
@Component
@Order(1) // 确保在其他 Filter 之前执行
public class SignedUrlFilter implements Filter {

    // 需要签名保护的路径前缀
    private static final String[] PROTECTED_PREFIXES = {
        "/static/datas/musicResouces/musics/",       // 本地歌曲文件
        "/spotify/external/stream-proxy/"             // 外部歌曲流代理
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = URLDecoder.decode(httpRequest.getRequestURI(), StandardCharsets.UTF_8);

        // 只拦截受保护路径下的请求
        boolean protected_ = false;
        for (String prefix : PROTECTED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                protected_ = true;
                break;
            }
        }
        if (!protected_) {
            chain.doFilter(request, response);
            return;
        }

        // 提取签名参数
        String expires = httpRequest.getParameter("expires");
        String sig = httpRequest.getParameter("sig");

        // 去掉查询参数，只保留资源路径进行签名验证
        // 注意：在反向代理场景下 URI 可能不含查询参数，所以直接用 URI
        if (SignedUrlUtil.verify(uri, expires, sig)) {
            // 签名合法，放行
            chain.doFilter(request, response);
        } else {
            // 签名非法或已过期
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":403, \"msg\":\"INVALID_OR_EXPIRED_SIGNATURE\"}");
        }
    }
}
