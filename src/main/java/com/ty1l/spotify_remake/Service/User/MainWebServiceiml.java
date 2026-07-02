package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.Admin.AdminPlaylist;
import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.User.HomeDashboardVO;
import com.ty1l.spotify_remake.Entity.User.LoginInfoVo;
import com.ty1l.spotify_remake.Entity.User.Playlist;
import com.ty1l.spotify_remake.Entity.User.PlaylistBriefVO;
import com.ty1l.spotify_remake.Entity.User.User;
import com.ty1l.spotify_remake.Mapper.User.CollectedPlaylistMapper;
import com.ty1l.spotify_remake.Mapper.User.LoginMapper;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Mapper.User.UserMapper;
import com.ty1l.spotify_remake.Mapper.User.mainWebMapper;
import com.ty1l.spotify_remake.Service.CacheService;
import com.ty1l.spotify_remake.Service.Public.FollowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MainWebServiceiml implements MainWebService {

    private static final Logger log = LoggerFactory.getLogger(MainWebServiceiml.class);

    @Autowired
    private mainWebMapper mainWebMapper;

    @Autowired
    private LoginMapper loginMapper;

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CollectedPlaylistMapper collectedPlaylistMapper;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private FollowService followService;

    @Autowired
    private ArtistMapper artistMapper;

    @Override
    public PlaylistBriefVO createPlaylist(Long userId) {
        // 1. 统计该用户已有歌单数量，生成默认名称
        int count = mainWebMapper.countUserPlaylists(userId);
        String name = "新建歌单 #" + (count + 1);

        // 2. 插入数据库
        Playlist playlist = new Playlist();
        playlist.setUserId(String.valueOf(userId));
        playlist.setName(name);
        playlist.setType("0");
        mainWebMapper.insertPlaylist(playlist);

        // 3. 封装返回值
        PlaylistBriefVO vo = new PlaylistBriefVO();
        vo.setId(playlist.getId());
        vo.setUserId(Math.toIntExact(userId));
        vo.setName(name);
        vo.setCoverUrl(null);
        vo.setType(0);
        return vo;
    }

    @Override
    public HomeDashboardVO getHomeDashboard(Long userId) {
        String cacheKey = String.format(CacheService.KEY_HOME, userId);
        long homeVersion = cacheService.getVersion(CacheService.KEY_VERSION_HOME);

        // 尝试从缓存读取（带版本号校验）
        HomeDashboardVO cached = cacheService.getWithVersion(cacheKey, HomeDashboardVO.class, homeVersion);
        if (cached != null) {
            log.debug("Home cache hit for userId={}", userId);
            return cached;
        }

        log.info("Home cache miss for userId={}, rebuilding...", userId);

        // 缓存未命中，从数据库组装
        Integer uid = userId.intValue();

        List<PlaylistBriefVO> myPlaylists = loginService.completePlaylistCoverUrl(
                loginMapper.PackageMyPlaylists(uid));
        List<PlaylistBriefVO> systemPlaylists = loginService.completePlaylistCoverUrl(
                loginMapper.PackageSystemPlaylists());

        // 查询用户收藏的歌单
        List<AdminPlaylist> collectedRaw = collectedPlaylistMapper.findCollectedPlaylistsByUserId(userId);
        List<PlaylistBriefVO> collectedPlaylists = collectedRaw.stream().map(p -> {
            PlaylistBriefVO vo = new PlaylistBriefVO();
            vo.setId(p.getId());
            try { vo.setUserId(Integer.valueOf(p.getUserId())); } catch (Exception ignored) {}
            vo.setName(p.getTitle());
            vo.setProfile(p.getProfile());
            vo.setCoverUrl(p.getCoverUrl());
            vo.setType(p.getType());
            vo.setIsPrivate(p.getIsPrivate());
            return vo;
        }).collect(Collectors.toList());
        collectedPlaylists = loginService.completePlaylistCoverUrl(collectedPlaylists);

        // 查询用户关注的艺人（Redis SET 取 ID 列表 + DB 补全详情）
        List<Artist> followedArtists = followService.getFollowedArtistIds(userId).stream()
                .map(Integer::parseInt)
                .map(artistMapper::findById)
                .filter(a -> a != null)
                .collect(Collectors.toList());

        // 组装 VO（LoginInfoVo 不含 token，由 Controller 填充）
        HomeDashboardVO vo = new HomeDashboardVO(null, myPlaylists, systemPlaylists, collectedPlaylists, followedArtists);

        // 写入缓存（带版本号）
        cacheService.setWithVersion(cacheKey, vo, homeVersion, 1800);
        return vo;
    }
}
