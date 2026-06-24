package com.ty1l.spotify_remake.Controller.Admin;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Entity.Public.SongArtist;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongArtistMapper;
import com.ty1l.spotify_remake.Service.Public.SongService;
import com.ty1l.spotify_remake.utility.FileUploadUtil;
import com.ty1l.spotify_remake.utility.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@Slf4j
@RestController
@RequestMapping("/admin/spotify/songs")
public class AdminSongController {

    @Autowired
    private SongService songService;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private SongArtistMapper songArtistMapper;

    @GetMapping
    public Result list() {
        List<Song> songs = songService.findAll();
        // 为每首歌附加副艺术家信息
        List<Map<String, Object>> songList = songs.stream().map(song -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", song.getId());
            map.put("title", song.getTitle());
            map.put("artistId", song.getArtistId());
            map.put("coverUrl", song.getCoverUrl());
            map.put("fileUrl", song.getFileUrl());
            map.put("duration", song.getDuration());
            map.put("createdAt", song.getCreatedAt());

            // 查询副艺术家名称
            List<SongArtist> relations = songArtistMapper.findBySongId(song.getId());
            List<String> featuredNames = relations.stream()
                    .filter(sa -> sa.getIsMain() != null && sa.getIsMain() == 0)
                    .map(sa -> artistMapper.findById(sa.getArtistId()))
                    .filter(a -> a != null)
                    .map(Artist::getName)
                    .collect(Collectors.toList());
            map.put("featuredArtistNames", featuredNames);
            return map;
        }).collect(Collectors.toList());

