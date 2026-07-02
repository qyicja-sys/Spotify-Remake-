package com.ty1l.spotify_remake.Entity.Public;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Song {
    private Integer id;
    private Integer artistId;
    private Long albumId;
    private String title;
    private String coverUrl;
    private String coverNetworkUrl;
    private String fileUrl;
    private Integer duration;
    private LocalDateTime createdAt;
    private String externalSource;
    private String externalId;
    private String lyrics;
    private String lyricsUrl;     // 歌词URL（外部歌词地址）
    private String picId;         // 外部封面图片ID
}
