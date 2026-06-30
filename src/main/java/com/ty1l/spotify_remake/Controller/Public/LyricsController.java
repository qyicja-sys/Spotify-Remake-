package com.ty1l.spotify_remake.Controller.Public;

import com.ty1l.spotify_remake.Service.Public.LyricsService;
import com.ty1l.spotify_remake.utility.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 歌词控制器
 *
 * 提供本地歌曲和外部歌曲的歌词获取接口。
 * 返回 LRC 格式歌词文本，前端自行解析和滚动。
 */
@RestController
@RequestMapping("/spotify/lyrics")
public class LyricsController {

    @Autowired
    private LyricsService lyricsService;

    /**
     * 获取本地歌曲歌词
     *
     * @param title 歌曲名称（模糊匹配，取第一条结果）
     * @return { "lyric": "[00:00.00]歌词内容\n..." }
     */
    @GetMapping
    public Result getLocalLyrics(@RequestParam("title") String title) {
        Map<String, String> data = lyricsService.getLocalLyrics(title);
        if (data == null) {
            return Result.error("未找到歌词");
        }
        return Result.success(data);
    }

    /**
     * 获取外部歌曲歌词（GD Music API）
     *
     * @param source  音乐源标识 (gdmusic)
     * @param lyricId 歌词ID
     * @return { "lyric": "[00:00.00]歌词内容\n..." }
     */
    @GetMapping("/external")
    public Result getExternalLyrics(@RequestParam("source") String source,
                                    @RequestParam("lyricId") String lyricId) {
        Map<String, String> data = lyricsService.getExternalLyrics(source, lyricId);
        if (data == null) {
            return Result.error("未找到歌词");
        }
        return Result.success(data);
    }
}
