package com.ty1l.spotify_remake.utility;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 签名 URL 工具类
 *
 * 原理（Spotify 方案）：
 * 1. 前端请求播放歌曲时，调用后端接口获取签名 URL
 * 2. 后端用 HMAC-SHA256 对「资源路径 + 过期时间」签名，生成带签名的临时 URL
 * 3. 前端用这个临时 URL 直接加载音频，无需带 token
 * 4. Filter 拦截所有静态资源请求，验证签名和过期时间
 * 5. 签名过期后 URL 失效，需要重新获取
 */
public class SignedUrlUtil {

    // 与 JwtGenerate 共用密钥，避免引入额外配置
    private static final String SECRET = "aXRjYXN0";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    // 默认 5 分钟有效期
    private static final long DEFAULT_EXPIRE_SECONDS = 300;

    /**
     * 生成签名 URL
     *
     * @param resourcePath 资源路径，如 /static/datas/musicResouces/musics/song.mp3
     * @param expireSeconds 有效期（秒）
     * @return 完整签名 URL，如 /static/datas/.../song.mp3?expires=1699999999&sig=abc123
     */
    public static String generate(String resourcePath, long expireSeconds) {
        long expires = System.currentTimeMillis() / 1000 + expireSeconds;
        String signature = sign(resourcePath, expires);
        return resourcePath + "?expires=" + expires + "&sig=" + signature;
    }

    /**
     * 生成签名 URL（默认 5 分钟有效期）
     */
    public static String generate(String resourcePath) {
        return generate(resourcePath, DEFAULT_EXPIRE_SECONDS);
    }

    /**
     * 验证签名 URL
     *
     * @param resourcePath 资源路径（不含查询参数）
     * @param expires 过期时间戳（秒）
     * @param sig 签名
     * @return true = 签名合法且未过期
     */
    public static boolean verify(String resourcePath, String expires, String sig) {
        if (expires == null || sig == null) {
            return false;
        }

        // 1. 检查是否过期
        long expiresLong;
        try {
            expiresLong = Long.parseLong(expires);
        } catch (NumberFormatException e) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000;
        if (now > expiresLong) {
            return false;
        }

        // 2. 验证签名
        String expectedSig = sign(resourcePath, expiresLong);
        return expectedSig.equals(sig);
    }

    /**
     * HMAC-SHA256 签名
     * 签名内容 = 资源路径 + "|" + 过期时间戳
     */
    private static String sign(String resourcePath, long expires) {
        String payload = resourcePath + "|" + expires;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            // 转为 hex 字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败", e);
        }
    }
}
