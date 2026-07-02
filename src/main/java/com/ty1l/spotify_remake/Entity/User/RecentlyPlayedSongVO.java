package com.ty1l.spotify_remake.Entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentlyPlayedSongVO {
    private Long id;           // 歌曲ID
    private String title;      // 歌曲名称
    private String artistName; // 歌手名称
    private String coverUrl;   // 封面URL
    private Integer duration;  // 时长(秒)
    private String playedAt;   // 最近播放时间
    private String externalSource; // 外部来源
    private String externalId;     // 外部ID
    private String coverNetworkUrl; // 封面URL（网络）
    private String picId;            // 外部封面图片ID
}
