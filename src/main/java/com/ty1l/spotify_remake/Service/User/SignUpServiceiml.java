package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.User.LoginInfoVo;
import com.ty1l.spotify_remake.Entity.User.User;
import com.ty1l.spotify_remake.Entity.User.signUpInfo;
import com.ty1l.spotify_remake.Exception.SignUpException;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Mapper.User.SignUpMapper;
import com.ty1l.spotify_remake.Mapper.User.UserMapper;

import com.ty1l.spotify_remake.utility.CaptchaVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SignUpServiceiml implements SignUpService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SignUpMapper signUpMapper;
    @Autowired
    private ArtistMapper artistMapper;



    @Override
    public LoginInfoVo signUp(signUpInfo signUpInfo) {
        // ================== 新增：验证码二次核验 ==================
        String token = signUpInfo.getToken();
        if (CaptchaVerifier.verify(token)==false) {
            throw new SignUpException("Captcha verification failed, please try again.");
        }
        // =======================================================
        try {
            // 1. 检查邮箱是否已存在
            if(userMapper.FindUserByEmail(signUpInfo.getEmail()) > 0){
                throw new SignUpException("Email is exist");
            }
            // 2. 检查用户名是否已存在
            if(userMapper.FindUserByName(signUpInfo.getUserName()) > 0) {
                throw new SignUpException("Username is exist");
            }
            // 3. 检查昵称是否为已存在的歌手名
            Artist artist = artistMapper.findByName(signUpInfo.getNickName().trim());
            if(artist != null) {
                throw new SignUpException("This nickname is reserved for an artist");
            }
            //-------------------------------------------------------------------


            // 1. 执行插入，rows 为影响行数
            //将密码加密
            String hashedPassword = BCrypt.hashpw(signUpInfo.getPassword(), BCrypt.gensalt());
            //创建新用户对象
            User newUser = new User(signUpInfo.getUserName(), signUpInfo.getNickName(), signUpInfo.getEmail(), hashedPassword);
            int rows = userMapper.Register(newUser);
            if(rows > 0){
                //捕获用户ID
                Integer userId = userMapper.FindUserIdByName(signUpInfo.getUserName());
                //向playlist插入收藏歌单
                signUpMapper.InsertLikePlaylist(userId);
                return new LoginInfoVo(signUpInfo.getEmail(), signUpInfo.getNickName(), "/static/datas/profilePic/default.jpg", null);
            }
        } catch (DuplicateKeyException e) {
            // 捕获数据库唯一键冲突异常
            log.error("注册失败，用户名或邮箱已存在: {}", e.getMessage());
            throw new SignUpException("SignUp failed");
        }
        throw new SignUpException("SignUp failed");
    }
}
