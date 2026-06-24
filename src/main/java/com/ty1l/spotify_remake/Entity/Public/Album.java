package com.ty1l.spotify_remake.Entity.Public;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Album {
    private Long id;
    private Long artistId;
    private String name;
    private String coverUrl;
    private String coverNetworkUrl;
    private String description;
    private Integer type; // 0=Single, 1=EP, 2=Album, 3=Compilation
    private LocalDate releaseDate;
    private LocalDateTime createdAt;
}
