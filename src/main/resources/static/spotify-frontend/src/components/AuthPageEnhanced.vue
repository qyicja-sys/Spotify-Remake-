<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import spotifyLogo from '../assets/spotify_242118.svg'
import playImage from '../assets/鎾斁.png'
import forwardImage from '../assets/蹇繘.png'
import musicImage from '../assets/音乐.png'
import CaptchaModal from './CaptchaModal.vue'
import { login, resetPassword, signUp } from '../api/auth'

const languageOptions = [
  { code: 'en', label: 'English', nativeLabel: 'English', region: 'Global' },
  { code: 'zh', label: 'Chinese', nativeLabel: '简体中文', region: 'China' },
  { code: 'ja', label: 'Japanese', nativeLabel: '日本語', region: 'Japan' },
  { code: 'ko', label: 'Korean', nativeLabel: '한국어', region: 'Korea' },
  { code: 'es', label: 'Spanish', nativeLabel: 'Español', region: 'Spain' },
  { code: 'fr', label: 'French', nativeLabel: 'Français', region: 'France' },
  { code: 'de', label: 'German', nativeLabel: 'Deutsch', region: 'Germany' },
  { code: 'pt', label: 'Portuguese', nativeLabel: 'Português', region: 'Portugal' },
]

const copyByLanguage = {
  en: {
    welcome: 'Welcome!',
    createAccount: 'Create Account',
    resetPassword: 'Reset Password',
    loginSubtitle: 'Log in to Spotify to continue to Spotify.',
    signupSubtitle: 'Sign up to get started with Spotify.',
    resetSubtitle: 'Enter your email and new password to reset your account.',
    defineMusic: 'Define The True Music',
    tryIt: 'Now Try It',
    users: 'users.',
    aiGraphics: 'AI generated graphics.',
    joinNow: 'Join Now',
  },
  zh: {
    welcome: '欢迎回来',
    createAccount: '创建账号',
    resetPassword: '重置密码',
    loginSubtitle: '登录 Spotify，继续你的音乐旅程。',
    signupSubtitle: '注册账号，开始使用 Spotify。',
    resetSubtitle: '输入邮箱和新密码来重置账号。',
    defineMusic: '定义真正的音乐',
    tryIt: '现在开始体验',
    users: '位用户。',
    aiGraphics: 'AI 生成图形。',
    joinNow: '立即加入',
  },
  ja: {
    welcome: 'ようこそ',
    createAccount: 'アカウントを作成',
    resetPassword: 'パスワードを再設定',
    loginSubtitle: 'Spotify にログインして続行します。',
    signupSubtitle: 'Spotify を始めるために登録しましょう。',
    resetSubtitle: 'メールアドレスと新しいパスワードを入力してください。',
    defineMusic: '本当の音楽を定義する',
    tryIt: '今すぐ試す',
    users: '人のユーザー。',
    aiGraphics: 'AI 生成グラフィック。',
    joinNow: '今すぐ参加',
  },
  ko: {
    welcome: '환영합니다',
    createAccount: '계정 만들기',
    resetPassword: '비밀번호 재설정',
    loginSubtitle: 'Spotify에 로그인하고 계속하세요.',
    signupSubtitle: 'Spotify를 시작하려면 가입하세요.',
    resetSubtitle: '이메일과 새 비밀번호를 입력하세요.',
    defineMusic: '진짜 음악을 정의하다',
    tryIt: '지금 시작하기',
    users: '명의 사용자.',
    aiGraphics: 'AI 생성 그래픽.',
    joinNow: '지금 참여',
  },
}

const email = ref('')
const password = ref('')
const username = ref('')
const nickname = ref('')
const loginErrorMessage = ref('')
const showPassword = ref(false)
const isSignUpMode = ref(false)
const isForgotPasswordMode = ref(false)
const resetEmail = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)
const selectedLanguage = ref('en')
const languageMenuOpen = ref(false)
const languagePanelRef = ref(null)

const captchaVerificationToken = ref('')
const captchaVerified = ref(false)
const captchaVerifiedScene = ref('')
const captchaVerifyVisible = ref(false)
const captchaScene = ref('signup')

const signUpVerified = computed(
  () => captchaVerificationToken.value && captchaVerifiedScene.value === 'signup',
)
const resetVerified = computed(
  () => captchaVerificationToken.value && captchaVerifiedScene.value === 'reset',
)
const currentLanguage = computed(
  () => languageOptions.find(option => option.code === selectedLanguage.value) || languageOptions[0],
)
const uiCopy = computed(() => copyByLanguage[selectedLanguage.value] || copyByLanguage.en)

function clearLoginError() {
  loginErrorMessage.value = ''
}

