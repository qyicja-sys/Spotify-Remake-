package com.ty1l.spotify_remake.Entity.User;

import com.ty1l.spotify_remake.Entity.Public.SearchSongVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileVO {
    private String nickName;        // 用户昵称
    private String email;           // 邮箱
    private String profilePic;      // 头像URL
    private Integer isArtist;       // 是否为艺术家 0=否 1=是
    private Integer playlistCount;  // 公开歌单数量（不含已点赞的歌曲）
    private Integer followingCount; // 关注艺人数
    private Integer fansCount;         // 粉丝数/关注人数（仅艺术家有）
    private Integer monthlyListeners;  // 月听众数（仅艺术家有，按 user_playback_history 统计）
    private List<RecentArtistVO> recentArtists;       // 最近听的歌手(最多5个)
    private List<RecentlyPlayedSongVO> recentSongs;   // 最近听的歌(最多50首)
    private List<PlaylistBriefVO> publicPlaylists;    // 公开歌单
    private Integer artistId;                         // 艺术家ID（仅艺术家有）
    private List<SearchSongVO> artistSongs;           // 艺术家自己的歌曲（最多10首）
}
