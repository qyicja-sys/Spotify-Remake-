package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.User.LoginInfoVo;
import com.ty1l.spotify_remake.Entity.User.signUpInfo;


public interface SignUpService {
    // 注册用户
    LoginInfoVo signUp(signUpInfo signUpInfo);
}
