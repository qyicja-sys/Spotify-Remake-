package com.ty1l.spotify_remake.Mapper.Public;

import com.ty1l.spotify_remake.Entity.Admin.AdminPlaylist;
import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Entity.User.PlaylistSongVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlaylistMapper {

    List<AdminPlaylist> findAll();

    List<AdminPlaylist> findByType(@Param("type") Integer type);

    List<AdminPlaylist> findAllSystem();

    AdminPlaylist findById(Integer id);

    List<Song> findSongsByPlaylistId(Integer playlistId);

    List<PlaylistSongVO> findPlaylistSongsByPlaylistId(Integer playlistId);

    int insert(AdminPlaylist playlist);

    int update(AdminPlaylist playlist);

    int deleteById(Integer id);

    int deleteSongsByPlaylistId(Integer playlistId);

    int addSong(@Param("playlistId") Integer playlistId, @Param("songId") Integer songId);

    int removeSong(@Param("playlistId") Integer playlistId, @Param("songId") Integer songId);

    AdminPlaylist findLikedPlaylistByUserId(@Param("userId") Long userId);

    int isSongInPlaylist(@Param("playlistId") Integer playlistId, @Param("songId") Integer songId);
}
