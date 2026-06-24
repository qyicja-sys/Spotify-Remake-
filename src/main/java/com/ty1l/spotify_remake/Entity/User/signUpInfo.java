package com.ty1l.spotify_remake.Entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class signUpInfo {
    private String userName;
    private String nickName;
    private String email;
    private String password;
    private String token;//用于接收滑块验证通过后的凭证 (captchaVerification)
}
