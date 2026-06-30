package com.ty1l.spotify_remake.utility;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.ty1l.spotify_remake.Exception.SignUpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 验证码二次核验工具类
 * 调用 AJ-Captcha 官方 verification 方法，从 Redis 取出数据比对并用完即焚。
 */
@Component
@Slf4j
public class CaptchaVerifier {

    @Autowired
    private CaptchaService captchaService;

    /**
     * 校验前端传来的验证码凭证是否有效
     *
     * @param captchaVerification 前端 &lt;Verify&gt; 组件 success 事件抛出的 captchaVerification
     * @return true 表示验证通过
     * @throws SignUpException 验证失败时抛出异常
     */
    public boolean verify(String captchaVerification) {
        // 1. 判空
        if (captchaVerification == null || captchaVerification.trim().isEmpty()) {
            log.warn("二次核验失败：前端传来的 captchaVerification 为空");
            throw new SignUpException("Please verify the captcha!");
        }

        // 2. 构建 CaptchaVO，设置二次验证凭证
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(captchaVerification);

        // 3. 调用官方 verification：Redis 取数据 → 比对 → 删除 key（用完即焚）
        ResponseModel result = captchaService.verification(captchaVO);
        log.info("二次核验结果 — captchaVerification: {}, success: {}", captchaVerification, result.isSuccess());

        if (!result.isSuccess()) {
            log.warn("二次核验失败：captchaVerification {} 无效或已被使用", captchaVerification);
            throw new SignUpException("Captcha verification failed, please try again.");
        }

        return true;
    }
}
