package com.ty1l.spotify_remake.Entity.Public;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalTrackVO {
    private String externalId;
    private String source;      // "gdmusic"
    private String lyricId;     // 歌词ID，用于获取外部歌词
    private String picId;         // 封面图片ID（用于重新获取封面）
    private String title;
    private String artistName;
    private String coverUrl;
    private Integer duration;   // 秒
    private String streamUrl;   // 后端代理流地址
}
