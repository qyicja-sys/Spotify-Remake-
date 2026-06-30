# AJ-Captcha Redis Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 AJ-Captcha 验证码模块从自建本地内存缓存迁移到 Redis + 官方 `<Verify>` 前端组件。

**Architecture:** 后端自写 `CaptchaCacheServiceRedisImpl` 实现 AJ-Captcha 的 `CaptchaCacheService` 接口，通过预注入到 `CaptchaServiceFactory.cacheService` 静态 Map 让 `init()` 自动发现。前端删除手写滑块组件，用官方 `<Verify>` 组件替代，组件自动处理 `/captcha/get` 和 `/captcha/check` 交互。

**Tech Stack:** Java 17, Spring Boot 3.2.0, AJ-Captcha 1.3.0, StringRedisTemplate, Vue 3, official verify.min.js

## Global Constraints

- 所有文件使用 UTF-8 无 BOM 编码
- 数据库为 database_spotify
- 客户端端口: 5000, 前端 dev: 8000
- 不改动 `application.yaml`、`SecurityConfig.java`、`RedisConfig.java`、`vite.config.js`
- AJ-Captcha 版本锁定 1.3.0，不可升级

---

## Important: AJ-Captcha 1.3.0 API Notes

The plan relies on these verified facts about AJ-Captcha 1.3.0:

- **No built-in Redis cache impl**: The core jar only ships `CaptchaCacheServiceMemImpl`
- **SPI discovery**: `CaptchaServiceFactory` static init uses `ServiceLoader.load(CaptchaCacheService.class)`, calls `type()` on each, stores in `public static volatile Map<String, CaptchaCacheService> cacheService`
- **`getCacheService(String type)`** → `CaptchaServiceFactory.getCache(type)` — reads from the static map
- **`BlockPuzzleCaptchaServiceImpl`**: no-arg constructor only, `init(Properties)` internally calls `getCacheService()` to obtain the cache
- **`CaptchaService.verification(CaptchaVO)`**: takes a `CaptchaVO` object, not String
- **`CaptchaVO`**: has `setCaptchaVerification(String)` and `getCaptchaVerification()` for the token
- **`ResponseModel.isSuccess()`**: returns boolean

### Our custom `CaptchaCacheService` must implement:

```java
public interface CaptchaCacheService {
    void set(String key, String value, long timeoutInSeconds);
    boolean exists(String key);
    void delete(String key);
    String get(String key);
    String type();          // must return "redis"
    default Long increment(String key, long delta) { return 0L; }
}
```

### Pre-injection strategy

Since SPI creates instances via no-arg constructor, our Redis impl cannot get `StringRedisTemplate` via constructor injection through SPI. Instead, we pre-populate the factory map before `init()`:

```java
// In CaptchaConfig, before service.init():
CaptchaCacheServiceRedisImpl cacheService = new CaptchaCacheServiceRedisImpl(stringRedisTemplate);
CaptchaServiceFactory.cacheService.put("redis", cacheService);
```

`CaptchaServiceFactory` static init runs when the class is first loaded (by `new BlockPuzzleCaptchaServiceImpl()`), loading only the default `CaptchaCacheServiceMemImpl`. We then add our "redis" entry. When `init()` calls `getCacheService("redis")`, it finds our instance.

---

## Task 1: Create `CaptchaCacheServiceRedisImpl.java`

**Files:**
- Create: `src/main/java/com/ty1l/spotify_remake/utility/CaptchaCacheServiceRedisImpl.java`

**Interfaces:**
- Consumes: `StringRedisTemplate` (bean from `RedisConfig.java`)
- Produces: class implementing `com.anji.captcha.service.CaptchaCacheService` with `type()` returning `"redis"`

- [ ] **Step 1: Create the implementation**

```java
package com.ty1l.spotify_remake.utility;

import com.anji.captcha.service.CaptchaCacheService;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * AJ-Captcha Redis 缓存实现
 * 将验证码坐标数据存入 Redis，1分钟过期，校验时取出比对并用完即焚。
 */
public class CaptchaCacheServiceRedisImpl implements CaptchaCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    public CaptchaCacheServiceRedisImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String type() {
        return "redis";
    }

    @Override
    public void set(String key, String value, long timeoutInSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, timeoutInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }
}
```

- [ ] **Step 2: Verify the file compiles** (manual: restart backend after all backend tasks complete)

---

## Task 2: Rewrite `CaptchaConfig.java`

**Files:**
- Modify: `src/main/java/com/ty1l/spotify_remake/config/CaptchaConfig.java`

