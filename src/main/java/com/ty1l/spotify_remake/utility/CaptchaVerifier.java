package com.ty1l.spotify_remake.utility;

import com.ty1l.spotify_remake.Exception.SignUpException; // 如果有更通用的业务异常，可以替换
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 验证码二次核验工具类
 */
@Component
@Slf4j
public class CaptchaVerifier {

    /**
     * 校验前端传来的验证码凭证是否有效
     *
     * @param captchaToken 前端传过来的 token (即 captchaVerification)
     * @throws SignUpException 验证失败时直接抛出异常，阻止后续业务执行
     */
    public static boolean verify(String captchaToken) {
        // 1. 判空
        if (captchaToken == null || captchaToken.trim().isEmpty()) {
            log.warn("二次核验失败：前端传来的 captchaToken 为空");
            throw new SignUpException("Please verify the captcha!");
        }

        // 2. 拼接缓存 Key
        String cacheKey = "captcha:" + captchaToken;

        // 3. 从本地缓存中获取状态
        String captchaStatus = LocalCaptchaCache.get(cacheKey);
        log.info("开始二次核验，Token: {}, 缓存状态: {}", captchaToken, captchaStatus);

        // 4. 判断状态是否为第一阶段校验成功的 "SUCCESS"
        if (captchaStatus == null) {
            log.warn("二次核验失败：Token {} 在缓存中不存在或已过期", captchaToken);
            return false;
        }

        if (!"SUCCESS".equals(captchaStatus)) {
            log.warn("二次核验失败：Token {} 尚未完成滑块对齐校验", captchaToken);
            return false;
        }

        // 5. 【核心防刷】校验通过后，立即作废该 Token，防止被抓包重复利用
        // 传入过期时间 0 秒或覆盖为空，达到删除效果
        LocalCaptchaCache.put(cacheKey, "", 0);
        log.info("二次核验成功，Token {} 已作废销毁", captchaToken);
        return true;
    }
}