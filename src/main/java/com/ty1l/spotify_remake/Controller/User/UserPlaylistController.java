package com.ty1l.spotify_remake.Controller.User;

import com.ty1l.spotify_remake.Entity.Public.ExternalTrackVO;
import com.ty1l.spotify_remake.Service.User.UserPlaylistService;
import com.ty1l.spotify_remake.utility.BaseContext;
import com.ty1l.spotify_remake.utility.FileUploadUtil;
import com.ty1l.spotify_remake.utility.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/spotify/playlist")
public class UserPlaylistController {

    @Autowired
    private UserPlaylistService userPlaylistService;

    /**
     * 收藏本地歌曲
     */
    @PostMapping("/like")
    public Result likeSong(@RequestBody Map<String, Integer> body) {
        Long userId = BaseContext.getCurrentId();
        Integer songId = body.get("songId");
        if (songId == null) {
            return Result.errorClient("songId 不能为空");
        }
        try {
            userPlaylistService.likeSong(userId, songId);
            return Result.success();
        } catch (Exception e) {
            log.error("收藏歌曲失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 收藏外部歌曲
     */
    @PostMapping("/like-external")
    public Result likeExternalSong(@RequestBody ExternalTrackVO track) {
        Long userId = BaseContext.getCurrentId();
        if (track.getExternalId() == null || track.getSource() == null) {
            return Result.errorClient("外部歌曲信息不完整");
        }
        try {
            Integer songId = userPlaylistService.likeExternalSong(userId, track);
            return Result.success(Map.of("songId", songId));
        } catch (Exception e) {
            log.error("收藏外部歌曲失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/like/{songId}")
    public Result unlikeSong(@PathVariable Integer songId) {
        Long userId = BaseContext.getCurrentId();
        try {
            userPlaylistService.unlikeSong(userId, songId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消收藏失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查是否已收藏
     */
    @GetMapping("/like/check/{songId}")
    public Result isLiked(@PathVariable Integer songId) {
        Long userId = BaseContext.getCurrentId();
        boolean liked = userPlaylistService.isLiked(userId, songId);
        return Result.success(Map.of("liked", liked));
    }

    /**
     * 添加歌曲到指定歌单
     */
    @PostMapping("/{playlistId}/add-song")
    public Result addSongToPlaylist(@PathVariable Integer playlistId, @RequestBody Map<String, Integer> body) {
        Long userId = BaseContext.getCurrentId();
        Integer songId = body.get("songId");
        if (songId == null) {
            return Result.errorClient("songId 不能为空");
        }
        try {
            userPlaylistService.addSongToPlaylist(userId, playlistId, songId);
            return Result.success();
        } catch (Exception e) {
            log.error("添加歌曲到歌单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 从歌单中移除歌曲
     */
    @DeleteMapping("/{playlistId}/songs/{songId}")
    public Result removeSongFromPlaylist(@PathVariable Integer playlistId, @PathVariable Integer songId) {
        Long userId = BaseContext.getCurrentId();
        try {
            userPlaylistService.removeSongFromPlaylist(userId, playlistId, songId);
            return Result.success();
        } catch (Exception e) {
            log.error("从歌单移除歌曲失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 编辑用户自建歌单详情
     */
    @PostMapping("/{playlistId}/edit")
    public Result editPlaylist(@PathVariable Integer playlistId,
                               @RequestParam("title") String title,
                               @RequestParam(value = "profile", required = false) String profile,
                               @RequestParam(value = "cover", required = false) MultipartFile cover) {
        Long userId = BaseContext.getCurrentId();
        try {
            String coverUrl = null;
            if (cover != null && !cover.isEmpty()) {
                coverUrl = FileUploadUtil.savePersonalPlaylistCover(cover, userId, playlistId);
            }
            userPlaylistService.editPlaylist(userId, playlistId, title, profile, coverUrl);
            return Result.success("歌单已更新");
        } catch (Exception e) {
            log.error("编辑歌单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除用户自建歌单
     */
    @DeleteMapping("/{playlistId}")
    public Result deletePlaylist(@PathVariable Integer playlistId) {
        Long userId = BaseContext.getCurrentId();
        try {
            userPlaylistService.deletePlaylist(userId, playlistId);
            return Result.success("歌单已删除");
        } catch (Exception e) {
            log.error("删除歌单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 收藏歌单到音乐库
     */
    @PostMapping("/{playlistId}/collect")
    public Result collectPlaylist(@PathVariable Integer playlistId) {
        Long userId = BaseContext.getCurrentId();
        try {
            userPlaylistService.collectPlaylist(userId, playlistId);
            return Result.success();
        } catch (Exception e) {
            log.error("收藏歌单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消收藏歌单
     */
    @DeleteMapping("/{playlistId}/collect")
    public Result uncollectPlaylist(@PathVariable Integer playlistId) {
        Long userId = BaseContext.getCurrentId();
        try {
            userPlaylistService.uncollectPlaylist(userId, playlistId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消收藏歌单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查歌单是否已收藏
     */
    @GetMapping("/{playlistId}/collect/check")
    public Result isPlaylistCollected(@PathVariable Integer playlistId) {
        Long userId = BaseContext.getCurrentId();
        boolean collected = userPlaylistService.isPlaylistCollected(userId, playlistId);
        return Result.success(Map.of("collected", collected));
    }

    /**
     * 切换歌单公开/私密状态
     */
    @PutMapping("/{playlistId}/privacy")
    public Result togglePrivacy(@PathVariable Integer playlistId) {
        Long userId = BaseContext.getCurrentId();
        try {
            userPlaylistService.togglePlaylistPrivacy(userId, playlistId);
            return Result.success();
        } catch (Exception e) {
            log.error("切换歌单隐私状态失败", e);
            return Result.error(e.getMessage());
        }
    }
}
