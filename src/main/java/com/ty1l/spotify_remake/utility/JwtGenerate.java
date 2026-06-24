package com.ty1l.spotify_remake.utility;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtGenerate {
    private static final String SECRET = "aXRjYXN0";//用来设置jwt的密钥
    private static final long EXPIRE_TIME = 43200000L;//用来设置jwt过期时间为12小时

    //用来生成jwt
    public static String generateJwt(Map<String, Object> claims){
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
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
