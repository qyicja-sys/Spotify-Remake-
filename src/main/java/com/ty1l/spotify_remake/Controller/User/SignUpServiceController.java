package com.ty1l.spotify_remake.Controller.User;

import com.ty1l.spotify_remake.Entity.User.LoginInfoVo;
import com.ty1l.spotify_remake.Entity.User.signUpInfo;
import com.ty1l.spotify_remake.Service.User.SignUpService;
import com.ty1l.spotify_remake.utility.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@Slf4j
@RequestMapping("/spotify")
public class SignUpServiceController {
    @Autowired
    private SignUpService signUpService;
    // 注册用户
    @PostMapping(value = "/signup")
    public Result signUp(@RequestBody signUpInfo signUpInfo){
        LoginInfoVo loginInfoVo = signUpService.signUp(signUpInfo);
        log.info("SignUp: {}", signUpInfo);
        return Result.success(loginInfoVo);
    }
}