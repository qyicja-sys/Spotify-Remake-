package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.Admin.AdminPlaylist;
import com.ty1l.spotify_remake.Entity.Public.ExternalTrackVO;
import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Entity.Public.SearchSongVO;
import java.util.List;
import java.util.Locale;
import com.ty1l.spotify_remake.Mapper.Public.PlaylistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.Mapper.User.CollectedPlaylistMapper;
import com.ty1l.spotify_remake.Service.User.UserPlaylistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserPlaylistServiceiml implements UserPlaylistService {

    @Autowired
    private PlaylistMapper playlistMapper;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private CollectedPlaylistMapper collectedPlaylistMapper;

    @Override
    public void likeSong(Long userId, Integer songId) {
        AdminPlaylist likedPlaylist = getLikedPlaylist(userId);
        playlistMapper.addSong(likedPlaylist.getId(), songId);
    }

    @Override
    public Integer likeExternalSong(Long userId, ExternalTrackVO track) {
        // 检查外部歌曲是否已导入
        Song existing = songMapper.findByExternalId(track.getSource(), track.getExternalId());
        Integer songId;
        if (existing != null) {
            songId = existing.getId();
        } else {
            // 模糊匹配：检查本地是否已有同名歌曲（优先保留本地）
            String normalizedExtTitle = normalizeTitle(track.getTitle());
            List<SearchSongVO> localMatches = songMapper.searchWithTitle(normalizedExtTitle);
            Song matchedLocal = null;
            for (SearchSongVO local : localMatches) {
                if (normalizeTitle(local.getTitle()).equals(normalizedExtTitle)) {
                    matchedLocal = songMapper.findById(local.getId());
                    break;
                }
            }
            if (matchedLocal != null) {
                // 本地已有相似歌曲，直接用本地歌曲
                songId = matchedLocal.getId();
            } else {
                // 导入外部歌曲到 songs 表
                Song song = new Song();
                song.setTitle(track.getTitle());
                song.setCoverUrl(track.getCoverUrl());
                song.setDuration(track.getDuration() != null ? track.getDuration() : 0);
                song.setFileUrl(""); // 外部歌曲无本地文件
                song.setExternalSource(track.getSource());
                song.setExternalId(track.getExternalId());
                songMapper.insert(song);
                songId = song.getId();
            }
        }
        // 添加到已点赞歌单
        AdminPlaylist likedPlaylist = getLikedPlaylist(userId);
        playlistMapper.addSong(likedPlaylist.getId(), songId);
        return songId;
    }

    /** 标题标准化：去括号注释、feat合作、版本标记等 */
    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase(Locale.ROOT)
            .replaceAll("\\(\\s*explicit\\s*\\)|\\[\\s*explicit\\s*\\]|\\(\\s*clean\\s*\\)|\\[\\s*clean\\s*\\]", "")
            .replaceAll("\\(\\s*remaster(?:ed)?\\s*(?:\\d{4})?\\s*\\)", "")
            .replaceAll("\\(\\s*feat\\.?.*?\\)|\\(\\s*ft\\.?.*?\\)|feat\\..*?(?=\\)|$)", "")
            .replaceAll("\\s*-\\s*explicit|\\s*-\\s*clean", "")
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    @Override
    public void unlikeSong(Long userId, Integer songId) {
        AdminPlaylist likedPlaylist = getLikedPlaylist(userId);
        playlistMapper.removeSong(likedPlaylist.getId(), songId);
    }

    @Override
    public boolean isLiked(Long userId, Integer songId) {
        AdminPlaylist likedPlaylist = getLikedPlaylist(userId);
        if (likedPlaylist == null) {
            return false;
        }
        return playlistMapper.isSongInPlaylist(likedPlaylist.getId(), songId) > 0;
    }

    @Override
    public void addSongToPlaylist(Long userId, Integer playlistId, Integer songId) {
        AdminPlaylist playlist = playlistMapper.findById(playlistId);
        if (playlist == null) {
            throw new RuntimeException("歌单不存在");
        }
        if (!String.valueOf(userId).equals(String.valueOf(playlist.getUserId()))) {
            throw new RuntimeException("无权操作此歌单");
        }
        playlistMapper.addSong(playlistId, songId);
    }

    @Override
    public void removeSongFromPlaylist(Long userId, Integer playlistId, Integer songId) {
        AdminPlaylist playlist = playlistMapper.findById(playlistId);
        if (playlist == null) {
            throw new RuntimeException("歌单不存在");
        }
        if (!String.valueOf(userId).equals(String.valueOf(playlist.getUserId()))) {
            throw new RuntimeException("无权操作此歌单");
        }
        playlistMapper.removeSong(playlistId, songId);
    }

    @Override
    public void deletePlaylist(Long userId, Integer playlistId) {
        AdminPlaylist playlist = playlistMapper.findById(playlistId);
        if (playlist == null) {
            throw new RuntimeException("歌单不存在");
        }
        if (!String.valueOf(userId).equals(playlist.getUserId())) {
            throw new RuntimeException("无权删除此歌单");
        }
        // 先删歌曲关联，再删歌单
        playlistMapper.deleteSongsByPlaylistId(playlistId);
        playlistMapper.deleteById(playlistId);
    }

    @Override
    public void editPlaylist(Long userId, Integer playlistId, String title, String profile, String coverUrl) {
        AdminPlaylist playlist = playlistMapper.findById(playlistId);
        if (playlist == null) {
            throw new RuntimeException("歌单不存在");
        }
        if (!String.valueOf(userId).equals(String.valueOf(playlist.getUserId()))) {
            throw new RuntimeException("无权编辑此歌单");
        }
        if (title == null || title.isBlank()) {
            throw new RuntimeException("歌单名不能为空");
        }
        AdminPlaylist update = new AdminPlaylist();
        update.setId(playlistId);
        update.setTitle(title.trim());
        update.setProfile(profile != null ? profile.trim() : null);
        if (coverUrl != null) {
            update.setCoverUrl(coverUrl);
        }
        playlistMapper.update(update);
    }

    private AdminPlaylist getLikedPlaylist(Long userId) {
        AdminPlaylist playlist = playlistMapper.findLikedPlaylistByUserId(userId);
        if (playlist == null) {
            throw new RuntimeException("未找到'已点赞的歌曲'歌单");
        }
        return playlist;
    }

    @Override
    public void collectPlaylist(Long userId, Integer playlistId) {
        AdminPlaylist playlist = playlistMapper.findById(playlistId);
        if (playlist == null) {
            throw new RuntimeException("歌单不存在");
        }
        // 不能收藏自己的歌单
        if (String.valueOf(userId).equals(String.valueOf(playlist.getUserId()))) {
            throw new RuntimeException("不能收藏自己的歌单");
        }
        collectedPlaylistMapper.collect(userId, playlistId);
    }

    @Override
    public void uncollectPlaylist(Long userId, Integer playlistId) {
        collectedPlaylistMapper.uncollect(userId, playlistId);
    }

    @Override
    public boolean isPlaylistCollected(Long userId, Integer playlistId) {
        return collectedPlaylistMapper.isCollected(userId, playlistId) > 0;
    }

    @Override
    public void togglePlaylistPrivacy(Long userId, Integer playlistId) {
        AdminPlaylist playlist = playlistMapper.findById(playlistId);
        if (playlist == null) {
            throw new RuntimeException("歌单不存在");
        }
        if (!String.valueOf(userId).equals(String.valueOf(playlist.getUserId()))) {
            throw new RuntimeException("无权操作此歌单");
        }
        AdminPlaylist update = new AdminPlaylist();
        update.setId(playlistId);
        // 当前是公开(0)→设为私密(1)，当前是私密(1)→设为公开(0)
        update.setIsPrivate(playlist.getIsPrivate() != null && playlist.getIsPrivate() == 1 ? 0 : 1);
        playlistMapper.update(update);
    }
}

