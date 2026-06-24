package com.ty1l.spotify_remake.Controller.Admin;

import com.ty1l.spotify_remake.Entity.Public.Album;
import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Mapper.Public.AlbumMapper;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.utility.FileUploadUtil;
import com.ty1l.spotify_remake.utility.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/admin/spotify/albums")
public class AdminAlbumController {

    @Autowired
    private AlbumMapper albumMapper;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private SongMapper songMapper;

    /**
     * 获取所有艺术家列表（用于CMS专辑管理首页）
     */
    @GetMapping("/artists")
    public Result listArtists() {
        List<Artist> artists = artistMapper.findAll();
        // 只返回有专辑的艺术家，附带专辑数量
        List<Map<String, Object>> result = new ArrayList<>();
        for (Artist a : artists) {
            List<Album> albums = albumMapper.findByArtistId(a.getId().longValue());
            if (albums != null && !albums.isEmpty()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", a.getId());
                item.put("name", a.getName());
                item.put("avatarUrl", a.getAvatarUrl());
                item.put("avatarNetworkUrl", a.getAvatarNetworkUrl());
                item.put("albumCount", albums.size());
                result.add(item);
            }
        }
        return Result.success(result);
    }

    /**
     * 获取指定艺术家的专辑列表
     */
    @GetMapping("/artist/{artistId}")
    public Result listByArtist(@PathVariable Long artistId) {
        Artist artist = artistMapper.findById(artistId.intValue());
        if (artist == null) {
            return Result.error("艺术家不存在");
        }
        List<Album> albums = albumMapper.findByArtistId(artistId);
        if (albums == null) albums = Collections.emptyList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("artist", artist);
        data.put("albums", albums);
        return Result.success(data);
    }

    /**
     * 获取单个专辑详情
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        Album album = albumMapper.findById(id);
        if (album == null) {
            return Result.error("专辑不存在");
        }
        List<Song> songs = songMapper.findByAlbumId(id);
        if (songs == null) songs = Collections.emptyList();

        Artist artist = artistMapper.findById(album.getArtistId().intValue());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("album", album);
        data.put("artist", artist);
        data.put("songs", songs);
        return Result.success(data);
    }

    /**
     * 创建专辑
     */
    @PostMapping
    public Result add(@RequestBody Album album) {
        log.info("Add album: {}", album.getName());
        if (album.getReleaseDate() == null) {
            album.setReleaseDate(java.time.LocalDate.now());
        }
        albumMapper.insert(album);
        return Result.success(Map.of("id", album.getId()));
    }

    /**
     * 更新专辑
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody Album album) {
        album.setId(id);
        log.info("Update album: {}", id);
        albumMapper.update(album);
        return Result.success("Album updated successfully");
    }

    /**
     * 删除专辑
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        log.info("Delete album: {}", id);
        // 先解除歌曲关联
        List<Song> songs = songMapper.findByAlbumId(id);
        if (songs != null) {
            for (Song s : songs) {
                s.setAlbumId(null);
                songMapper.update(s);
            }
        }
        albumMapper.deleteById(id);
        return Result.success("Album deleted successfully");
    }

    /**
     * 上传专辑封面
     */
    @PostMapping("/upload-cover")
    public Result uploadCover(
            @RequestParam("cover") MultipartFile coverFile,
            @RequestParam("albumName") String albumName) {
        try {
            String coverUrl = FileUploadUtil.saveAlbumCover(coverFile, albumName);
            return Result.success(Map.of("coverUrl", coverUrl));
        } catch (Exception e) {
            log.error("Album cover upload failed", e);
            return Result.error("封面上传失败: " + e.getMessage());
        }
    }
}
