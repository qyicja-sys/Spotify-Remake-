package com.ty1l.spotify_remake.Controller.User;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ty1l.spotify_remake.Entity.Public.*;
import com.ty1l.spotify_remake.Entity.User.User;
import com.ty1l.spotify_remake.Mapper.Public.AlbumMapper;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.Mapper.User.UserMapper;
import com.ty1l.spotify_remake.utility.BaseContext;
import com.ty1l.spotify_remake.utility.FileUploadUtil;
import com.ty1l.spotify_remake.utility.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/spotify")
public class UserAlbumController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private AlbumMapper albumMapper;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private SongArtistMapper songArtistMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 搜索当前艺术家的歌曲（用于选择已有歌曲加入专辑）
     */
    @GetMapping("/profile/my-songs/search")
    public ResponseEntity<Result> searchMySongs(@RequestParam(value = "keyword", defaultValue = "") String keyword) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        User user = userMapper.findById(userId);
        if (user == null || user.getIsArtist() == null || user.getIsArtist() != 1) {
            return ResponseEntity.badRequest().body(Result.error("只有艺术家才能搜索歌曲"));
        }
        Artist artist = artistMapper.findByName(user.getNickName().trim());
        if (artist == null) {
            return ResponseEntity.badRequest().body(Result.error("未找到对应的艺术家信息"));
        }
        try {
            List<SearchSongVO> songs;
            if (keyword.isBlank()) {
                // 无关键词，返回该艺术家的所有歌曲
                songs = songMapper.findByArtistIdWithNames(artist.getId());
            } else {
                // 有关键词，在该艺术家的歌曲中模糊搜索
                songs = songMapper.findByArtistIdWithNames(artist.getId());
                String lowerKeyword = keyword.toLowerCase();
                songs = songs.stream()
                        .filter(s -> s.getTitle() != null && s.getTitle().toLowerCase().contains(lowerKeyword))
                        .collect(Collectors.toList());
            }
            if (songs == null) songs = Collections.emptyList();
            return ResponseEntity.ok(Result.success(songs));
        } catch (Exception e) {
            log.error("搜索歌曲失败", e);
            return ResponseEntity.status(500).body(Result.error("搜索歌曲失败"));
        }
    }

    /**
     * 创建专辑并关联歌曲
     */
    @PostMapping("/profile/create-album")
    public ResponseEntity<Result> createAlbum(
            @RequestParam("name") String name,
            @RequestParam("cover") MultipartFile coverFile,
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestParam(value = "type", defaultValue = "2") Integer type,
            @RequestParam(value = "existingSongIds", required = false) List<Integer> existingSongIds,
            @RequestParam(value = "newSongFiles", required = false) List<MultipartFile> newSongFiles,
            @RequestParam(value = "newSongTitles", required = false) List<String> newSongTitles,
            @RequestParam(value = "newSongDurations", required = false) String newSongDurationsJson,
            @RequestParam(value = "featuredArtistIds", required = false) List<Integer> featuredArtistIds) {

        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Result.errorClient("专辑名不能为空"));
        }
        if (coverFile == null || coverFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.errorClient("请上传专辑封面"));
        }

        // 验证用户是艺术家
        User user = userMapper.findById(userId);
        if (user == null || user.getIsArtist() == null || user.getIsArtist() != 1) {
            return ResponseEntity.badRequest().body(Result.error("只有注册艺术家才能创建专辑"));
        }
        Artist artist = artistMapper.findByName(user.getNickName().trim());
        if (artist == null) {
            return ResponseEntity.badRequest().body(Result.error("未找到对应的艺术家信息"));
        }

        // 校验：至少要有歌曲
        boolean hasExistingSongs = existingSongIds != null && !existingSongIds.isEmpty();
        boolean hasNewSongs = newSongFiles != null && !newSongFiles.isEmpty();
        if (!hasExistingSongs && !hasNewSongs) {
            return ResponseEntity.badRequest().body(Result.error("专辑至少需要包含一首歌曲"));
        }

        try {
            // 1. 保存封面
            String coverUrl = FileUploadUtil.saveAlbumCover(coverFile, name.trim());

            // 2. 创建专辑记录
            Album album = new Album();
            album.setArtistId(artist.getId().longValue());
            album.setName(name.trim());
            album.setCoverUrl(coverUrl);
            album.setDescription(description.isBlank() ? null : description.trim());
            album.setType(type);
            album.setReleaseDate(java.time.LocalDate.now());
            albumMapper.insert(album);
            Long albumId = album.getId();

            // 3. 解析新歌曲时长
            List<Integer> newSongDurations = null;
            if (newSongDurationsJson != null && !newSongDurationsJson.isBlank()) {
                try {
                    newSongDurations = objectMapper.readValue(newSongDurationsJson, new TypeReference<List<Integer>>() {});
                } catch (Exception e) {
                    log.warn("解析歌曲时长失败，将使用默认值", e);
                }
            }

            // 4. 处理已有歌曲：更新 albumId 和 coverUrl
            if (hasExistingSongs) {
                for (Integer songId : existingSongIds) {
                    if (songId != null) {
                        songMapper.updateAlbumIdAndCover(songId, albumId, coverUrl);
                    }
                }
            }

            // 5. 构建完整艺术家名（含联名），用于文件命名
            String fullArtistName = artist.getName();
            if (featuredArtistIds != null && !featuredArtistIds.isEmpty()) {
                StringBuilder nameBuilder = new StringBuilder(artist.getName());
                for (Integer featId : featuredArtistIds) {
                    if (featId != null && !featId.equals(artist.getId())) {
                        Artist featArtist = artistMapper.findById(featId);
                        if (featArtist != null) {
                            nameBuilder.append(", ").append(featArtist.getName());
                        }
                    }
                }
                fullArtistName = nameBuilder.toString();
            }

            // 6. 处理新上传的歌曲
            if (hasNewSongs) {
                for (int i = 0; i < newSongFiles.size(); i++) {
                    MultipartFile musicFile = newSongFiles.get(i);
                    if (musicFile == null || musicFile.isEmpty()) continue;

                    String songTitle = (newSongTitles != null && i < newSongTitles.size() && !newSongTitles.get(i).isBlank())
                            ? newSongTitles.get(i).trim()
                            : ("Track " + (i + 1));

                    // 保存音频文件，文件名：艺术家1, 艺术家2 - 歌曲名
                    String fileUrl = FileUploadUtil.saveMusic(musicFile, fullArtistName, songTitle);

                    // 获取时长
                    Integer duration = null;
                    if (newSongDurations != null && i < newSongDurations.size()) {
                        duration = newSongDurations.get(i);
                    }

                    // 创建歌曲记录（封面使用专辑封面）
                    Song song = new Song();
                    song.setTitle(songTitle);
                    song.setArtistId(artist.getId());
                    song.setAlbumId(albumId);
                    song.setCoverUrl(coverUrl);
                    song.setFileUrl(fileUrl);
                    song.setDuration(duration);
                    songMapper.insert(song);

                    // 保存歌曲-艺术家关系（主艺术家）
                    songArtistMapper.insert(new SongArtist(song.getId(), artist.getId(), 1));

                    // 保存联名艺术家
                    if (featuredArtistIds != null) {
                        for (Integer featId : featuredArtistIds) {
                            if (featId != null && !featId.equals(artist.getId())) {
                                songArtistMapper.insert(new SongArtist(song.getId(), featId, 0));
                            }
                        }
                    }
                }
            }

            log.info("艺术家 {} 创建了专辑: {}，包含 {} 首已有歌曲和 {} 首新歌曲",
                    artist.getName(), name.trim(),
                    hasExistingSongs ? existingSongIds.size() : 0,
                    hasNewSongs ? newSongFiles.size() : 0);

            return ResponseEntity.ok(Result.success(Map.of("albumId", albumId)));
        } catch (Exception e) {
            log.error("创建专辑失败", e);
            return ResponseEntity.status(500).body(Result.error("创建专辑失败: " + e.getMessage()));
        }
    }

    /**
     * 获取专辑详情（公开）
     */
    @GetMapping("/album/{id}")
    public ResponseEntity<Result> getAlbumDetail(@PathVariable Long id) {
        try {
            Album album = albumMapper.findById(id);
            if (album == null) {
                return ResponseEntity.badRequest().body(Result.error("专辑不存在"));
            }
            List<Song> songs = songMapper.findByAlbumId(id);
            if (songs == null) songs = Collections.emptyList();

            // 获取艺术家信息
            Artist artist = artistMapper.findById(album.getArtistId().intValue());

            return ResponseEntity.ok(Result.success(Map.of(
                    "album", album,
                    "songs", songs,
                    "artist", artist != null ? artist : new Artist()
            )));
        } catch (Exception e) {
            log.error("获取专辑详情失败", e);
            return ResponseEntity.status(500).body(Result.error("获取专辑详情失败"));
        }
    }

    /**
     * 获取艺术家的专辑列表（公开）
     */
    @GetMapping("/artist/{id}/albums")
    public ResponseEntity<Result> getArtistAlbums(@PathVariable Long id) {
        try {
            List<Album> albums = albumMapper.findByArtistId(id);
            if (albums == null) albums = Collections.emptyList();
            return ResponseEntity.ok(Result.success(albums));
        } catch (Exception e) {
            log.error("获取艺术家专辑列表失败", e);
            return ResponseEntity.status(500).body(Result.error("获取艺术家专辑列表失败"));
        }
    }
}
