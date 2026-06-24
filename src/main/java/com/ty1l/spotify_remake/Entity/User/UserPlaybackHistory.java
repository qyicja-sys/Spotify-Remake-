package com.ty1l.spotify_remake.Entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPlaybackHistory {
    private Long id;
    private Long userId;
    private Long songId;
    private Long artistId;
    private LocalDateTime playedAt;
}