function resetCaptchaState() {
  captchaVerificationToken.value = ''
  captchaVerified.value = false
  captchaVerifyVisible.value = false
  captchaScene.value = 'signup'
}

function togglePasswordVisibility() {
  showPassword.value = !showPassword.value
}

function toggleNewPasswordVisibility() {
  showNewPassword.value = !showNewPassword.value
}

function toggleConfirmPasswordVisibility() {
  showConfirmPassword.value = !showConfirmPassword.value
}

function handleGoogleLogin() {
  window.alert('Google login demo')
}

function handleAppleLogin() {
  window.alert('Apple login demo')
}

function handleForgotPassword() {
  isForgotPasswordMode.value = true
  resetCaptchaState()
  clearLoginError()
}

function handleBackToLogin() {
  isForgotPasswordMode.value = false
  resetEmail.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
  resetCaptchaState()
  clearLoginError()
}

async function handleLogin() {
  clearLoginError()
  if (!email.value || !password.value) {
    loginErrorMessage.value = 'Please enter your email and password'
    return
  }

  try {
    const response = await login({
      email: email.value,
      password: password.value,
    })
    const { code, message } = response.data
    if (code === 400 || message === 'email or password is wrong') {
      loginErrorMessage.value = message || 'email or password is wrong'
      return
    }
    window.alert(message)
  } catch (error) {
    if (error.response?.data) {
      const { code, message } = error.response.data
      if (code === 400 || message === 'email or password is wrong') {
        loginErrorMessage.value = message || 'email or password is wrong'
        return
      }
      loginErrorMessage.value = message || 'Login failed'
      return
    }
    loginErrorMessage.value = error.message || 'Network error, please try again'
  }
}

function toggleMode() {
  isSignUpMode.value = !isSignUpMode.value
  clearLoginError()
  resetCaptchaState()
}

async function handleSignUpSubmit() {
  clearLoginError()
  if (!email.value || !username.value || !nickname.value || !password.value) {
    loginErrorMessage.value = 'Please complete all required fields'
    return
  }
  if (!captchaVerificationToken.value) {
    loginErrorMessage.value = 'Please complete the slider verification first'
    return
  }

  try {
    const response = await signUp({
      email: email.value,
      userName: username.value,
      nickName: nickname.value,
      password: password.value,
      token: captchaVerificationToken.value,
    })
    const { code, message } = response.data
    if (code === 400) {
      loginErrorMessage.value = message || 'Sign up failed'
      return
    }
    window.alert(message || 'Sign up successful')
    email.value = ''
    username.value = ''
    nickname.value = ''
    password.value = ''
    resetCaptchaState()
    isSignUpMode.value = false
  } catch (error) {
    if (error.response?.data) {
      const { message } = error.response.data
      loginErrorMessage.value = message || 'Sign up failed'
      return
    }
    loginErrorMessage.value = error.message || 'Network error, please try again'
  }
}

async function handleResetPassword() {
  clearLoginError()
  if (!resetEmail.value || !newPassword.value) {
    loginErrorMessage.value = 'Please complete all required fields'
    return
  }
  if (!captchaVerificationToken.value) {
    loginErrorMessage.value = 'Please complete the slider verification first'
    return
  }
  if (newPassword.value.length < 6) {
    loginErrorMessage.value = 'Password must be at least 6 characters'
    return
  }

  try {
    const response = await resetPassword({
      email: resetEmail.value,
      NewPassword: newPassword.value,
      token: captchaVerificationToken.value,
    })
    const { code, message } = response.data
    if (code === 400) {
      loginErrorMessage.value = message || 'Reset password failed'
      return
    }
    loginErrorMessage.value = message || 'Password reset successful!'
    window.setTimeout(() => {
      handleBackToLogin()
    }, 3000)
  } catch (error) {
    if (error.response?.data) {
      const { code, message } = error.response.data
      if (code === 400) {
        loginErrorMessage.value = message || 'Reset password failed'
        return
      }
      loginErrorMessage.value = message || 'Reset password failed'
      return
    }
    loginErrorMessage.value = error.message || 'Network error, please try again'
  }
}

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

function handleJoinNow() {
  isSignUpMode.value = true
}

function toggleLanguageMenu() {
  languageMenuOpen.value = !languageMenuOpen.value
}

function selectLanguage(code) {
  selectedLanguage.value = code
  languageMenuOpen.value = false
}

function handleDocumentClick(event) {
  if (!languagePanelRef.value?.contains(event.target)) {
    languageMenuOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
})
</script>

