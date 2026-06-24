package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.Public.SearchSongVO;
import com.ty1l.spotify_remake.Entity.User.*;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.Mapper.User.PlaybackHistoryMapper;
import com.ty1l.spotify_remake.Mapper.User.UserArtistFollowMapper;
import com.ty1l.spotify_remake.Mapper.User.UserMapper;
import com.ty1l.spotify_remake.Mapper.User.mainWebMapper;
import com.ty1l.spotify_remake.Service.Public.ArtistService;
import com.ty1l.spotify_remake.utility.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    private UserArtistFollowMapper userArtistFollowMapper;

    @Override
    public ProfileVO getProfile(Long userId) {
        // 1. 查询用户基本信息
        User user = userMapper.findById(userId);

        // 2. 查询公开歌单数量（不含已点赞的歌曲）
        int playlistCount = mainWebMapper.countUserPublicPlaylists(userId);

        // 3. 查询关注艺人数
        int followingCount = playbackHistoryMapper.countFollowing(userId);

        // 4. 查询最近播放的歌手（最多5个）
        List<RecentArtistVO> recentArtists = playbackHistoryMapper.selectRecentArtists(userId, 5);

        // 5. 查询最近播放的歌曲（最多50首）
        List<RecentlyPlayedSongVO> recentSongs = playbackHistoryMapper.selectRecentSongs(userId, 50);

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
                fansCount = userArtistFollowMapper.countFollowersByArtistId(artist.getId());
                // 查询本月听众数
                monthlyListeners = playbackHistoryMapper.countMonthlyListeners(artist.getId());
            }
        }

        // 8. 组装返回
        return new ProfileVO(
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
     * 复制用户头像文件到 artists 目录，返回 artists 的 URL
     * static/datas/profilePic/xxx.jpg → static/datas/profilePic/artists/xxx.jpg
     */
    private String copyProfilePicToArtists(String profilePic) {
        if (profilePic == null || profilePic.isBlank()) return null;
        if (profilePic.contains("/profilePic/artists/")) return profilePic;

        try {
            // 从 URL 路径解析出实际文件路径
            String relativePath = profilePic.startsWith("/") ? profilePic.substring(1) : profilePic;
            Path source = Paths.get("src/main/resources", relativePath);
            if (!Files.exists(source)) {
                // 尝试从绝对路径解析（WebConfig 映射的 file: 前缀路径）
                source = Paths.get("D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources", relativePath);
            }
            if (!Files.exists(source)) {
                return profilePic; // 源文件不存在，返回原路径
            }

            // 目标路径
            String fileName = source.getFileName().toString();
            Path targetDir = Paths.get("D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/datas/profilePic/artists/");
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(fileName);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            return "/static/datas/profilePic/artists/" + fileName;
        } catch (Exception e) {
            // 复制失败则回退到原路径
            return profilePic;
        }
    }
}
