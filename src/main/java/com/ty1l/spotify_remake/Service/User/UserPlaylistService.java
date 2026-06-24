package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.Public.ExternalTrackVO;

public interface UserPlaylistService {

    /**
     * 收藏本地歌曲到"已点赞的歌曲"歌单
     */
    void likeSong(Long userId, Integer songId);

    /**
     * 收藏外部歌曲：先导入 songs 表，再添加到"已点赞的歌曲"歌单
     * @return 导入后的本地 songId
     */
    Integer likeExternalSong(Long userId, ExternalTrackVO track);

    /**
     * 从"已点赞的歌曲"歌单移除歌曲
     */
    void unlikeSong(Long userId, Integer songId);

    /**
     * 检查歌曲是否已收藏
     */
    boolean isLiked(Long userId, Integer songId);

    /**
     * 删除用户自建歌单（先删歌曲关联，再删歌单）
     */
    void deletePlaylist(Long userId, Integer playlistId);

    /**
     * 将歌曲添加到用户指定歌单
     */
    void addSongToPlaylist(Long userId, Integer playlistId, Integer songId);

    /**
     * 从用户指定歌单中移除歌曲
     */
    void removeSongFromPlaylist(Long userId, Integer playlistId, Integer songId);

    /**
     * 编辑用户自建歌单详情（名称、简介、封面）
     */
    void editPlaylist(Long userId, Integer playlistId, String title, String profile, String coverUrl);

    /**
     * 收藏歌单到音乐库
     */
    void collectPlaylist(Long userId, Integer playlistId);

    /**
     * 取消收藏歌单
     */
    void uncollectPlaylist(Long userId, Integer playlistId);

    /**
     * 检查歌单是否已收藏
     */
    boolean isPlaylistCollected(Long userId, Integer playlistId);

    /**
     * 切换歌单公开/私密状态
     */
    void togglePlaylistPrivacy(Long userId, Integer playlistId);
}
