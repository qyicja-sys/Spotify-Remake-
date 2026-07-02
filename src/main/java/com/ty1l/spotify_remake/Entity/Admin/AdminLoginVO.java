package com.ty1l.spotify_remake.Entity.Admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginVO {
    private Integer id;
    private String username;
    private String token;
    private String refreshToken;
}