        return Result.success(songList);
    }

    @GetMapping("/search")
    public Result search(@RequestParam(defaultValue = "") String keyword,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int pageSize) {
        List<Song> songs;
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        if (hasKeyword) {
            // 按歌曲名模糊搜索
            songs = new java.util.ArrayList<>(songService.findByTitle(keyword.trim()));
            // 按艺术家名模糊搜索，再查该艺术家的歌曲
            List<Artist> artists = artistMapper.findByNameSearch(keyword.trim());
            for (Artist artist : artists) {
                List<Song> artistSongs = songService.findByArtistId(artist.getId());
                for (Song s : artistSongs) {
                    boolean exists = songs.stream().anyMatch(existing -> existing.getId().equals(s.getId()));
                    if (!exists) {
                        songs.add(s);
                    }
                }
            }
        } else {
            songs = songService.findAll();
        }

        int total = songs.size();
        int fromIndex = Math.min((page - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Song> pageSongs = songs.subList(fromIndex, toIndex);

        // 为每首歌附加艺术家信息
        List<Map<String, Object>> songList = pageSongs.stream().map(song -> enrichSong(song)).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("records", songList);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("totalPages", (int) Math.ceil((double) total / pageSize));
        return Result.success(data);
    }

    /**
     * 为歌曲附加艺术家信息
     */
    private Map<String, Object> enrichSong(Song song) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", song.getId());
        map.put("title", song.getTitle());
        map.put("artistId", song.getArtistId());
        map.put("coverUrl", song.getCoverUrl());
        map.put("fileUrl", song.getFileUrl());
        map.put("duration", song.getDuration());
        map.put("createdAt", song.getCreatedAt());

        List<SongArtist> relations = songArtistMapper.findBySongId(song.getId());
        List<String> featuredNames = relations.stream()
                .filter(sa -> sa.getIsMain() != null && sa.getIsMain() == 0)
                .map(sa -> artistMapper.findById(sa.getArtistId()))
                .filter(a -> a != null)
                .map(Artist::getName)
                .collect(Collectors.toList());

        if (song.getArtistId() != null) {
            Artist mainArtist = artistMapper.findById(song.getArtistId());
            if (mainArtist != null) {
                map.put("artistName", mainArtist.getName());
            }
        }
        map.put("featuredArtistNames", featuredNames);
        return map;
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Song song = songService.findById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("song", song);
        // 查询副艺术家（is_main=0）
        List<SongArtist> relations = songArtistMapper.findBySongId(id);
        List<Integer> featuredArtistIds = relations.stream()
                .filter(sa -> sa.getIsMain() != null && sa.getIsMain() == 0)
                .map(SongArtist::getArtistId)
                .collect(Collectors.toList());
        result.put("featuredArtistIds", featuredArtistIds);
        return Result.success(result);
    }

    @SuppressWarnings("unchecked")
    @PostMapping
    public Result add(@RequestBody Map<String, Object> body) {
        Song song = new Song();
        song.setTitle((String) body.get("title"));
        song.setArtistId(body.get("artistId") != null ? ((Number) body.get("artistId")).intValue() : null);
        song.setDuration(body.get("duration") != null ? ((Number) body.get("duration")).intValue() : null);
        song.setCoverUrl((String) body.get("coverUrl"));
        song.setFileUrl((String) body.get("fileUrl"));

        log.info("Add song: {}", song);
        songService.add(song);

        // 保存歌曲-艺术家关系
        saveSongArtists(song.getId(), song.getArtistId(), body.get("featuredArtistIds"));

        return Result.success("Song added successfully");
    }

    @SuppressWarnings("unchecked")
    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Song song = new Song();
        song.setId(id);
        song.setTitle((String) body.get("title"));
        song.setArtistId(body.get("artistId") != null ? ((Number) body.get("artistId")).intValue() : null);
        song.setDuration(body.get("duration") != null ? ((Number) body.get("duration")).intValue() : null);
        if (body.get("coverUrl") != null) song.setCoverUrl((String) body.get("coverUrl"));
        if (body.get("fileUrl") != null) song.setFileUrl((String) body.get("fileUrl"));

        log.info("Update song: {}", song);
        songService.update(song);

        // 重新保存歌曲-艺术家关系
        saveSongArtists(id, song.getArtistId(), body.get("featuredArtistIds"));

        return Result.success("Song updated successfully");
    }

    /**
     * 保存歌曲与艺术家的关系（先删后插）
     */
    @SuppressWarnings("unchecked")
    private void saveSongArtists(Integer songId, Integer mainArtistId, Object featuredArtistIdsObj) {
        songArtistMapper.deleteBySongId(songId);

        // 插入主艺术家
        if (mainArtistId != null) {
            songArtistMapper.insert(new SongArtist(songId, mainArtistId, 1));
        }

        // 插入副艺术家
        if (featuredArtistIdsObj instanceof List) {
            List<Number> featuredIds = (List<Number>) featuredArtistIdsObj;
            for (Number artistId : featuredIds) {
                if (artistId != null) {
                    songArtistMapper.insert(new SongArtist(songId, artistId.intValue(), 0));
                }
            }
        }
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        log.info("Delete song id: {}", id);
        songService.delete(id);
        return Result.success("Song deleted successfully");
    }

    @PostMapping("/upload")
    public Result uploadFiles(
            @RequestParam("cover") MultipartFile coverFile,
            @RequestParam("music") MultipartFile musicFile,
            @RequestParam("title") String title,
            @RequestParam(value = "artistName", required = false, defaultValue = "") String artistName) {
        try {
            // 查找或创建艺术家
            Integer artistId = null;
            if (!artistName.isBlank()) {
                Artist artist = artistMapper.findByName(artistName.trim());
                if (artist != null) {
                    artistId = artist.getId();
                } else {
                    Artist newArtist = new Artist();
                    newArtist.setName(artistName.trim());
                    artistMapper.insert(newArtist);
                    artistId = newArtist.getId();
                    log.info("Auto-created artist: {} (id={})", artistName, artistId);
                }
            }

            String coverUrl = FileUploadUtil.saveCover(coverFile, artistName, title);
            String musicUrl = FileUploadUtil.saveMusic(musicFile, artistName, title);

            Map<String, Object> result = new HashMap<>();
            result.put("coverUrl", coverUrl);
            result.put("fileUrl", musicUrl);
            result.put("artistId", artistId);

            log.info("Uploaded files for song: {} by {}: cover={}, music={}", title, artistName, coverUrl, musicUrl);
            return Result.success(result);
        } catch (Exception e) {
            log.error("File upload failed", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}
