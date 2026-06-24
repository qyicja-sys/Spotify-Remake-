package com.ty1l.spotify_remake.Service.Admin;

import com.ty1l.spotify_remake.Entity.Admin.AdminLoginVO;

public interface AdminLoginService {
    AdminLoginVO login(String username, String password);
}
