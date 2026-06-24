package com.ty1l.spotify_remake.Service.Admin;

import com.ty1l.spotify_remake.Entity.Admin.Admin;
import com.ty1l.spotify_remake.Entity.Admin.AdminLoginVO;
import com.ty1l.spotify_remake.Exception.LoginFailedException;
import com.ty1l.spotify_remake.Mapper.Admin.AdminMapper;
import com.ty1l.spotify_remake.utility.JwtGenerate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminLoginServiceiml implements AdminLoginService {

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public AdminLoginVO login(String username, String password) {
        Admin admin = adminMapper.findByUsername(username);
        if (admin == null) {
            throw new LoginFailedException("管理员账号不存在");
        }
        if (!BCrypt.checkpw(password, admin.getPassword())) {
            throw new LoginFailedException("密码错误");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", admin.getId());
        claims.put("role", "admin");
        String token = JwtGenerate.generateJwt(claims);

        return new AdminLoginVO(admin.getId(), admin.getUsername(), token);
    }
}
