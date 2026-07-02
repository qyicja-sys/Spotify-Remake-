package com.ty1l.spotify_remake.Controller.User;


import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.Public.SearchSongVO;
import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Entity.Public.SongArtist;
import com.ty1l.spotify_remake.Entity.Admin.AdminPlaylist;
import com.ty1l.spotify_remake.Entity.User.HomeDashboardVO;
import com.ty1l.spotify_remake.Entity.User.LoginInfoVo;
import com.ty1l.spotify_remake.Entity.User.PlaylistBriefVO;
import com.ty1l.spotify_remake.Entity.User.ProfileVO;
import com.ty1l.spotify_remake.Entity.User.User;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.Mapper.User.CollectedPlaylistMapper;
import com.ty1l.spotify_remake.Mapper.User.LoginMapper;
import com.ty1l.spotify_remake.Mapper.User.PlaybackHistoryMapper;
import com.ty1l.spotify_remake.Mapper.User.UserMapper;
import com.ty1l.spotify_remake.Service.CacheService;
import com.ty1l.spotify_remake.Service.Public.ArtistService;
import com.ty1l.spotify_remake.Service.Public.FollowService;
import com.ty1l.spotify_remake.Service.Public.SongService;
import com.ty1l.spotify_remake.Service.User.LoginService;
import com.ty1l.spotify_remake.Service.User.MainWebService;
import com.ty1l.spotify_remake.Service.User.PlaybackHistoryService;
import com.ty1l.spotify_remake.Service.User.ProfileService;
import com.ty1l.spotify_remake.utility.BaseContext;
import com.ty1l.spotify_remake.utility.FileUploadUtil;
import com.ty1l.spotify_remake.utility.JwtGenerate;
import com.ty1l.spotify_remake.utility.Result;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/spotify")
public class mainWebController {

    @Autowired
    private LoginMapper loginMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LoginService loginService;

    @Autowired
    private MainWebService mainWebService;

    @Autowired
    private ArtistService artistService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private SongService songService;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private SongArtistMapper songArtistMapper;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private CollectedPlaylistMapper collectedPlaylistMapper;

    @Autowired
    private PlaybackHistoryMapper playbackHistoryMapper;

    @Autowired
    private FollowService followService;

    @Autowired
    private PlaybackHistoryService playbackHistoryService;

