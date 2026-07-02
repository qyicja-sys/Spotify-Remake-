package com.ty1l.spotify_remake.Controller.Public;

import com.ty1l.spotify_remake.Entity.Public.ExternalTrackVO;
import com.ty1l.spotify_remake.Service.Public.ExternalMusicService;
import com.ty1l.spotify_remake.utility.Result;
import jakarta.servlet.http.HttpServletRequest;
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

    @GetMapping("/artist-search")
    public Result artistSearch(@RequestParam("artistName") String artistName) {
        if (artistName == null || artistName.isBlank()) {
            return Result.success(java.util.Collections.emptyList());
        }
        List<ExternalTrackVO> results = externalMusicService.searchByArtist(artistName.trim());
        return Result.success(results);
    }

    @GetMapping("/stream-proxy/{source}/{trackId}")
    public void streamProxy(@PathVariable String source,
                            @PathVariable String trackId,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        String realUrl = externalMusicService.getRealStreamUrl(source, trackId);
        if (realUrl == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 客户端 Range 请求头（用于支持拖拽进度条 seek）
        String rangeHeader = request.getHeader("Range");

        HttpURLConnection conn = null;
        try {
            // 手动处理重定向，不使用 auto-redirect（避免跨域重定向失败）
            conn = openConnection(realUrl, 5000, 10000);
            if (rangeHeader != null && !rangeHeader.isEmpty()) {
                conn.setRequestProperty("Range", rangeHeader);
            }
            int status = conn.getResponseCode();

            // 跟随最多 3 次重定向
            int redirects = 0;
            while ((status == 301 || status == 302 || status == 303 || status == 307) && redirects < 3) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null) break;
                conn = openConnection(location, 5000, 10000);
                if (rangeHeader != null && !rangeHeader.isEmpty()) {
                    conn.setRequestProperty("Range", rangeHeader);
                }
                status = conn.getResponseCode();
                redirects++;
            }

            if (status != 200 && status != 206) {
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                return;
            }

            // 转发响应状态码（206 Partial Content 或 200 OK）
            response.setStatus(status);

            String contentType = conn.getContentType();
            if (contentType == null) contentType = "audio/mpeg";
            response.setContentType(contentType);

            // 转发 Content-Length（浏览器需要它来确定音频总时长和 seek 范围）
            String contentLength = conn.getHeaderField("Content-Length");
            if (contentLength != null) {
                response.setContentLengthLong(Long.parseLong(contentLength));
            }

            // 转发 Accept-Ranges（告知浏览器支持分段请求）
            String acceptRanges = conn.getHeaderField("Accept-Ranges");
            response.setHeader("Accept-Ranges", acceptRanges != null ? acceptRanges : "bytes");

            // 转发 Content-Range（206 响应时必须）
            String contentRange = conn.getHeaderField("Content-Range");
            if (contentRange != null) {
                response.setHeader("Content-Range", contentRange);
            }

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
