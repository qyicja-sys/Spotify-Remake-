package com.ty1l.spotify_remake.Entity.Public;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongArtist {
    private Integer songId;
    private Integer artistId;
    private Integer isMain; // 1=主创, 0=feat
}
