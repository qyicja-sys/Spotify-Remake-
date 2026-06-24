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
    private String title;
    private String artistName;
    private String coverUrl;
    private Integer duration;   // 秒
    private String streamUrl;   // 后端代理流地址
}
