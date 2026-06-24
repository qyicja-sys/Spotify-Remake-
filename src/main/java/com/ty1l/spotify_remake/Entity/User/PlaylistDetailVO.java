package com.ty1l.spotify_remake.Entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistDetailVO {
    private Integer id;
    private Integer userId;
    private String title;
    private String coverUrl;
    private String coverNetworkUrl;
    private String backgroundUrl;
    private String backgroundNetworkUrl;
    private Integer type;
    private Integer isPrivate; // 0=公开, 1=私人
    private String profile;
    private Integer songCount;
    private Integer totalDuration;
    private List<PlaylistSongVO> songs;
}