**Interfaces:**
- Consumes: `StringRedisTemplate` (autowired), `CaptchaCacheServiceRedisImpl` (Task 1)
- Produces: `CaptchaService` bean (blockPuzzle or clickWord, backed by Redis cache)

- [ ] **Step 1: Rewrite the class**

```java
package com.ty1l.spotify_remake.config;

import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.BlockPuzzleCaptchaServiceImpl;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.anji.captcha.service.impl.ClickWordCaptchaServiceImpl;
import com.ty1l.spotify_remake.utility.CaptchaCacheServiceRedisImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Properties;

@Configuration
public class CaptchaConfig {

    @Value("${aj.captcha.type:blockPuzzle}")
    private String captchaType;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Bean
    public CaptchaService captchaService() {
        // 1. 创建 Redis 缓存实现，注入 StringRedisTemplate
        CaptchaCacheServiceRedisImpl cacheService = new CaptchaCacheServiceRedisImpl(stringRedisTemplate);

        // 2. 预注入到 CaptchaServiceFactory 的静态 Map 中
        //    "new BlockPuzzleCaptchaServiceImpl()" 触发 CaptchaServiceFactory 静态初始化，
        //    加载 SPI 实现（只有 MemImpl），我们在 init() 之前注入 Redis 实现。
        BlockPuzzleCaptchaServiceImpl service = new BlockPuzzleCaptchaServiceImpl();
        CaptchaServiceFactory.cacheService.put("redis", cacheService);

        // 3. 配置 Properties
        Properties props = new Properties();
        props.setProperty("aj.captcha.cache-type", "redis");
        props.setProperty("aj.captcha.type", captchaType);

        // 4. 根据类型创建
        if ("clickWord".equals(captchaType)) {
            ClickWordCaptchaServiceImpl clickService = new ClickWordCaptchaServiceImpl();
            CaptchaServiceFactory.cacheService.put("redis", cacheService);
            clickService.init(props);
            return clickService;
        } else {
            service.init(props);
            return service;
        }
    }
}
```

- [ ] **Step 2: Note** — The static map injection must happen AFTER `new BlockPuzzleCaptchaServiceImpl()` (which triggers `CaptchaServiceFactory` class loading) and BEFORE `service.init(props)` (which reads from the map). The code above handles this correctly.

---

## Task 3: Delete `LocalCaptchaCache.java`

**Files:**
- Delete: `src/main/java/com/ty1l/spotify_remake/utility/LocalCaptchaCache.java`

- [ ] **Step 1: Delete the file**

```bash
rm src/main/java/com/ty1l/spotify_remake/utility/LocalCaptchaCache.java
```

---

## Task 4: Delete `CustomCaptchaController.java`

**Files:**
- Delete: `src/main/java/com/ty1l/spotify_remake/Controller/User/CustomCaptchaController.java`

AJ-Captcha 内置端点自动注册在 `/captcha/get` 和 `/captcha/check`，路径不变。`SecurityConfig` 已放行这两个路径。

- [ ] **Step 1: Delete the file**

```bash
rm src/main/java/com/ty1l/spotify_remake/Controller/User/CustomCaptchaController.java
```

---

## Task 5: Rewrite `CaptchaVerifier.java`

**Files:**
- Modify: `src/main/java/com/ty1l/spotify_remake/utility/CaptchaVerifier.java`

**Interfaces:**
- Consumes: `CaptchaService` bean (Task 2)
- Produces: `boolean verify(String captchaVerification)` — called by `LoginServiceiml.forgetPassword()` and `SignUpServiceiml.signUp()`

- [ ] **Step 1: Rewrite the class**

```java
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
     * @param captchaVerification 前端 <Verify> 组件 success 事件抛出的 captchaVerification
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
```

- [ ] **Step 2: Verify callers are unaffected** — `LoginServiceiml.forgetPassword()` and `SignUpServiceiml.signUp()` call `CaptchaVerifier.verify(token)` which has the same signature. No changes needed there.

---

## Task 6: Frontend — Add official Verify script to `index.html`

**Files:**
- Modify: `src/main/resources/static/spotify-frontend/index.html`

- [ ] **Step 1: Add the script tag before the closing `</head>` or before `</body>`**

Find the `<script>` tag that loads the main app, and add the Verify CDN script right after the existing external scripts. The exact insertion point depends on the file structure, but the script must load BEFORE Vue app initialization.

```html
<!-- AJ-Captcha official Verify component -->
<script src="https://cdn.jsdelivr.net/npm/@anji-plus/verify@1.3.0/dist/verify.min.js"></script>
```

If the CDN URL doesn't resolve at verification time, try the alternative:
```html
<script src="https://unpkg.com/@anji-plus/verify@1.3.0/dist/verify.min.js"></script>
```

