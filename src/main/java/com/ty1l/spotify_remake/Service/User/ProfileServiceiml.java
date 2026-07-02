package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.Public.SearchSongVO;
import com.ty1l.spotify_remake.Entity.User.*;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.Mapper.User.PlaybackHistoryMapper;
import com.ty1l.spotify_remake.Mapper.User.UserMapper;
import com.ty1l.spotify_remake.Mapper.User.mainWebMapper;
import com.ty1l.spotify_remake.Service.CacheService;
import com.ty1l.spotify_remake.Service.Public.ArtistService;
import com.ty1l.spotify_remake.Service.Public.FollowService;
import com.ty1l.spotify_remake.Service.Public.MonthlyListenersService;
import com.ty1l.spotify_remake.utility.FileUploadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfileServiceiml implements ProfileService {

    @Autowired
    private PlaybackHistoryMapper playbackHistoryMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private mainWebMapper mainWebMapper;

    @Autowired
    private ArtistService artistService;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private PlaybackHistoryService playbackHistoryService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private MonthlyListenersService monthlyListenersService;

    @Autowired
    private FollowService followService;

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceiml.class);

    @Override
    public ProfileVO getProfile(Long userId) {
        String cacheKey = String.format(CacheService.KEY_PROFILE, userId);

        // 尝试从缓存读取（播放数据和艺人歌曲实时刷新，其余元数据走缓存）
        ProfileVO cached = cacheService.get(cacheKey, ProfileVO.class);
        if (cached != null) {
            log.debug("Profile cache hit for userId={}", userId);
            // 播放数据实时从 Redis ZSET 获取，Redis 空则 MySQL 兜底
            List<Long> recentSongIds = playbackHistoryService.getRecentSongIds(userId, 50);
            List<RecentlyPlayedSongVO> recentSongs;
            if (recentSongIds.isEmpty()) {
                recentSongs = playbackHistoryMapper.selectRecentSongs(userId, 50);
                if (recentSongs == null) recentSongs = Collections.emptyList();
            } else {
                recentSongs = playbackHistoryService.buildRecentSongs(recentSongIds);
            }
            List<Long> recentArtistIds = playbackHistoryService.getRecentArtistIds(userId, 5);
            List<RecentArtistVO> recentArtists;
            if (recentArtistIds.isEmpty()) {
                recentArtists = playbackHistoryMapper.selectRecentArtists(userId, 5);
                if (recentArtists == null) recentArtists = Collections.emptyList();
            } else {
                recentArtists = playbackHistoryService.buildRecentArtists(recentArtistIds);
            }
            cached.setRecentSongs(recentSongs);
            cached.setRecentArtists(recentArtists);
            // Redis 冷启动：ZSET 为空时从 MySQL 回写，恢复缓存
            if (recentSongIds.isEmpty() || recentArtistIds.isEmpty()) {
                playbackHistoryService.syncMySQLToRedis(userId);
            }
            // 艺人歌曲实时刷新（艺人上传新歌后立即反映）
            if (cached.getIsArtist() != null && cached.getIsArtist() == 1 && cached.getArtistId() != null) {
                List<SearchSongVO> artistSongs = songMapper.findByArtistIdWithNames(cached.getArtistId());
                if (artistSongs == null) artistSongs = Collections.emptyList();
                if (artistSongs.size() > 10) {
                    artistSongs = artistSongs.subList(0, 10);
                }
                cached.setArtistSongs(artistSongs);
                // 月听众数实时刷新（Redis 实时 + DB 兜底）
                int liveListeners = monthlyListenersService.getMonthlyListeners(cached.getArtistId());
                cached.setMonthlyListeners(liveListeners);
            }
            return cached;
        }

        log.info("Profile cache miss for userId={}, rebuilding...", userId);

        // 1. 查询用户基本信息
        User user = userMapper.findById(userId);

        // 2. 查询公开歌单数量（不含已点赞的歌曲）
        int playlistCount = mainWebMapper.countUserPublicPlaylists(userId);

        // 3. 查询关注艺人数（Redis SET 实时计数）
        int followingCount = followService.getFollowingCount(userId);

        // 4. 最近播放的艺人（Redis ZSET，实时；空则 MySQL 兜底）
        List<Long> recentArtistIds = playbackHistoryService.getRecentArtistIds(userId, 5);
        List<RecentArtistVO> recentArtists;
        if (recentArtistIds.isEmpty()) {
            recentArtists = playbackHistoryMapper.selectRecentArtists(userId, 5);
            if (recentArtists == null) recentArtists = Collections.emptyList();
        } else {
            recentArtists = playbackHistoryService.buildRecentArtists(recentArtistIds);
        }

        // 5. 最近播放的歌曲（Redis ZSET，实时；空则 MySQL 兜底）
        List<Long> recentSongIds = playbackHistoryService.getRecentSongIds(userId, 50);
        List<RecentlyPlayedSongVO> recentSongs;
        if (recentSongIds.isEmpty()) {
            recentSongs = playbackHistoryMapper.selectRecentSongs(userId, 50);
            if (recentSongs == null) recentSongs = Collections.emptyList();
        } else {
            recentSongs = playbackHistoryService.buildRecentSongs(recentSongIds);
        }

        // Redis 冷启动：ZSET 为空时从 MySQL 回写，恢复缓存
        if (recentSongIds.isEmpty() || recentArtistIds.isEmpty()) {
            playbackHistoryService.syncMySQLToRedis(userId);
        }

        // 6. 查询公开歌单（不含已点赞的歌曲）
        List<PlaylistBriefVO> publicPlaylists = mainWebMapper.findPublicPlaylistsByUserId(userId);

        // 7. 如果是艺术家，查询艺术家自己的歌曲（最多10首）、粉丝数和月听众数
        Integer artistId = null;
        List<SearchSongVO> artistSongs = Collections.emptyList();
        int fansCount = 0;
        int monthlyListeners = 0;
        if (user.getIsArtist() != null && user.getIsArtist() == 1) {
            Artist artist = artistService.findByName(user.getNickName().trim());
            if (artist != null) {
                artistId = artist.getId();
                artistSongs = songMapper.findByArtistIdWithNames(artist.getId());
                if (artistSongs == null) artistSongs = Collections.emptyList();
                if (artistSongs.size() > 10) {
                    artistSongs = artistSongs.subList(0, 10);
                }
                // 查询粉丝数（关注人数）
                fansCount = followService.getFollowerCount(artist.getId());
                // 月听众数 = max(Redis实时值, DB兜底值)，保证刚播放就可见
                monthlyListeners = monthlyListenersService.getMonthlyListeners(artist.getId());
            }
        }

        // 8. 组装返回
        ProfileVO vo = new ProfileVO(
                user.getNickName(),
                user.getEmail(),
                user.getProfilePic(),
                user.getIsArtist(),
                playlistCount,
                followingCount,
                fansCount,
                monthlyListeners,
                recentArtists,
                recentSongs,
                publicPlaylists,
                artistId,
                artistSongs
        );

        // 写入缓存
        cacheService.set(cacheKey, vo);
        return vo;
    }

    @Override
    public void updateNickName(Long userId, String nickName) {
        userMapper.updateNickNameById(userId, nickName);
    }

    @Override
    public String updateProfilePic(Long userId, MultipartFile file) throws Exception {
        // 查询用户获取 userName 用于文件命名
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 保存头像文件，文件名为 [userName].ext
        String profilePicUrl = FileUploadUtil.saveUserProfilePic(file, user.getUserName());
        if (profilePicUrl == null) {
            throw new RuntimeException("头像保存失败");
        }

        // 更新数据库
        userMapper.updateProfilePicById(userId, profilePicUrl);
        return profilePicUrl;
    }

    @Override
    public void registerArtist(Long userId, String nickName) {
        // 1. 检查昵称是否已被 user 表中其他用户使用
        int count = userMapper.countByNickName(nickName);
        User currentUser = userMapper.findById(userId);
        // 如果有同名用户，且不是当前用户自己，则报错
        if (count > 0 && (currentUser == null || !nickName.equals(currentUser.getNickName()))) {
            throw new RuntimeException("已存在相关名字");
        }

        // 2. 检查 artists 表中是否已有同名艺术家
        Artist existingArtist = artistService.findByName(nickName);
        if (existingArtist != null) {
            throw new RuntimeException("已存在相关名字");
        }

        // 3. 更新用户的 nickName 和 is_artist 标记
        if (currentUser != null && !nickName.equals(currentUser.getNickName())) {
            userMapper.updateNickNameById(userId, nickName);
        }
        userMapper.updateIsArtistById(userId, 1);

        // 4. 同步到 artists 表，复制用户头像到 artists 目录
        String avatarUrl = null;
        if (currentUser != null && currentUser.getProfilePic() != null && !currentUser.getProfilePic().isBlank()) {
            avatarUrl = copyProfilePicToArtists(currentUser.getProfilePic());
        }
        Artist newArtist = new Artist();
        newArtist.setName(nickName);
        newArtist.setAvatarUrl(avatarUrl);
        artistService.add(newArtist);
    }

    /**
     * 复制用户头像到艺术家目录（OSS 上复制，旧本地路径保持兼容）
     */
    private String copyProfilePicToArtists(String profilePic) {
        return FileUploadUtil.copyAvatarToArtistDir(profilePic);
    }
}
