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

import java.util.ArrayList;
import java.util.List;
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
        vo.setTotalDuration(songs.stream().mapToInt(PlaylistSongVO::getDuration).sum());
        vo.setSongs(songs);
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
}
