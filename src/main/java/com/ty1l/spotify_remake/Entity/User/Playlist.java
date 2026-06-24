package com.ty1l.spotify_remake.Entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Playlist {
    private Integer id;
    private String userId;
    private String name;
    private String profile;
    private String coverUrl;
    private String coverNetworkUrl;
    private String createAt;
    private String type;
    private Integer isPrivate;
}
