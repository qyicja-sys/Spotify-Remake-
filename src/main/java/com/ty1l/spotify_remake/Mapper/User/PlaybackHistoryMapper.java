package com.ty1l.spotify_remake.Mapper.User;

import com.ty1l.spotify_remake.Entity.User.RecentArtistVO;
import com.ty1l.spotify_remake.Entity.User.RecentlyPlayedSongVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ty1l.spotify_remake.Entity.User.UserPlaybackHistory;

import java.util.List;

@Mapper
public interface PlaybackHistoryMapper {

    // 查询用户最近播放的歌手（去重，按最近播放时间排序，最多5个）
    List<RecentArtistVO> selectRecentArtists(@Param("userId") Long userId, @Param("limit") int limit);

    // 查询用户最近播放的歌曲（去重，按最近播放时间排序，最多50首）
    List<RecentlyPlayedSongVO> selectRecentSongs(@Param("userId") Long userId, @Param("limit") int limit);

    // 查询用户关注的艺人数
    int countFollowing(@Param("userId") Long userId);

    // 查询某艺人本月听众数（去重，按 user_playback_history 统计）
    int countMonthlyListeners(@Param("artistId") Integer artistId);

    // 插入播放记录
    int insertPlaybackHistory(UserPlaybackHistory history);
}
