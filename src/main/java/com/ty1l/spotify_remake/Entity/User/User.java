package com.ty1l.spotify_remake.Entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class User {
    private Integer id;
    private String userName;
    private String nickName;
    private String gender;
    private String email;
    private String password;
    private String phone;
    private String personalMotto;
    private String profilePic;
    private Integer isArtist; // 0=普通用户, 1=艺术家

    //新建一个构造器，用于注册
    public User(String userName, String nickName, String email, String password) {
        this.userName = userName;
        this.nickName = nickName;
        this.email = email;
        this.password = password;
    }
}
