# AJ-Captcha Redis Migration Design

**Date:** 2026-06-30
**Status:** Approved

## Overview

将 AJ-Captcha 验证码模块从自建本地内存缓存迁移到官方 Redis 缓存实现，前端用官方 `<Verify>` 组件替代手写滑块组件。

## Current State

| 文件 | 问题 |
|------|------|
| `CaptchaConfig.java` | 强制 `cache-type=local`，忽略 `application.yaml` 的 `redis` 配置 |
| `LocalCaptchaCache.java` | 自建 ConcurrentHashMap + 定时清理，模拟 Redis |
| `CustomCaptchaController.java` | 手写 `/captcha/get` + `/captcha/check`，从 picsum 拉图、简单矩形切片 |
| `CaptchaVerifier.java` | 读 LocalCaptchaCache 做二次核验 |
| `CaptchaModal.vue` | 手写滑块 UI + 拖拽逻辑 |
| `AuthPage*.vue` (3个) | ~20 个 captcha 状态变量、手动拖拽事件处理 |

## Target State

全面切换到 AJ-Captcha 1.3.0 官方实现。

## Backend Changes

### 1. Rewrite `CaptchaConfig.java`

```java
@Configuration
public class CaptchaConfig {

    @Value("${aj.captcha.type:blockPuzzle}")
    private String captchaType;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Bean
    public CaptchaService captchaService() {
        CaptchaCacheServiceRedisImpl cacheService = new CaptchaCacheServiceRedisImpl();
        cacheService.setStringRedisTemplate(stringRedisTemplate);

        Properties props = new Properties();
        props.setProperty("aj.captcha.cache-type", "redis");
        props.setProperty("aj.captcha.type", captchaType);

        if ("clickWord".equals(captchaType)) {
            return new ClickWordCaptchaServiceImpl(cacheService);
        } else {
            return new BlockPuzzleCaptchaServiceImpl(cacheService);
        }
    }
}
```

- 创建 `CaptchaCacheServiceRedisImpl` 实例，注入 `StringRedisTemplate`
- 通过构造函数传递给 Service 实现类
- AJ-Captcha 内置端点自动注册在 `/captcha/get` 和 `/captcha/check`（路径不变）
- 坐标 AES 加密存入 Redis，1分钟过期，校验时取出比对并用完即焚

### 2. Delete `LocalCaptchaCache.java`

全部缓存操作由 `CaptchaCacheServiceRedisImpl` 通过 Redis 完成。

### 3. Delete `CustomCaptchaController.java`

AJ-Captcha 内置端点自动接管 `/captcha/get` 和 `/captcha/check`，无需手动注册。

### 4. Rewrite `CaptchaVerifier.java`

```java
@Component
public class CaptchaVerifier {
    @Autowired
    private CaptchaService captchaService;

    public boolean verify(String captchaVerification) {
        if (captchaVerification == null || captchaVerification.trim().isEmpty()) {
            throw new SignUpException("Please verify the captcha!");
        }
        ResponseModel result = captchaService.verification(captchaVerification);
        if (!result.isSuccess()) {
            throw new SignUpException("Captcha verification failed!");
        }
        return true;
    }
}
```

- `captchaService.verification()` 自动从 Redis 取数据 → 比对 → 删除 key
- 不再需要手动拼 key 前缀、管理状态机

### 5. `SecurityConfig.java` — No Changes

`/captcha/get` 和 `/captcha/check` 已在白名单中。

### 6. `application.yaml` — No Changes

已有配置保持不变：
```yaml
aj:
  captcha:
    type: blockpuzzle
    cache-type: redis
    watermark: "MyProject"
    aes-status: true
spring:
  data:
    redis:
      host: 192.168.100.128
      port: 6379
```

## Frontend Changes

### 1. `index.html` — Add official Verify script

```html
<script src="https://cdn.jsdelivr.net/npm/verify@1.3.0/dist/verify.min.js"></script>
```