<template>
  <div class="auth-page" v-cloak>
    <div class="left-panel">
      <div class="logo-area">
        <div class="logo-text">
          <img class="logo-icon" :src="spotifyLogo" alt="Logo" />
          Spotify
        </div>
      </div>

      <h1 class="welcome-title">
        {{ isForgotPasswordMode ? uiCopy.resetPassword : (isSignUpMode ? uiCopy.createAccount : uiCopy.welcome) }}
      </h1>
      <p class="welcome-subtitle">
        {{ isForgotPasswordMode ? uiCopy.resetSubtitle : (isSignUpMode ? uiCopy.signupSubtitle : uiCopy.loginSubtitle) }}
      </p>

      <div class="social-buttons">
        <button class="btn-social btn-google" @click="handleGoogleLogin">
          <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
            <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
            <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
            <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
          </svg>
          Login with Google
        </button>
        <button class="btn-social btn-apple" @click="handleAppleLogin">
          <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M17.05 20.28c-.98.95-2.05.88-3.08.4-1.09-.5-2.08-.48-3.24 0-1.44.62-2.2.44-3.06-.4C2.79 15.25 3.51 7.59 9.05 7.31c1.35.07 2.29.74 3.08.8 1.18-.24 2.31-.93 3.57-.84 1.51.12 2.65.72 3.4 1.8-3.12 1.87-2.38 5.98.48 7.13-.57 1.5-1.31 2.99-2.54 4.09zM12.03 7.25c-.15-2.23 1.66-4.07 3.74-4.25.29 2.58-2.34 4.5-3.74 4.25z" fill="#000" />
          </svg>
          Login with Apple
        </button>
      </div>

      <div class="divider">OR</div>

      <div v-if="!isForgotPasswordMode">
        <div class="form-group">
          <div class="form-label-row">
            <label class="form-label" for="email">Email</label>
            <span v-if="loginErrorMessage" class="form-error-text">{{ loginErrorMessage }}</span>
          </div>
          <div class="input-wrapper">
            <input id="email" v-model="email" type="email" class="form-input" placeholder="Your email address" autocomplete="email" @input="clearLoginError" />
          </div>
        </div>

        <div v-if="isSignUpMode" class="form-group">
          <label class="form-label" for="username">Username</label>
          <div class="input-wrapper">
            <input id="username" v-model="username" type="text" class="form-input" placeholder="Choose a username" autocomplete="username" />
          </div>
        </div>

        <div v-if="isSignUpMode" class="form-group">
          <label class="form-label" for="nickname">Nickname</label>
          <div class="input-wrapper">
            <input id="nickname" v-model="nickname" type="text" class="form-input" placeholder="Choose a nickname" autocomplete="nickname" />
          </div>
        </div>

        <div class="form-group">
          <label class="form-label" for="password">Password</label>
          <div class="input-wrapper password-wrapper">
            <input id="password" v-model="password" :type="showPassword ? 'text' : 'password'" class="form-input password-input" :placeholder="isSignUpMode ? 'Create a password' : 'Your password'" :autocomplete="isSignUpMode ? 'new-password' : 'current-password'" @input="clearLoginError" />
            <button type="button" class="toggle-password" :aria-label="showPassword ? 'Hide password' : 'Show password'" :title="showPassword ? 'Hide password' : 'Show password'" @click="togglePasswordVisibility">
              <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
            </button>
          </div>
        </div>

        <div v-if="isSignUpMode" class="form-group">
          <div class="captcha-action-row">
            <div class="captcha-action-copy">
              <span class="captcha-action-label">Verification</span>
              <span class="captcha-action-state" :class="{ verified: signUpVerified }">
                {{ signUpVerified ? 'Slider verified' : 'Complete the slider check before sign up' }}
              </span>
            </div>
            <button type="button" class="btn-request-code" @click="handleRequestCode">
              Request Code
            </button>
          </div>
        </div>

        <div class="btn-row-right">
          <button v-if="!isSignUpMode" class="btn-forgot" @click="handleForgotPassword">Forgot password?</button>
        </div>

        <button class="btn-login" @click="isSignUpMode ? handleSignUpSubmit() : handleLogin()">
          {{ isSignUpMode ? 'Sign up' : 'Log in' }}
        </button>
      </div>

      <div v-else>
        <div class="form-group">
          <div class="form-label-row">
            <label class="form-label" for="reset-email">Email</label>
            <span v-if="loginErrorMessage" class="form-error-text">{{ loginErrorMessage }}</span>
          </div>
          <div class="input-wrapper">
            <input id="reset-email" v-model="resetEmail" type="email" class="form-input" placeholder="Your email address" autocomplete="email" @input="clearLoginError" />
          </div>
        </div>

        <div class="form-group">
          <label class="form-label" for="new-password">New password</label>
          <div class="input-wrapper password-wrapper">
            <input id="new-password" v-model="newPassword" :type="showNewPassword ? 'text' : 'password'" class="form-input password-input" placeholder="Create a new password" autocomplete="new-password" @input="clearLoginError" />
            <button type="button" class="toggle-password" :aria-label="showNewPassword ? 'Hide password' : 'Show password'" :title="showNewPassword ? 'Hide password' : 'Show password'" @click="toggleNewPasswordVisibility">
              <svg v-if="!showNewPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
            </button>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label" for="confirm-password">Confirm the password</label>
          <div class="input-wrapper password-wrapper">
            <input id="confirm-password" v-model="confirmPassword" :type="showConfirmPassword ? 'text' : 'password'" class="form-input password-input" placeholder="Confirm your new password" autocomplete="new-password" @input="clearLoginError" />
            <button type="button" class="toggle-password" :aria-label="showConfirmPassword ? 'Hide password' : 'Show password'" :title="showConfirmPassword ? 'Hide password' : 'Show password'" @click="toggleConfirmPasswordVisibility">
              <svg v-if="!showConfirmPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
            </button>
          </div>
        </div>

        <div class="form-group">
          <div class="captcha-action-row">
            <div class="captcha-action-copy">
              <span class="captcha-action-label">Verification</span>
              <span class="captcha-action-state" :class="{ verified: resetVerified }">
                {{ resetVerified ? 'Slider verified' : 'Complete the slider check before resetting password' }}
              </span>
            </div>
            <button type="button" class="btn-request-code" @click="handleRequestResetCode">
              Request Code
            </button>
          </div>
        </div>

        <div class="btn-row-right">
          <button class="btn-forgot" @click="handleBackToLogin">Back to Login</button>
        </div>

        <button class="btn-login" @click="handleResetPassword">Reset Password</button>
      </div>

      <div v-if="!isForgotPasswordMode" class="signup-area">
        <span>{{ isSignUpMode ? 'Already have an account?' : "Don't have an account?" }}</span>
        <button type="button" class="btn-signup" @click="toggleMode">
          {{ isSignUpMode ? 'Log in' : 'Sign up' }}
        </button>
      </div>
    </div>

    <div class="right-panel">
      <div ref="languagePanelRef" class="language-selector">
        <div class="language-panel" :class="{ open: languageMenuOpen }">
          <button type="button" class="language-trigger" :aria-expanded="languageMenuOpen" aria-haspopup="listbox" @click="toggleLanguageMenu">
            <span class="language-trigger-copy">
              <span class="language-trigger-label">Language</span>
              <span class="language-trigger-value">{{ currentLanguage.nativeLabel }}</span>
            </span>
            <span class="language-trigger-meta">{{ currentLanguage.region }}</span>
            <span class="language-trigger-icon">{{ languageMenuOpen ? '-' : '+' }}</span>
          </button>

          <div v-if="languageMenuOpen" class="language-menu" role="listbox" aria-label="Language options">
            <button
              v-for="option in languageOptions"
              :key="option.code"
              type="button"
              class="language-option"
              :class="{ active: option.code === selectedLanguage }"
              :aria-selected="option.code === selectedLanguage"
              @click="selectLanguage(option.code)"
            >
              <span class="language-option-copy">
                <span class="language-option-native">{{ option.nativeLabel }}</span>
                <span class="language-option-label">{{ option.label }}</span>
              </span>
              <span class="language-option-region">{{ option.region }}</span>
            </button>
          </div>
        </div>
      </div>

      <div class="glow-aura"></div>

      <div class="scene-container">
        <div class="image-wrapper image-top">
          <img class="floating-image" :src="playImage" alt="Play" />
        </div>
        <div class="image-wrapper image-left">
          <img class="floating-image" :src="forwardImage" alt="Forward" />
        </div>
        <div class="image-wrapper image-right">
          <img class="floating-image" :src="musicImage" alt="Music" />
        </div>
        <h2 class="secondary-title">{{ uiCopy.defineMusic }}</h2>
        <h3 class="tertiary-title">{{ uiCopy.tryIt }}</h3>
      </div>

      <div class="bottom-info">
        <p class="stats-text slide-up-text slide-up-text-delay-1">
          <span class="highlight">400K+</span> {{ uiCopy.users }}
          <span class="highlight">50M+</span> {{ uiCopy.aiGraphics }}
        </p>
        <button type="button" class="btn-join slide-up-text slide-up-text-delay-2" @click="handleJoinNow">
          {{ uiCopy.joinNow }}
        </button>
      </div>
    </div>

    <CaptchaModal
      :visible="captchaVerifyVisible"
      @close="captchaVerifyVisible = false"
      @success="onCaptchaSuccess"
      @error="onCaptchaError"
    />
  </div>
</template>
