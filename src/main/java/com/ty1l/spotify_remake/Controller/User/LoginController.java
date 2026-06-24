package com.ty1l.spotify_remake.Controller.User;

import com.ty1l.spotify_remake.Entity.User.HomeDashboardVO;
import com.ty1l.spotify_remake.Entity.User.LoginUser;
import com.ty1l.spotify_remake.Service.User.LoginService;
import com.ty1l.spotify_remake.utility.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@Slf4j
@RestController
@RequestMapping("/spotify")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping(value = "/login")
    public Result Login(@RequestBody LoginUser user)throws Exception{
        log.info("Login: {}", user);
        HomeDashboardVO homeDashboardVO = loginService.Login(user);
        return Result.success(homeDashboardVO);
    }

    // 忘记密码
    @PostMapping(value = "/login/forgetPassword")
    public Result forgetPassword(@RequestBody Map<String, String> map)throws Exception{
        log.info("ForgetPassword: {}", map);
        loginService.forgetPassword(map.get("email"), map.get("NewPassword"), map.get("token"));
        return Result.success("Reset Password Success");
    }
}