    @GetMapping("/home")
    public ResponseEntity<Result> home(@RequestHeader(value = "token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        try {
            Claims claims = JwtGenerate.parseJwt(token);
            Integer userId = Integer.valueOf(claims.get("userId") != null ? claims.get("userId").toString() : claims.get("id").toString());

            // 查询用户基本信息（不走缓存，轻量查询）
            User user = userMapper.findById(userId.longValue());

            // 从缓存/数据库获取主页数据
            HomeDashboardVO vo = mainWebService.getHomeDashboard(userId.longValue());

            // 填充用户信息（含当前 token，不从缓存取）
            if (user != null) {
                vo.setUserInfo(new LoginInfoVo(user.getEmail(), user.getNickName(), user.getProfilePic(), token, null));
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .body(Result.success(vo));
        } catch (Exception e) {
            log.error("Token 解析失败", e);
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
    }

    @PostMapping("/playlist/create")
    public ResponseEntity<Result> createPlaylist() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        try {
            PlaylistBriefVO playlist = mainWebService.createPlaylist(userId);
            cacheService.evictBoth(String.format(CacheService.KEY_HOME, userId));
            return ResponseEntity.ok(Result.success(playlist));
        } catch (Exception e) {
            log.error("创建歌单失败", e);
            return ResponseEntity.status(500).body(Result.error("创建歌单失败"));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<Result> getProfile() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        try {
            ProfileVO profile = profileService.getProfile(userId);
            return ResponseEntity.ok(Result.success(profile));
        } catch (Exception e) {
            log.error("获取用户资料失败", e);
            return ResponseEntity.status(500).body(Result.error("获取用户资料失败"));
        }
    }

    @PutMapping("/profile/nickname")
    public ResponseEntity<Result> updateNickName(@RequestBody Map<String, String> body) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        String nickName = body.get("nickName");
        if (nickName == null || nickName.isBlank()) {
            return ResponseEntity.badRequest().body(Result.errorClient("昵称不能为空"));
        }
        try {
            profileService.updateNickName(userId, nickName.trim());
            cacheService.evictBoth(String.format(CacheService.KEY_PROFILE, userId));
            return ResponseEntity.ok(Result.success());
        } catch (Exception e) {
            log.error("更新昵称失败", e);
            return ResponseEntity.status(500).body(Result.error("更新昵称失败"));
        }
    }

    @PostMapping("/profile/avatar")
    public ResponseEntity<Result> updateAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.errorClient("请选择头像文件"));
        }
        try {
            String profilePicUrl = profileService.updateProfilePic(userId, file);
            cacheService.evictBoth(String.format(CacheService.KEY_PROFILE, userId));
            return ResponseEntity.ok(Result.success(Map.of("profilePic", profilePicUrl)));
        } catch (Exception e) {
            log.error("更新头像失败", e);
            return ResponseEntity.status(500).body(Result.error("更新头像失败"));
        }
    }

    @PostMapping("/profile/register-artist")
    public ResponseEntity<Result> registerArtist(@RequestBody Map<String, String> body) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        String nickName = body.get("nickName");
        if (nickName == null || nickName.isBlank()) {
            return ResponseEntity.badRequest().body(Result.errorClient("艺术家昵称不能为空"));
        }
        try {
            profileService.registerArtist(userId, nickName.trim());
            cacheService.evictBoth(String.format(CacheService.KEY_PROFILE, userId));
            return ResponseEntity.ok(Result.success());
        } catch (RuntimeException e) {
            log.warn("注册艺术家失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Result.error(e.getMessage()));
        } catch (Exception e) {
            log.error("注册艺术家失败", e);
            return ResponseEntity.status(500).body(Result.error("注册艺术家失败"));
        }
    }

    @GetMapping("/artists/search")
    public ResponseEntity<Result> searchArtists(@RequestParam("keyword") String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.ok(Result.success(Collections.emptyList()));
        }
        try {
            List<Artist> artists = artistMapper.findByNameSearch(keyword.trim());
            return ResponseEntity.ok(Result.success(artists));
        } catch (Exception e) {
            log.error("搜索艺术家失败", e);
            return ResponseEntity.status(500).body(Result.error("搜索艺术家失败"));
        }
    }

    @GetMapping("/artist/{id}")
    public ResponseEntity<Result> getArtistDetail(@PathVariable Integer id) {
        try {
            Map<String, Object> detail = artistService.getArtistDetail(id);
            if (detail == null) {
                return ResponseEntity.badRequest().body(Result.error("艺术家不存在"));
            }
            return ResponseEntity.ok(Result.success(detail));
        } catch (Exception e) {
            log.error("获取艺术家详情失败", e);
            return ResponseEntity.status(500).body(Result.error("获取艺术家详情失败"));
        }
    }

    @PostMapping("/artist/{id}/follow")
    public ResponseEntity<Result> followArtist(@PathVariable Integer id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        try {
            Artist artist = artistMapper.findById(id);
            if (artist == null) {
                return ResponseEntity.badRequest().body(Result.error("艺术家不存在"));
            }
            // 不能关注自己
            User user = userMapper.findById(userId);
            if (user != null && user.getIsArtist() != null && user.getIsArtist() == 1
                    && artist.getName().equals(user.getNickName())) {
                return ResponseEntity.badRequest().body(Result.error("不能关注自己"));
            }
            followService.follow(userId, id);
            return ResponseEntity.ok(Result.success());
        } catch (Exception e) {
            log.error("关注艺术家失败", e);
            return ResponseEntity.status(500).body(Result.error("关注艺术家失败"));
        }
    }

    @DeleteMapping("/artist/{id}/follow")
    public ResponseEntity<Result> unfollowArtist(@PathVariable Integer id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        try {
            followService.unfollow(userId, id);
            return ResponseEntity.ok(Result.success());
        } catch (Exception e) {
            log.error("取消关注艺术家失败", e);
            return ResponseEntity.status(500).body(Result.error("取消关注失败"));
        }
    }

    @GetMapping("/artist/{id}/follow/check")
    public ResponseEntity<Result> checkArtistFollowed(@PathVariable Integer id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        try {
            boolean followed = followService.isFollowing(userId, id);
            return ResponseEntity.ok(Result.success(Map.of("followed", followed)));
        } catch (Exception e) {
            log.error("检查关注状态失败", e);
            return ResponseEntity.status(500).body(Result.error("检查关注状态失败"));
        }
    }

    @PostMapping("/profile/upload-song")
    public ResponseEntity<Result> uploadSong(
            @RequestParam("title") String title,
            @RequestParam("cover") MultipartFile coverFile,
            @RequestParam("music") MultipartFile musicFile,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestParam(value = "featuredArtistIds", required = false) List<Integer> featuredArtistIds) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Result.errorClient("歌曲名不能为空"));
        }

        // 查找当前用户对应的艺术家
        User user = userMapper.findById(userId);
        if (user == null || user.getIsArtist() == null || user.getIsArtist() != 1) {
            return ResponseEntity.badRequest().body(Result.error("只有注册艺术家才能上传歌曲"));
        }
        Artist artist = artistMapper.findByName(user.getNickName().trim());
        if (artist == null) {
            return ResponseEntity.badRequest().body(Result.error("未找到对应的艺术家信息"));
        }

        try {
            // 构建完整艺术家名（含联名），用于文件命名
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

            // 复用 FileUploadUtil 上传文件，文件名：艺术家1, 艺术家2 - 歌曲名
            String coverUrl = FileUploadUtil.saveCover(coverFile, fullArtistName, title.trim());
            String fileUrl = FileUploadUtil.saveMusic(musicFile, fullArtistName, title.trim());

            // 创建歌曲记录
            Song song = new Song();
            song.setTitle(title.trim());
            song.setArtistId(artist.getId());
            song.setCoverUrl(coverUrl);
            song.setFileUrl(fileUrl);
            song.setDuration(duration);
            songService.add(song);

            // 保存歌曲-艺术家关系（主艺术家）
            songArtistMapper.insert(new SongArtist(song.getId(), artist.getId(), 1));

            // 保存联名艺术家（feat.）
            if (featuredArtistIds != null) {
                for (Integer featId : featuredArtistIds) {
                    if (featId != null && !featId.equals(artist.getId())) {
                        songArtistMapper.insert(new SongArtist(song.getId(), featId, 0));
                    }
                }
            }

            log.info("艺术家 {} 上传了歌曲: {}", artist.getName(), title.trim());
            cacheService.evictBoth(String.format(CacheService.KEY_ARTIST, artist.getId()));
            cacheService.evictBoth(String.format(CacheService.KEY_PROFILE, userId));
            cacheService.incrVersion(CacheService.KEY_VERSION_HOME);
            return ResponseEntity.ok(Result.success(Map.of(
                    "songId", song.getId(),
                    "coverUrl", coverUrl,
                    "fileUrl", fileUrl
            )));
        } catch (Exception e) {
            log.error("上传歌曲失败", e);
            return ResponseEntity.status(500).body(Result.error("上传歌曲失败: " + e.getMessage()));
        }
    }

    /**
     * 记录播放历史（前端异步调用）
     *
     * 前端 Pinia store 乐观更新后，fire-and-forget 此接口。
     * 后端同步写 Redis ZSET（~1ms），异步落库 MySQL。
     *
     * @param body { "songId": 123 }
     */
    @PostMapping("/playback/record")
    public ResponseEntity<Result> recordPlayback(@RequestBody Map<String, Object> body) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.errorClient("NOT_LOGIN"));
        }
        try {
            Integer songId = body.get("songId") != null
                    ? ((Number) body.get("songId")).intValue() : null;
            if (songId == null) {
                return ResponseEntity.badRequest().body(Result.errorClient("songId 不能为空"));
            }
            playbackHistoryService.recordPlayback(userId, songId);
            return ResponseEntity.ok(Result.success());
        } catch (Exception e) {
            log.warn("记录播放历史失败: {}", e.getMessage());
            return ResponseEntity.ok(Result.success()); // 静默失败，不影响前端播放
        }
    }
}
