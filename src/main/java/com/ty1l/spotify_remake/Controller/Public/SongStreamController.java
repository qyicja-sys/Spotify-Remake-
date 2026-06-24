package com.ty1l.spotify_remake.Controller.Public;

import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Entity.User.UserPlaybackHistory;
import com.ty1l.spotify_remake.Mapper.User.PlaybackHistoryMapper;
import com.ty1l.spotify_remake.Service.Public.SongService;
import com.ty1l.spotify_remake.utility.BaseContext;
import com.ty1l.spotify_remake.utility.Result;
import com.ty1l.spotify_remake.utility.SignedUrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 歌曲流媒体控制器
 *
 * 提供签名 URL 接口，前端获取临时 URL 后直接加载音频，
 * 无需在每次请求中携带 token。
 *
 * 使用流程：
 * 1. 前端调用 GET /stream/songs/{id}/stream-url（带 token）
 * 2. 后端返回带签名的临时 URL（5 分钟有效）
 * 3. 前端用临时 URL 设置 <audio src> 播放
 */

// 歌曲流媒体控制器，新版接口，新版方案
@RestController
@RequestMapping("/stream/songs")
public class SongStreamController {

    @Autowired
    private SongService songService;

    @Autowired
    private PlaybackHistoryMapper playbackHistoryMapper;

    /**
     * 获取歌曲的签名播放 URL
     *
     * @param id 歌曲 ID
     * @return 包含签名 URL 的响应
     */
    @GetMapping("/{id}/stream-url")
    public Result getStreamUrl(@PathVariable Integer id) {
        Song song = songService.findById(id);
        if (song == null) {
            return Result.error("歌曲不存在");
        }

        // 记录播放历史
        Long userId = BaseContext.getCurrentId();
        if (userId != null && song.getArtistId() != null) {
            UserPlaybackHistory history = new UserPlaybackHistory();
            history.setUserId(userId);
            history.setSongId(song.getId().longValue());
            history.setArtistId(song.getArtistId().longValue());
            history.setPlayedAt(LocalDateTime.now());
            playbackHistoryMapper.insertPlaybackHistory(history);
        }

        // 生成签名 URL（5 分钟有效）
        String signedFileUrl = SignedUrlUtil.generate(song.getFileUrl());
        String signedCoverUrl = SignedUrlUtil.generate(song.getCoverUrl());

        Map<String, Object> data = new HashMap<>();
        data.put("id", song.getId());
        data.put("title", song.getTitle());
        data.put("duration", song.getDuration());
        data.put("streamUrl", signedFileUrl);
        data.put("coverUrl", signedCoverUrl);
        data.put("expiresIn", 300); // 提示前端有效期

        return Result.success(data);
    }
}