The script registers `window.Verify` globally, which Vue can reference as a custom element.

---

## Task 7: Frontend — Create `CaptchaVerify.vue`

**Files:**
- Create: `src/main/resources/static/spotify-frontend/src/components/CaptchaVerify.vue`
- (Keep `CaptchaModal.vue` until Task 8-10 are complete, then delete it)

**Interfaces:**
- Props: `visible` (Boolean) — controls modal visibility
- Emits: `close`, `success(params)` — params contains `captchaVerification` field

- [ ] **Step 1: Create the component**

```vue
<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['close', 'success', 'error'])

function onSuccess(params) {
  emit('success', params)
}

function onError() {
  emit('error')
}
</script>

<template>
  <div v-if="visible" class="captcha-verify-overlay" @click.self="emit('close')">
    <div class="captcha-verify-box">
      <div class="captcha-verify-header">
        <h3>Slider Verification</h3>
        <p>Drag the slider to fit the puzzle piece</p>
        <button type="button" class="captcha-verify-close" @click="emit('close')">&times;</button>
      </div>
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

<style scoped>
.captcha-verify-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.captcha-verify-box {
  background: #282828;
  border-radius: 8px;
  padding: 24px;
  min-width: 360px;
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.5);
}

.captcha-verify-header {
  margin-bottom: 16px;
}

.captcha-verify-header h3 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #fff;
}

.captcha-verify-header p {
  margin: 0;
  font-size: 13px;
  color: #b3b3b3;
}

.captcha-verify-close {
  position: absolute;
  top: 12px;
  right: 16px;
  background: none;
  border: none;
  color: #b3b3b3;
  font-size: 24px;
  cursor: pointer;
}

.captcha-verify-close:hover {
  color: #fff;
}
</style>
```

- [ ] **Step 2: Verify the global `Verify` component** — This relies on `window.Verify` being registered by the CDN script in Task 6. Vue 3 treats unrecognized custom elements as custom elements by default, but if Vue complains, we may need to configure `compilerOptions.isCustomElement`.

---

## Task 8: Frontend — Simplify `AuthPage.vue`

**Files:**
- Modify: `src/main/resources/static/spotify-frontend/src/components/AuthPage.vue`

**Changes:**
- Replace `import CaptchaModal` with `import CaptchaVerify`
- Delete all slider/fetchCaptcha state and handler functions
- Replace "Request Code" button logic with direct modal opening
- Replace `<CaptchaModal>` template with `<CaptchaVerify>`

- [ ] **Step 1: Replace the import**

Change line 7 (`import CaptchaModal from './CaptchaModal.vue'`):
```js
import CaptchaVerify from './CaptchaVerify.vue'
```

- [ ] **Step 2: Replace captcha state variables**

Delete lines 31-42 (all captcha state variables) and lines 46-47 (computed slider values), lines 49-52 (computed verified states). Replace with:

```js
const captchaVerificationToken = ref('')
const captchaVerified = ref(false)
const captchaVerifyVisible = ref(false)
const captchaScene = ref('signup')
```

Keep `signUpVerified` and `resetVerified` computed properties — they now check `captchaVerificationToken` + `captchaVerifiedScene`:
```js
const signUpVerified = computed(() =>
  captchaVerificationToken.value && captchaVerifiedScene.value === 'signup'
)
const resetVerified = computed(() =>
  captchaVerificationToken.value && captchaVerifiedScene.value === 'reset'
)
```

- [ ] **Step 3: Replace the `resetCaptchaState` function**

Delete lines 59-72 (old resetCaptchaState). Replace with:

```js
function resetCaptchaState() {
  captchaVerificationToken.value = ''
  captchaVerifiedScene.value = ''
  captchaVerified.value = false
  captchaVerifyVisible.value = false
  captchaScene.value = 'signup'
}
```

- [ ] **Step 4: Delete slider/fetchCaptcha functions**

Delete these functions and all their code:
- `handleRequestCode` (the old one that calls fetchCaptcha)
- `handleRequestResetCode`
- `fetchCaptcha`
- `closeCaptchaModal`
- `CAPTCHA_SLIDER_TOP`, `CAPTCHA_TRACK_BUTTON_SIZE`, `CAPTCHA_MAX_OFFSET` constants
- `isDraggingSlider` ref
- `handleSliderStart`, `handleSliderMove`, `handleSliderEnd`

Replace `handleRequestCode` with:

```js
function handleRequestCode() {
  captchaScene.value = 'signup'
  captchaVerified.value = false
  captchaVerifyVisible.value = true
}
```

Replace `handleRequestResetCode` with:

