package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.User.*;
import com.ty1l.spotify_remake.Exception.LoginFailedException;
import com.ty1l.spotify_remake.Mapper.User.LoginMapper;
import com.ty1l.spotify_remake.Mapper.User.UserMapper;
import com.ty1l.spotify_remake.utility.CaptchaVerifier;
import com.ty1l.spotify_remake.utility.JwtGenerate;
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


    // 登录
    @Override
    public HomeDashboardVO Login(LoginUser user) {
        User exist = userMapper.Login(user);
        if(exist == null){
            throw new LoginFailedException("Email is not exist");
        }
        // 验证密码是否正确
        if(BCrypt.checkpw(user.getPassword(), exist.getPassword())){
            Map<String, Object> data = new HashMap<>();
            data.put("id", exist.getId());
            data.put("userName", exist.getUserName());

            String jwt = JwtGenerate.generateJwt(data);
            //封装HomeDashboardVO，给主界面提供相关数据，包含用户信息、用户自建的歌单列表、系统推荐的歌单列表
            LoginInfoVo userInfo = new LoginInfoVo(exist.getEmail(), exist.getNickName(), exist.getProfilePic(), jwt);
            List<PlaylistBriefVO> myPlaylists = completePlaylistCoverUrl(loginMapper.PackageMyPlaylists(exist.getId()));
            List<PlaylistBriefVO> systemPlaylists = completePlaylistCoverUrl(loginMapper.PackageSystemPlaylists());
            HomeDashboardVO homeDashboardVO = new HomeDashboardVO(userInfo, myPlaylists, systemPlaylists, java.util.Collections.emptyList(), java.util.Collections.emptyList());
            return homeDashboardVO;
        }else{
            throw new LoginFailedException("Password is wrong");
        }
    }

    // 忘记密码

    @Override
    public void forgetPassword(String email, String newPassword, String token) {
        // ================== 新增：验证码二次核验 ==================
        if (CaptchaVerifier.verify(token)==false) {
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
