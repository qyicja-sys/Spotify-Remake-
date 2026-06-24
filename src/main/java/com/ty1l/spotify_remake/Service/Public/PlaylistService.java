package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Admin.AdminPlaylist;
import com.ty1l.spotify_remake.Entity.User.PlaylistDetailVO;

import java.util.List;

public interface PlaylistService {
    List<AdminPlaylist> findAll();

    List<AdminPlaylist> findByType(Integer type);

    List<AdminPlaylist> findAllSystem();

    AdminPlaylist findById(Integer id);

    PlaylistDetailVO findDetailById(Integer id);

    void add(AdminPlaylist playlist);

    void update(AdminPlaylist playlist);

    void delete(Integer id);

    void addSong(Integer playlistId, Integer songId);

    void removeSong(Integer playlistId, Integer songId);
}
