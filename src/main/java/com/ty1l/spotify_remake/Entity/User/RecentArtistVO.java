package com.ty1l.spotify_remake.Entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentArtistVO {
    private Long id;           // 歌手ID
    private String name;       // 歌手名称
    private String avatarUrl;  // 头像URL
    private String avatarNetworkUrl; // 头像URL（网络）
}
