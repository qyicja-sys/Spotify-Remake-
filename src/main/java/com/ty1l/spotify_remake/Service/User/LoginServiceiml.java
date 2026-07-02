package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.User.*;
import com.ty1l.spotify_remake.Exception.LoginFailedException;
import com.ty1l.spotify_remake.Mapper.User.LoginMapper;
import com.ty1l.spotify_remake.Mapper.User.UserMapper;
import com.ty1l.spotify_remake.utility.CaptchaVerifier;
import com.ty1l.spotify_remake.utility.JwtGenerate;
import com.ty1l.spotify_remake.utility.TokenService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoginServiceiml implements LoginService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private LoginMapper loginMapper;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private CaptchaVerifier captchaVerifier;


    // 登录
    @Override
    public HomeDashboardVO Login(LoginUser user) {
        User exist = userMapper.Login(user);
        if(exist == null){
            throw new LoginFailedException("Email is not exist");
        }
        // 验证密码是否正确
        if(BCrypt.checkpw(user.getPassword(), exist.getPassword())){
            // 生成 token version（UUID），用于 Redis 双重Token机制
            String version = tokenService.generateVersion();

            // 生成 Access Token（20分钟过期）
            Map<String, Object> atClaims = new HashMap<>();
            atClaims.put("userId", exist.getId());
            atClaims.put("userName", exist.getUserName());
            atClaims.put("version", version);
            atClaims.put("type", "access");
            String accessToken = JwtGenerate.generateAccessToken(atClaims);

            // 生成 Refresh Token（7天过期）
            Map<String, Object> rtClaims = new HashMap<>();
            rtClaims.put("userId", exist.getId());
            rtClaims.put("version", version);
            rtClaims.put("type", "refresh");
            String refreshToken = JwtGenerate.generateRefreshToken(rtClaims);

            // 存储 version 到 Redis，B设备登录时会覆盖，使A设备失效
            tokenService.storeUserVersion(exist.getId().longValue(), version);

            //封装HomeDashboardVO，给主界面提供相关数据，包含用户信息、用户自建的歌单列表、系统推荐的歌单列表
            LoginInfoVo userInfo = new LoginInfoVo(exist.getEmail(), exist.getNickName(), exist.getProfilePic(), accessToken, refreshToken);
            List<PlaylistBriefVO> myPlaylists = completePlaylistCoverUrl(loginMapper.PackageMyPlaylists(exist.getId()));
            List<PlaylistBriefVO> systemPlaylists = completePlaylistCoverUrl(loginMapper.PackageSystemPlaylists());
            HomeDashboardVO homeDashboardVO = new HomeDashboardVO(userInfo, myPlaylists, systemPlaylists, java.util.Collections.emptyList(), java.util.Collections.emptyList());
            return homeDashboardVO;
        }else{
            throw new LoginFailedException("Password is wrong");
        }
    }

    // 刷新Token
    @Override
    public LoginInfoVo refresh(String refreshToken) {
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

        Long userId = Long.valueOf(claims.get("userId").toString());
        String oldVersion = String.valueOf(claims.get("version"));

        // 校验旧 version 并生成新 version（内部校验 Redis 匹配 + 更新）
        String newVersion;
        try {
            newVersion = tokenService.refreshUserVersion(userId, oldVersion);
        } catch (TokenService.TokenInvalidException e) {
            throw new LoginFailedException(e.getMessage());
        }

        // 生成新的 Access Token（20分钟过期）
        Map<String, Object> atClaims = new HashMap<>();
        atClaims.put("userId", userId);
        atClaims.put("version", newVersion);
        atClaims.put("type", "access");
        String newAccessToken = JwtGenerate.generateAccessToken(atClaims);

        // 生成新的 Refresh Token（7天过期）
        Map<String, Object> rtClaims = new HashMap<>();
        rtClaims.put("userId", userId);
        rtClaims.put("version", newVersion);
        rtClaims.put("type", "refresh");
        String newRefreshToken = JwtGenerate.generateRefreshToken(rtClaims);

        // 注意：refreshUserVersion 已经更新了 Redis，这里不需要再次 store

        // 返回新的 token 信息（不包含完整 HomeDashboardVO，前端自行合并）
        return new LoginInfoVo(null, null, null, newAccessToken, newRefreshToken);
    }

    // 忘记密码

    @Override
    public void forgetPassword(String email, String newPassword, String token) {
        // ================== 新增：验证码二次核验 ==================
        if (captchaVerifier.verify(token)==false) {
            throw new LoginFailedException("Captcha verification failed, please try again.");
        }
        // =======================================================
        Integer exist = userMapper.FindUserByEmail(email);
        // 验证邮箱是否存在
        if(exist == null){
            throw new LoginFailedException("Email is not exist");
        }
        // 更新密码
        userMapper.UpdatePassword(email, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
    }

    // 补全歌单图片url（保留相对路径，由前端通过代理或直接访问）
    @Override
    public List<PlaylistBriefVO> completePlaylistCoverUrl(List<PlaylistBriefVO> playlists) {
        return playlists;
    }
}
