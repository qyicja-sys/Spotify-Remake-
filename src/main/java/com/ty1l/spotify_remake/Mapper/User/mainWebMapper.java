package com.ty1l.spotify_remake.Mapper.User;

import com.ty1l.spotify_remake.Entity.User.Playlist;
import com.ty1l.spotify_remake.Entity.User.PlaylistBriefVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface mainWebMapper {

    // 统计用户创建的歌单数量
    int countUserPlaylists(@Param("userId") Long userId);

    // 插入新歌单，自动生成主键回填到 playlist.id
    int insertPlaylist(Playlist playlist);

    // 统计用户公开歌单数量（不含已点赞的歌曲）
    int countUserPublicPlaylists(@Param("userId") Long userId);

    // 查询用户的公开歌单（不含已点赞的歌曲），带创建者昵称
    List<PlaylistBriefVO> findPublicPlaylistsByUserId(@Param("userId") Long userId);
}
