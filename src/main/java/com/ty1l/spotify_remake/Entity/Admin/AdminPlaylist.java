package com.ty1l.spotify_remake.Entity.Admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPlaylist {
    private Integer id;
    private String userId;
    private String title;
    private String coverUrl;
    private String coverNetworkUrl;
    private String backgroundUrl;
    private String backgroundNetworkUrl;
    private String profile;
    private String createAt;
    private Integer type;
    private Integer isPrivate; // 0=公开, 1=私人
}
