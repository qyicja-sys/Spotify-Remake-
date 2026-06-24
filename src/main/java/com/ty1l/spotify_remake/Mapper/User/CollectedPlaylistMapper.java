package com.ty1l.spotify_remake.Mapper.User;

import com.ty1l.spotify_remake.Entity.Admin.AdminPlaylist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CollectedPlaylistMapper {

    // 收藏歌单
    int collect(@Param("userId") Long userId, @Param("playlistId") Integer playlistId);

    // 取消收藏
    int uncollect(@Param("userId") Long userId, @Param("playlistId") Integer playlistId);

    // 检查是否已收藏
    int isCollected(@Param("userId") Long userId, @Param("playlistId") Integer playlistId);

    // 获取用户收藏的所有歌单
    List<AdminPlaylist> findCollectedPlaylistsByUserId(@Param("userId") Long userId);
}