```js
function handleRequestResetCode() {
  captchaScene.value = 'reset'
  captchaVerified.value = false
  captchaVerifyVisible.value = true
}
```

- [ ] **Step 5: Add captcha success/error handlers**

```js
function onCaptchaSuccess(params) {
  captchaVerificationToken.value = params.captchaVerification
  captchaVerified.value = true
  captchaVerifiedScene.value = captchaScene.value
  captchaVerifyVisible.value = false
}

function onCaptchaError() {
  captchaVerifyVisible.value = false
}
```

- [ ] **Step 6: Replace the `CaptchaModal` template section**

Delete lines 651-667 (the entire `<CaptchaModal>` block). Replace with:

```html
    <CaptchaVerify
      :visible="captchaVerifyVisible"
      @close="captchaVerifyVisible = false"
      @success="onCaptchaSuccess"
      @error="onCaptchaError"
    />
```

- [ ] **Step 7: Update the "Request Code" buttons** — Remove the `@click="handleRequestCode"` if it still calls the old fetchCaptcha-based function. Verify it now calls the simplified `handleRequestCode` from Step 4.

---

## Task 9: Frontend — Simplify `AuthPageEnhanced.vue`

**Files:**
- Modify: `src/main/resources/static/spotify-frontend/src/components/AuthPageEnhanced.vue`

**Changes:** Same pattern as Task 8.

- [ ] **Step 1: Replace the import**

Change `import CaptchaModal from './CaptchaModal.vue'` to:
```js
import CaptchaVerify from './CaptchaVerify.vue'
```

- [ ] **Step 2: Replace captcha state variables**

Delete lines 99-115 (captcha state + computed slider). Replace with:

```js
const captchaVerificationToken = ref('')
const captchaVerified = ref(false)
const captchaVerifyVisible = ref(false)
const captchaScene = ref('signup')

const signUpVerified = computed(() =>
  captchaVerificationToken.value && captchaVerifiedScene.value === 'signup'
)
const resetVerified = computed(() =>
  captchaVerificationToken.value && captchaVerifiedScene.value === 'reset'
)
```

Wait — check the actual field name. Looking at the grep output, it's `captchaVerifiedScene` not `captchaVerifiedScene`:
```
100:const captchaVerifiedScene = ref('')
117:  () => captchaVerificationToken.value && captchaVerifiedScene.value === 'signup',
120:  () => captchaVerificationToken.value && captchaVerifiedScene.value === 'reset',
```
Yes, it's `captchaVerifiedScene`.

- [ ] **Step 3: Replace `resetCaptchaState`**

```js
function resetCaptchaState() {
  captchaVerificationToken.value = ''
  captchaVerifiedScene.value = ''
  captchaVerified.value = false
  captchaVerifyVisible.value = false
  captchaScene.value = 'signup'
}
```

- [ ] **Step 4: Delete old captcha functions and add new ones**

Delete: `handleRequestCode`, `handleRequestResetCode`, `fetchCaptcha`, `closeCaptchaModal`, all slider handlers, slider constants, `isDraggingSlider`.

Add:
```js
function handleRequestCode() {
  captchaScene.value = 'signup'
  captchaVerified.value = false
  captchaVerifyVisible.value = true
}

function handleRequestResetCode() {
  captchaScene.value = 'reset'
  captchaVerified.value = false
  captchaVerifyVisible.value = true
}

function onCaptchaSuccess(params) {
  captchaVerificationToken.value = params.captchaVerification
  captchaVerified.value = true
  captchaVerifiedScene.value = captchaScene.value
  captchaVerifyVisible.value = false
}

function onCaptchaError() {
  captchaVerifyVisible.value = false
}
```

- [ ] **Step 5: Replace template**

Replace `<CaptchaModal ...>` block (lines 707-723) with:
```html
    <CaptchaVerify
      :visible="captchaVerifyVisible"
      @close="captchaVerifyVisible = false"
      @success="onCaptchaSuccess"
      @error="onCaptchaError"
    />
```

---

## Task 10: Frontend — Simplify `AuthPageLocalized.vue`

**Files:**
- Modify: `src/main/resources/static/spotify-frontend/src/components/AuthPageLocalized.vue`

**Changes:** Same pattern as Tasks 8 and 9. Key differences based on grep output — the file uses `captchaModalVisible`, `fetchCaptcha`, all the same patterns.

- [ ] **Step 1: Replace the import**

Change `import CaptchaModal from './CaptchaModal.vue'` to:
```js
import CaptchaVerify from './CaptchaVerify.vue'
```

- [ ] **Step 2: Replace captcha state variables**

Same replacement as Tasks 8/9.