### 2. Replace `CaptchaModal.vue` with `CaptchaVerify.vue`

```vue
<template>
  <div v-if="visible" class="captcha-verify-overlay" @click.self="$emit('close')">
    <div class="captcha-verify-box">
      <Verify
        :mode="'pop'"
        :captchaType="'blockPuzzle'"
        :imgSize="{ width: '310px', height: '155px' }"
        :barSize="{ width: '310px', height: '40px' }"
        @success="onSuccess"
        @error="onError"
      />
    </div>
  </div>
</template>
```

- 官方组件自动调用 `/captcha/get` 获取验证码、渲染真实拼图形状的滑块
- 用户拖拽完成后自动调用 `/captcha/check` 验证
- `success` 事件的 `params.captchaVerification` 即为后端二次核验所需凭证

### 3. Simplify `AuthPage.vue` / `AuthPageEnhanced.vue` / `AuthPageLocalized.vue`

删除 20+ 个 captcha 相关状态变量和所有手动拖拽处理函数，简化为：

```js
const captchaVerificationToken = ref('')
const captchaVerified = ref(false)
const captchaVerifyVisible = ref(false)

function onCaptchaSuccess(params) {
  captchaVerificationToken.value = params.captchaVerification
  captchaVerified.value = true
  captchaVerifyVisible.value = false
}
```

改动：
- 「Request Code」按钮 → 直接打开 Verify 弹窗（`captchaVerifyVisible = true`）
- `CaptchaModal` 替换为 `CaptchaVerify`
- 删除 `import CaptchaModal from './CaptchaModal.vue'`
- 删除所有 slider 相关变量和事件处理函数（`handleSliderStart/Move/End`、`fetchCaptcha`、`closeCaptchaModal` 等）
- 表单提交逻辑不变（仍传 `token: captchaVerificationToken` 给后端 signup/resetPassword）

### 4. `auth.js` — Delete `getCaptcha()` and `checkCaptcha()`

官方 `<Verify>` 组件内部自行发起 HTTP 请求，前端不再需要这两个 API 封装。

### 5. `vite.config.js` — No Changes

`/captcha` 代理已配置。

## Files Summary

| Action | File |
|--------|------|
| **Backend — Rewrite** | `config/CaptchaConfig.java` |
| **Backend — Delete** | `utility/LocalCaptchaCache.java` |
| **Backend — Delete** | `Controller/User/CustomCaptchaController.java` |
| **Backend — Rewrite** | `utility/CaptchaVerifier.java` |
| **Frontend — Edit** | `spotify-frontend/index.html` (add script tag) |
| **Frontend — Replace** | `CaptchaModal.vue` → `CaptchaVerify.vue` |
| **Frontend — Edit** | `AuthPage.vue` (simplify captcha logic) |
| **Frontend — Edit** | `AuthPageEnhanced.vue` (simplify captcha logic) |
| **Frontend — Edit** | `AuthPageLocalized.vue` (simplify captcha logic) |
| **Frontend — Edit** | `api/auth.js` (remove getCaptcha/checkCaptcha) |
| **No Change** | `config/SecurityConfig.java` |
| **No Change** | `config/RedisConfig.java` |
| **No Change** | `application.yaml` |
| **No Change** | `vite.config.js` |

## Redis Key Design

官方 `CaptchaCacheServiceRedisImpl` 的 key 格式（不做自定义，完全交给官方管理）：
- 验证码数据：内置格式，1分钟过期
- 二次验证：`captchaService.verification()` 内部取出并删除（用完即焚）

## Verification

1. 启动后端，确认 `/captcha/get` 返回官方格式的验证码数据
2. 前端打开登录页，点击验证码触发 `<Verify>` 组件
3. 完成滑块验证，确认 `success` 事件返回 `captchaVerification`
4. 执行注册/重置密码，确认后端 `captchaService.verification()` 通过
5. 重复使用同一个 `captchaVerification`，确认第二次失败（用完即焚）
