package com.ty1l.spotify_remake.Entity.Public;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchSongVO {
    private Integer id;
    private String title;
    private String coverUrl;
    private String coverNetworkUrl;
    private Integer duration;
    private String artistName;
    private Integer albumId;
    private String albumName;
}
