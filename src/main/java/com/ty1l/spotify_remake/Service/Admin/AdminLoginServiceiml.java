package com.ty1l.spotify_remake.Service.Admin;

import com.ty1l.spotify_remake.Entity.Admin.Admin;
import com.ty1l.spotify_remake.Entity.Admin.AdminLoginVO;
import com.ty1l.spotify_remake.Exception.LoginFailedException;
import com.ty1l.spotify_remake.Mapper.Admin.AdminMapper;
import com.ty1l.spotify_remake.utility.JwtGenerate;
import com.ty1l.spotify_remake.utility.TokenService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminLoginServiceiml implements AdminLoginService {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private TokenService tokenService;

    @Override
    public AdminLoginVO login(String username, String password) {
        Admin admin = adminMapper.findByUsername(username);
        if (admin == null) {
            throw new LoginFailedException("管理员账号不存在");
        }
        if (!BCrypt.checkpw(password, admin.getPassword())) {
            throw new LoginFailedException("密码错误");
        }

        // 生成 token version（UUID），用于 Redis 双重Token机制
        String version = tokenService.generateVersion();

        // 生成 Access Token（20分钟过期）
        Map<String, Object> atClaims = new HashMap<>();
        atClaims.put("userId", admin.getId());
        atClaims.put("role", "admin");
        atClaims.put("version", version);
        atClaims.put("type", "access");
        String accessToken = JwtGenerate.generateAccessToken(atClaims);

        // 生成 Refresh Token（7天过期）
        Map<String, Object> rtClaims = new HashMap<>();
        rtClaims.put("userId", admin.getId());
        rtClaims.put("role", "admin");
        rtClaims.put("version", version);
        rtClaims.put("type", "refresh");
        String refreshToken = JwtGenerate.generateRefreshToken(rtClaims);

        // 存储 version 到 Redis，其他设备登录时会覆盖，使当前设备失效
        tokenService.storeAdminVersion(admin.getId().longValue(), version);

        AdminLoginVO vo = new AdminLoginVO(admin.getId(), admin.getUsername(), accessToken, refreshToken);
        return vo;
    }

    @Override
    public AdminLoginVO refresh(String refreshToken) {
        // 解析 Refresh Token
        Claims claims;
        try {
            claims = JwtGenerate.parseJwt(refreshToken);
        } catch (Exception e) {
            throw new LoginFailedException("Invalid refresh token");
        }

        // 校验 token 类型
        String type = String.valueOf(claims.get("type"));
        if (!"refresh".equals(type)) {
            throw new LoginFailedException("Token type must be refresh");
        }

        // 校验角色
        String role = String.valueOf(claims.get("role"));
        if (!"admin".equals(role)) {
            throw new LoginFailedException("Invalid admin token");
        }

        Long adminId = Long.valueOf(claims.get("userId").toString());
        String oldVersion = String.valueOf(claims.get("version"));

        // 校验旧 version 并生成新 version（内部校验 Redis 匹配 + 更新）
        String newVersion;
        try {
            newVersion = tokenService.refreshAdminVersion(adminId, oldVersion);
        } catch (TokenService.TokenInvalidException e) {
            throw new LoginFailedException(e.getMessage());
        }

        // 生成新的 Access Token（20分钟过期）
        Map<String, Object> atClaims = new HashMap<>();
        atClaims.put("userId", adminId);
        atClaims.put("role", "admin");
        atClaims.put("version", newVersion);
        atClaims.put("type", "access");
        String newAccessToken = JwtGenerate.generateAccessToken(atClaims);

        // 生成新的 Refresh Token（7天过期）
        Map<String, Object> rtClaims = new HashMap<>();
        rtClaims.put("userId", adminId);
        rtClaims.put("role", "admin");
        rtClaims.put("version", newVersion);
        rtClaims.put("type", "refresh");
        String newRefreshToken = JwtGenerate.generateRefreshToken(rtClaims);

        return new AdminLoginVO(adminId.intValue(), null, newAccessToken, newRefreshToken);
    }
}
