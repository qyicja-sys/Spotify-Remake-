package com.ty1l.spotify_remake.Controller.Admin;

import com.ty1l.spotify_remake.Entity.Admin.AdminPlaylist;
import com.ty1l.spotify_remake.Entity.User.PlaylistDetailVO;
import com.ty1l.spotify_remake.Entity.User.PlaylistSongVO;
import com.ty1l.spotify_remake.Service.CacheService;
import com.ty1l.spotify_remake.Service.Public.PlaylistService;
import com.ty1l.spotify_remake.utility.FileUploadUtil;
import com.ty1l.spotify_remake.utility.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@Slf4j
@RestController
@RequestMapping("/admin/spotify/playlists")
public class AdminPlaylistController {

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private CacheService cacheService;

    @GetMapping
    public Result list(@RequestParam(required = false) Integer type,
                       @RequestParam(required = false) Boolean systemOnly) {
        List<AdminPlaylist> playlists;
        if (type != null) {
            playlists = playlistService.findByType(type);
        } else if (Boolean.TRUE.equals(systemOnly)) {
            playlists = playlistService.findAllSystem();
        } else {
            playlists = playlistService.findAll();
        }
        return Result.success(playlists);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        // 使用 findDetailById 获取歌单详情（含歌曲列表和艺术家信息）
        PlaylistDetailVO detail = playlistService.findDetailById(id);
        if (detail == null) {
            return Result.error("歌单不存在");
        }
        // 将 PlaylistDetailVO 的字段平铺到 Map 中，保持前端兼容
        Map<String, Object> data = new HashMap<>();
        data.put("id", detail.getId());
        data.put("title", detail.getTitle());
        data.put("coverUrl", detail.getCoverUrl());
        data.put("backgroundUrl", detail.getBackgroundUrl());
        data.put("type", detail.getType());
        data.put("profile", detail.getProfile());
        data.put("songCount", detail.getSongCount());
        data.put("totalDuration", detail.getTotalDuration());
        data.put("songList", detail.getSongs());
        return Result.success(data);
    }

    @PostMapping
    public Result add(@RequestBody AdminPlaylist playlist) {
        log.info("Add playlist: {}", playlist);
        playlistService.add(playlist);
        cacheService.incrVersion(CacheService.KEY_VERSION_HOME);
        return Result.success("Playlist created successfully");
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody AdminPlaylist playlist) {
        playlist.setId(id);
        log.info("Update playlist: {}", playlist);
        playlistService.update(playlist);
        cacheService.incrVersion(CacheService.KEY_VERSION_HOME);
        return Result.success("Playlist updated successfully");
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        log.info("Delete playlist id: {}", id);
        playlistService.delete(id);
        cacheService.incrVersion(CacheService.KEY_VERSION_HOME);
        return Result.success("Playlist deleted successfully");
    }

    @PostMapping("/{id}/songs")
    public Result addSong(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer songId = body.get("songId");
        log.info("Add song {} to playlist {}", songId, id);
        playlistService.addSong(id, songId);
        cacheService.incrVersion(CacheService.KEY_VERSION_HOME);
        return Result.success("Song added to playlist");
    }

    @DeleteMapping("/{id}/songs/{songId}")
    public Result removeSong(@PathVariable Integer id, @PathVariable Integer songId) {
        log.info("Remove song {} from playlist {}", songId, id);
        playlistService.removeSong(id, songId);
        cacheService.incrVersion(CacheService.KEY_VERSION_HOME);
        return Result.success("Song removed from playlist");
    }

    @PostMapping("/upload-cover")
    public Result uploadCover(
            @RequestParam("cover") MultipartFile coverFile,
            @RequestParam("filename") String filename) {
        try {
            String coverUrl = FileUploadUtil.savePlaylistCover(coverFile, filename);
            Map<String, String> result = new HashMap<>();
            result.put("coverUrl", coverUrl);
            log.info("Uploaded playlist cover: {}", coverUrl);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Playlist cover upload failed", e);
            return Result.error("封面上传失败: " + e.getMessage());
        }
    }
}
