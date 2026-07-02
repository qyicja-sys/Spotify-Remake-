package com.ty1l.spotify_remake.Entity.Public;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 排行榜歌曲 VO — 返回给前端的歌曲信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardSongVO {
    private Integer id;
    private String title;
    private String artistName;
    private String coverUrl;
    private String coverNetworkUrl;
    private Integer duration;
    private Long playCount;
    private String externalSource;
    private String externalId;
    private String picId;
    /** 排名（1-based） */
    private Integer rank;
}
