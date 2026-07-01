package com.ty1l.spotify_remake.Controller.Public;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * AJ-Captcha 控制器（Jakarta 兼容版）
 * <p>
 * AJ-Captcha 1.3.0 内置的 CaptchaController 使用 javax.servlet（Spring Boot 2.x 兼容），
 * 且其自动配置依赖 spring.factories（Spring Boot 3.x 已移除），导致端点不注册。
 * 本控制器用 jakarta.servlet 替代，直接委托 {@link CaptchaService} bean。
 * </p>
 */
@RestController
@Slf4j
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    /**
     * 获取验证码（滑块/点选）
     * <p>前端 AJ-Captcha SDK 发起 POST，body 含 {@code captchaType: "blockPuzzle"}。</p>
     */
    @PostMapping("/captcha/get")
    public ResponseModel getCaptcha(@RequestBody CaptchaVO captchaVO, HttpServletRequest request) {
        log.debug("验证码获取请求: captchaType={}", captchaVO.getCaptchaType());
        return captchaService.get(captchaVO);
    }

    /**
     * 前端校验验证码（滑块位置比对）
     * <p>前端 AJ-Captcha SDK 在用户完成滑块后发起，body 含滑块偏移数据。</p>
     */
    @PostMapping("/captcha/check")
    public ResponseModel checkCaptcha(@RequestBody CaptchaVO captchaVO, HttpServletRequest request) {
        log.debug("验证码校验请求: captchaType={}", captchaVO.getCaptchaType());
        return captchaService.check(captchaVO);
    }
}
