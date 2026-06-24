package com.ty1l.spotify_remake.Entity.User;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeDashboardVO {
    // 用户信息
    private LoginInfoVo userInfo;

    // 用户自建的歌单列表
    private List<PlaylistBriefVO> myPlaylists;

    // 系统推荐的歌单列表
    private List<PlaylistBriefVO> systemPlaylists;

    // 用户收藏的歌单列表
    private List<PlaylistBriefVO> collectedPlaylists;

    // 用户关注的艺人列表
    private List<Artist> followedArtists;
}
