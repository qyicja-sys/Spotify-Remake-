package com.ty1l.spotify_remake.Controller.Public;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Entity.Public.SongArtist;
import com.ty1l.spotify_remake.Service.CacheService;
import com.ty1l.spotify_remake.Service.Public.ArtistService;
import com.ty1l.spotify_remake.Mapper.Public.SongArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.Service.Public.SongService;
import com.ty1l.spotify_remake.Service.User.PlaybackHistoryService;
import com.ty1l.spotify_remake.utility.BaseContext;
import com.ty1l.spotify_remake.utility.Result;
import com.ty1l.spotify_remake.utility.SignedUrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
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
    private SongMapper songMapper;

    @Autowired
    private SongArtistMapper songArtistMapper;

    @Autowired
    private ArtistService artistService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PlaybackHistoryService playbackHistoryService;

    private static final Logger log = LoggerFactory.getLogger(SongStreamController.class);

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

        // 播放历史由前端异步调用 /spotify/playback/record 记录

        // 校验文件 URL，防止对 null 生成签名 URL
        if (song.getFileUrl() == null || song.getFileUrl().isBlank()) {
            return Result.error("歌曲文件不存在");
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

    /**
     * 获取外部歌曲的签名流代理 URL
     *
     * 与本地歌曲一致：前端调用此接口获取带签名的临时 URL，
     * 然后用临时 URL 直接加载音频。
     *
     * @param source 外部来源（如 gdmusic）
     * @param externalId 外部歌曲 ID
     * @return 包含签名 URL 的响应
     */
    @GetMapping("/external/{source}/{externalId}/stream-url")
    public Result getExternalStreamUrl(@PathVariable String source, @PathVariable String externalId) {
        if (source == null || source.isBlank() || externalId == null || externalId.isBlank()) {
            return Result.error("参数不能为空");
        }

        // 记录播放历史（需要登录）
        Long userId = BaseContext.getCurrentId();

        // 生成签名 URL（5 分钟有效）
        String proxyPath = "/spotify/external/stream-proxy/" + source + "/" + externalId;
        String signedUrl = SignedUrlUtil.generate(proxyPath);

        Map<String, Object> data = new HashMap<>();
        data.put("streamUrl", signedUrl);
        data.put("expiresIn", 300);

        return Result.success(data);
    }

    /**
     * 记录外部歌曲播放历史
     * 前端播放外部歌曲时调用，fire-and-forget 模式
     * 支持多艺人名（逗号分隔），每个艺人独立记录播放历史
     */
    @PostMapping("/external/record-play")
    public Result recordExternalPlay(@RequestBody Map<String, Object> body) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("未登录");
            }

            String source = (String) body.get("source");
            String externalId = (String) body.get("externalId");
            String title = (String) body.get("title");
            String artistName = (String) body.get("artistName");
            String coverUrl = (String) body.get("coverUrl");
            String picId = (String) body.get("picId");
            Integer duration = body.get("duration") != null ? ((Number) body.get("duration")).intValue() : null;

            // 如果封面为空但有 picId，重新获取封面
            if ((coverUrl == null || coverUrl.isBlank()) && picId != null && !picId.isBlank()) {
                try {
                    String picUrl = String.format("%s/api.php?types=pic&source=netease&id=%s&size=300",
                            "https://music-api.gdstudio.xyz", picId);
                    String json = new org.springframework.web.client.RestTemplate().getForObject(picUrl, String.class);
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = om.readTree(json);
                    coverUrl = root.path("url").asText(null);
                } catch (Exception ignored) {}
            }
            if (source == null || source.isBlank() || externalId == null || externalId.isBlank()) {
                return Result.error("source 和 externalId 不能为空");
            }

            if (artistName == null || artistName.isBlank() || "未知艺人".equals(artistName)) {
                return Result.success(); // 无艺人名则跳过，不记录
            }

            // 1. 解析艺人列表（按逗号分割），查找或创建艺人
            String[] artistNames = artistName.split(",");
            List<Artist> resolvedArtists = artistService.resolveOrCreateArtists(artistNames);
            Artist primaryArtist = resolvedArtists.isEmpty() ? null : resolvedArtists.get(0);

            // 2. 查找或创建 Song
            Song song = songMapper.findByExternalId(source, externalId);
            boolean isNewSong = false;
            if (song == null) {
                song = new Song();
                song.setTitle(title != null ? title : "Unknown");
                song.setCoverNetworkUrl(coverUrl);
                song.setDuration(duration);
                song.setExternalSource(source);
                song.setExternalId(externalId);
                song.setPicId(picId);
                song.setFileUrl("");
                song.setArtistId(primaryArtist != null ? primaryArtist.getId() : null);
                songMapper.insert(song);
                isNewSong = true;
            } else if (coverUrl != null && !coverUrl.isBlank()) {
                song.setCoverNetworkUrl(coverUrl);
                if (picId != null) song.setPicId(picId);
                songMapper.update(song);
            }
            // 如果已有歌曲但封面为空，且有 picId，重新获取封面
            if (song.getCoverNetworkUrl() == null || song.getCoverNetworkUrl().isBlank()) {
                String effectivePicId = picId != null ? picId : song.getPicId();
                if (effectivePicId != null && !effectivePicId.isBlank()) {
                    try {
                        String picUrl = String.format("https://music-api.gdstudio.xyz/api.php?types=pic&source=netease&id=%s&size=300", effectivePicId);
                        String json = new org.springframework.web.client.RestTemplate().getForObject(picUrl, String.class);
                        com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
                        String fetchedCover = root.path("url").asText(null);
                        if (fetchedCover != null && !fetchedCover.isBlank()) {
                            song.setCoverNetworkUrl(fetchedCover);
                            if (song.getPicId() == null) song.setPicId(effectivePicId);
                            songMapper.update(song);
                        }
                    } catch (Exception ignored) {}
                }
            }

            // 3. 维护 song_artist 关系并记录播放历史
            for (int i = 0; i < resolvedArtists.size(); i++) {
                Artist artist = resolvedArtists.get(i);

                // 确保 song_artist 关系存在
                if (song.getId() != null && artist.getId() != null) {
                    List<SongArtist> existing = songArtistMapper.findBySongId(song.getId());
                    Artist finalArtist = artist;
                    boolean hasRelation = existing.stream().anyMatch(sa -> sa.getArtistId().equals(finalArtist.getId()));
                    if (!hasRelation) {
                        songArtistMapper.insert(new SongArtist(song.getId(), artist.getId(), i == 0 ? 1 : 0));
                    }
                }

            }

            // 写入 Redis ZSET + 异步落库 MySQL
            if (song.getId() != null) {
                playbackHistoryService.recordPlayback(userId, song.getId());
            }

            return Result.success();

        } catch (Exception e) {
            log.warn("记录外部歌曲播放历史失败: {}", e.getMessage(), e);
            return Result.success(); // 静默失败，不影响音乐播放
        }
    }
}
