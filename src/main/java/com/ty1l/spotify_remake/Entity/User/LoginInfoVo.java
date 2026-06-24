package com.ty1l.spotify_remake.Entity.User;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginInfoVo {// 登录信息，传回给前端，不包含密码
    private String email;
    private String nickName;
    private String profilePic;
    private String jwt;
}
