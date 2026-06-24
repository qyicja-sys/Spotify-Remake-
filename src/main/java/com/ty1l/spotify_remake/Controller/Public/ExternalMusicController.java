package com.ty1l.spotify_remake.Controller.Public;

import com.ty1l.spotify_remake.Entity.Public.ExternalTrackVO;
import com.ty1l.spotify_remake.Service.Public.ExternalMusicService;
import com.ty1l.spotify_remake.utility.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@RestController
@RequestMapping("/spotify/external")
public class ExternalMusicController {

    @Autowired
    private ExternalMusicService externalMusicService;

    @GetMapping("/search")
    public Result search(@RequestParam("keyword") String keyword) {
        List<ExternalTrackVO> results = externalMusicService.search(keyword);
        return Result.success(results);
    }

    @GetMapping("/stream-proxy/{source}/{trackId}")
    public void streamProxy(@PathVariable String source,
                            @PathVariable String trackId,
                            HttpServletResponse response) {
        String realUrl = externalMusicService.getRealStreamUrl(source, trackId);
        if (realUrl == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        HttpURLConnection conn = null;
        try {
            // 手动处理重定向，不使用 auto-redirect（避免跨域重定向失败）
            conn = openConnection(realUrl, 5000, 10000);
            int status = conn.getResponseCode();

            // 跟随最多 3 次重定向
            int redirects = 0;
            while ((status == 301 || status == 302 || status == 303 || status == 307) && redirects < 3) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null) break;
                conn = openConnection(location, 5000, 10000);
                status = conn.getResponseCode();
                redirects++;
            }

            if (status != 200) {
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                return;
            }

            String contentType = conn.getContentType();
            if (contentType == null) contentType = "audio/mpeg";
            response.setContentType(contentType);
            response.setHeader("Accept-Ranges", "bytes");

            try (InputStream in = conn.getInputStream();
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
        } catch (Exception e) {
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private HttpURLConnection openConnection(String urlStr, int connectTimeout, int readTimeout) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false); // 手动处理重定向
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Referer", "https://music.163.com/");
        return conn;
    }
}
