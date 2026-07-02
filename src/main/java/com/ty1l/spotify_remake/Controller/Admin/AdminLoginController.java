package com.ty1l.spotify_remake.Controller.Admin;

import com.ty1l.spotify_remake.Entity.Admin.AdminLoginVO;
import com.ty1l.spotify_remake.Service.Admin.AdminLoginService;
import com.ty1l.spotify_remake.utility.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@Slf4j
@RestController
@RequestMapping("/admin/spotify")
public class AdminLoginController {

    @Autowired
    private AdminLoginService adminLoginService;

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        log.info("Admin login attempt: {}", username);
        AdminLoginVO vo = adminLoginService.login(username, password);
        return Result.success(vo);
    }

    @PostMapping("/token/refresh")
    public Result refreshToken(@RequestBody Map<String, String> map) {
        String refreshToken = map.get("refreshToken");
        log.info("Admin token refresh requested");
        AdminLoginVO vo = adminLoginService.refresh(refreshToken);
        return Result.success(vo);
    }
}
