package com.ty1l.spotify_remake.utility;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtGenerate {
    private static final String SECRET = "aXRjYXN0";//用来设置jwt的密钥
    private static final long EXPIRE_TIME = 604800000L;//用来设置jwt过期时间为7天
    private static final long ACCESS_EXPIRE = 1200000L;// Access Token 过期时间：20分钟
    private static final long REFRESH_EXPIRE = 604800000L;// Refresh Token 过期时间：7天

    //用来生成jwt（保留向后兼容，默认7天过期）
    public static String generateJwt(Map<String, Object> claims){
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    //生成 Access Token（20分钟过期）
    public static String generateAccessToken(Map<String, Object> claims){
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRE))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    //生成 Refresh Token（7天过期）
    public static String generateRefreshToken(Map<String, Object> claims){
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRE))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    //用来解析jwt
    public static Claims parseJwt(String jwt){
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(jwt)
                .getBody();
    }
}
