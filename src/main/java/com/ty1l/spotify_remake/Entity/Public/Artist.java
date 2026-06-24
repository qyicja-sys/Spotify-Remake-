package com.ty1l.spotify_remake.Entity.Public;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Artist {
    private Integer id;
    private String name;
    private Integer fansCount;
    private Integer monthlyListeners;
    private String biography;
    private String avatarUrl;
    private String avatarNetworkUrl;
    private LocalDateTime createdAt;
}
