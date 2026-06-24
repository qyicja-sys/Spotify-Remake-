package com.ty1l.spotify_remake.Entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistSongVO {
    private Integer id;
    private Integer artistId;
    private String artistName;
    private String title;
    private String coverUrl;
    private String fileUrl;
    private Integer duration;
    private LocalDateTime createdAt;
    private String addedAt;
    private String externalSource;
    private String externalId;
    private Integer albumId;
    private String albumName;
}
