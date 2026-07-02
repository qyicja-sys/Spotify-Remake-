package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Admin.AdminPlaylist;
import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.Public.SongArtist;
import com.ty1l.spotify_remake.Entity.User.PlaylistDetailVO;
import com.ty1l.spotify_remake.Entity.User.PlaylistSongVO;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.PlaylistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongArtistMapper;
import com.ty1l.spotify_remake.utility.BaseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlaylistServiceiml implements PlaylistService {

    @Autowired
    private PlaylistMapper playlistMapper;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private SongArtistMapper songArtistMapper;

    @Override
    public List<AdminPlaylist> findAll() {
        return playlistMapper.findAll();
    }

    @Override
    public List<AdminPlaylist> findByType(Integer type) {
        return playlistMapper.findByType(type);
    }

    @Override
    public List<AdminPlaylist> findAllSystem() {
        return playlistMapper.findAllSystem();
    }

    @Override
    public AdminPlaylist findById(Integer id) {
        return playlistMapper.findById(id);
    }

    @Override
    public PlaylistDetailVO findDetailById(Integer id) {
        AdminPlaylist playlist = playlistMapper.findById(id);
        if (playlist == null) {
            return null;
        }
        List<PlaylistSongVO> songs = playlistMapper.findPlaylistSongsByPlaylistId(id);
        if (songs == null) {
            songs = new ArrayList<>();
        }
        for (PlaylistSongVO song : songs) {
            List<SongArtist> songArtists = songArtistMapper.findBySongId(song.getId());
            if (songArtists != null && !songArtists.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (SongArtist sa : songArtists) {
                    Artist artist = artistMapper.findById(sa.getArtistId());
                    if (artist != null) {
                        names.add(artist.getName());
                    }
                }
                song.setArtistName(String.join(",", names));
            } else if (song.getArtistId() != null) {
                Artist artist = artistMapper.findById(song.getArtistId());
                if (artist != null) {
                    song.setArtistName(artist.getName());
                }
            }
        }
        PlaylistDetailVO vo = new PlaylistDetailVO();
        vo.setId(playlist.getId());
        try {
            vo.setUserId(Integer.valueOf(playlist.getUserId()));
        } catch (NumberFormatException e) {
            vo.setUserId(null);
        }
        vo.setTitle(playlist.getTitle());
        vo.setCoverUrl(playlist.getCoverUrl());
        vo.setBackgroundUrl(playlist.getBackgroundUrl());
        vo.setProfile(playlist.getProfile());
        vo.setType(playlist.getType());
        vo.setIsPrivate(playlist.getIsPrivate());
        vo.setSongCount(songs.size());
        vo.setTotalDuration(songs.stream().mapToInt(s -> s.getDuration() != null ? s.getDuration() : 0).sum());
        // 去重：标题标准化后相同的歌曲只保留第一条
        List<PlaylistSongVO> deduped = new ArrayList<>();
        Set<String> seenTitles = new HashSet<>();
        for (PlaylistSongVO s : songs) {
            String key = normalizeTitle(s.getTitle());
            if (!seenTitles.contains(key)) {
                seenTitles.add(key);
                deduped.add(s);
            }
        }
        vo.setSongs(deduped);
        return vo;
    }

    @Override
    public void add(AdminPlaylist playlist) {
        if (playlist.getType() == null) {
            playlist.setType(1);
        }
        // 系统歌单(1=系统推荐, 2=官方精选)使用system用户id=1
        Integer type = playlist.getType();
        if (type == 1 || type == 2) {
            playlist.setUserId("1");
        } else {
            playlist.setUserId(String.valueOf(BaseContext.getCurrentId()));
        }
        playlistMapper.insert(playlist);
    }

    @Override
    public void update(AdminPlaylist playlist) {
        playlistMapper.update(playlist);
    }

    @Override
    public void delete(Integer id) {
        playlistMapper.deleteSongsByPlaylistId(id);
        playlistMapper.deleteById(id);
    }

    @Override
    public void addSong(Integer playlistId, Integer songId) {
        playlistMapper.addSong(playlistId, songId);
    }

    @Override
    public void removeSong(Integer playlistId, Integer songId) {
        playlistMapper.removeSong(playlistId, songId);
    }
    /** 标题标准化：去括号注释、feat合作、版本标记等，用于本地/外部歌曲去重匹配 */
    private String normalizeTitle(String title) {
        if (title == null) return "";
        String s = title.toLowerCase(Locale.ROOT);
        // 1. 移除 (...) 中的 feat/ft/featuring 标记（含可选句点）
        s = s.replaceAll("\\(\\s*(?:feat|ft|featuring)\\.?[^)]*\\)", "");
        // 2. 移除 [...] 中的 feat/ft/featuring 标记
        s = s.replaceAll("\\[\\s*(?:feat|ft|featuring)\\.?[^]]*\\]", "");
        // 3. 移除 (...) 中的版本标记
        s = s.replaceAll(
            "\\(\\s*(?:live|acoustic|remix|radio\\s*edit|extended|bonus\\s*track|rework|version|edit|cover|instrumental|original\\s*mix|remaster(?:ed)?\\s*(?:\\d{4})?|explicit|clean|deluxe|single\\s*edit|album\\s*version)\\s*\\)",
            "");
        // 4. 移除 [...] 中的版本标记
        s = s.replaceAll(
            "\\[\\s*(?:live|acoustic|remix|radio\\s*edit|extended|bonus\\s*track|rework|version|edit|cover|instrumental|original\\s*mix|remaster(?:ed)?\\s*(?:\\d{4})?|explicit|clean|deluxe|single\\s*edit|album\\s*version)\\s*\\]",
            "");
        // 5. 移除破折号分隔的 feat 标记: " - feat. X", " – feat. X"
        s = s.replaceAll("\\s*[-–]\\s*(?:feat|ft|featuring)\\.?\\s+.*$", "");
        // 6. 移除空格分隔的 feat 标记: " feat. X", " ft. X"
        s = s.replaceAll("\\s+(?:feat|ft|featuring)\\.?\\s+.*$", "");
        // 7. 移除破折号分隔的 explicit/clean 标记
        s = s.replaceAll("\\s*[-–]\\s*(?:explicit|clean)\\s*$", "");
        // 8. 去除残留的非字母数字（保留空格）
        s = s.replaceAll("[^a-z0-9\\s]", "");
        // 9. 合并连续空格并修剪
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }
}