- [ ] **Step 3: Replace functions**

Same pattern — delete all slider/fetchCaptcha functions, add simplified `handleRequestCode`, `handleRequestResetCode`, `onCaptchaSuccess`, `onCaptchaError`.

- [ ] **Step 4: Replace template**

Same template replacement — `<CaptchaModal>` → `<CaptchaVerify>`.

---

## Task 11: Frontend — Remove captcha API wrappers from `auth.js`

**Files:**
- Modify: `src/main/resources/static/spotify-frontend/src/api/auth.js`

- [ ] **Step 1: Delete `getCaptcha` function (lines 242-244)**

```js
export function getCaptcha() {
  return request('GET', '/captcha/get')
}
```

Delete these 3 lines.

- [ ] **Step 2: Delete `checkCaptcha` function (lines 246-248)**

```js
export function checkCaptcha(payload) {
  return request('POST', '/captcha/check', payload)
}
```

Delete these 3 lines.

- [ ] **Step 3: Check for remaining references** — Run a grep to confirm no other file imports `getCaptcha` or `checkCaptcha` from `auth.js`:

```bash
grep -r "getCaptcha\|checkCaptcha" src/main/resources/static/spotify-frontend/src/
```

If only the deleted AuthPage files reference them, we're clean. If any other file references them, note that file for a separate fix.

---

## Task 12: Delete `CaptchaModal.vue`

**Files:**
- Delete: `src/main/resources/static/spotify-frontend/src/components/CaptchaModal.vue`

Only after Tasks 8-10 are complete and verified.

- [ ] **Step 1: Delete the file**

```bash
rm src/main/resources/static/spotify-frontend/src/components/CaptchaModal.vue
```

---

## Task 13: Verification

**Prerequisite:** User must start the backend server (requires JDK). If frontend is running in dev mode (`npm run dev` on port 8000), restart it.

- [ ] **Step 1: Backend — Start the server and check logs**

Look for:
- `supported-captchaCache-service:{redis, local}` in startup logs (confirms Redis impl was found)
- No errors related to captcha bean creation

- [ ] **Step 2: Backend — Test `/captcha/get` endpoint**

```bash
curl http://localhost:5000/captcha/get
```

Expected: JSON response with captcha data (background image base64, slider image base64, token/captchaId).

- [ ] **Step 3: Backend — Check Redis for stored captcha data**

Connect to Redis and check for captcha keys:
```bash
redis-cli -h 192.168.100.128 keys "aj*"
```

Expected: One or more keys with prefix matching AJ-Captcha's internal format, with TTL < 120s.

- [ ] **Step 4: Frontend — Open login page and test Verify component**

Navigate to `http://localhost:5000/` (or `http://localhost:8000/` in dev mode). Click the verification button. The `<Verify>` component should:
1. Render the slider puzzle
2. Allow drag-to-complete
3. On success, close and populate `captchaVerificationToken`

- [ ] **Step 5: Frontend — Test signup with captcha**

1. Fill in registration form
2. Complete slider verification
3. Submit

Expected: Signup succeeds (or fails with "email already exists"), NOT with "Captcha verification failed".

- [ ] **Step 6: Frontend — Test reset password with captcha**

Same flow but using the reset password form.

- [ ] **Step 7: Test use-and-burn (captchaVerification reuse)**

Use the browser's network tab to capture a `captchaVerification` value from a successful verification. Then try calling the backend directly:

```bash
curl -X POST http://localhost:5000/spotify/signup \
  -H "Content-Type: application/json" \
  -d '{"token":"<captured-captchaVerification>","email":"test@test.com",...}'
```

Repeat with the same `captchaVerification`. The second request should fail with "Captcha verification failed".

- [ ] **Step 8: Check `CaptchaModal.vue` references are clean**

```bash
grep -r "CaptchaModal" src/main/resources/static/spotify-frontend/src/
```

Expected: No results (or only in backup directories, which are fine).

---

## Task Order Dependency Summary

```
Task 1 (CaptchaCacheServiceRedisImpl)
  ↓
Task 2 (CaptchaConfig) ← depends on Task 1
  ↓
Task 3, 4, 5 (deletes + CaptchaVerifier) ← can run parallel, no inter-dependency
  ↓
Task 6 (index.html CDN script)
  ↓
Task 7 (CaptchaVerify.vue) ← can run parallel with Task 6 if CDN URL is known
  ↓
Tasks 8, 9, 10 (AuthPage*.vue) ← can run in parallel
  ↓
Task 11 (auth.js cleanup)
  ↓
Task 12 (delete CaptchaModal.vue) ← only after 8-10 verified
  ↓
Task 13 (verification)
```
