const { createApp } = Vue;

function isProxyMode() {
    const { hostname, port } = window.location;
    return (hostname === 'localhost' || hostname === '127.0.0.1') && port === '90';
}

function getLoginUrl() {
    return isProxyMode() ? '/api/login' : 'http://localhost:8080/login';
}

function getSignUpUrl() {
    return isProxyMode() ? '/api/signup' : 'http://localhost:8080/signup';
}

function getForgetPasswordUrl() {
    return isProxyMode() ? '/api/login/forgetPassword' : 'http://localhost:8080/login/forgetPassword';
}

function getCaptchaGetUrl() {
    return isProxyMode() ? '/api/captcha/get' : 'http://localhost:8080/captcha/get';
}

function getCaptchaCheckUrl() {
    return isProxyMode() ? '/api/captcha/check' : 'http://localhost:8080/captcha/check';
}

const CAPTCHA_IMAGE_WIDTH = 310;
const CAPTCHA_SLIDER_SIZE = 45;
const CAPTCHA_SLIDER_TOP = 40;
const CAPTCHA_MAX_OFFSET = CAPTCHA_IMAGE_WIDTH - CAPTCHA_SLIDER_SIZE;
const CAPTCHA_TRACK_BUTTON_SIZE = 48;

createApp({
    data() {
        return {
            email: '',
            password: '',
            username: '',
            nickname: '',
            captcha: '',
            loginErrorMessage: '',
            showPassword: false,
            isSignUpMode: false,
            isForgotPasswordMode: false,
            resetEmail: '',
            newPassword: '',
            confirmPassword: '',
            resetCaptcha: '',
            showNewPassword: false,
            showConfirmPassword: false,
            selectedLanguage: 'en',
            captchaVerificationToken: '',
            captchaModalVisible: false,
            captchaLoading: false,
            captchaChecking: false,
            captchaVerified: false,
            captchaBackgroundImage: '',
            captchaSliderImage: '',
            captchaToken: '',
            captchaHintText: 'Click "Request Code" to load the slider captcha.',
            captchaScene: 'signup',
            sliderLeft: 0,
            sliderButtonLeft: 0,
            isDraggingSlider: false,
        };
    },
    computed: {
        sliderTop() {
            return CAPTCHA_SLIDER_TOP;
        },
        sliderProgressWidth() {
            return this.sliderButtonLeft + CAPTCHA_TRACK_BUTTON_SIZE;
        },
    },
    methods: {
        togglePasswordVisibility() {
            this.showPassword = !this.showPassword;
        },
        handleGoogleLogin() {
            console.log('Google login clicked');
            alert('Google login demo');
        },
        handleAppleLogin() {
            console.log('Apple login clicked');
            alert('Apple login demo');
        },
        handleForgotPassword() {
            console.log('Forgot password clicked');
            this.isForgotPasswordMode = true;
            this.resetCaptchaState();
            this.clearLoginError();
        },
        toggleNewPasswordVisibility() {
            this.showNewPassword = !this.showNewPassword;
        },
        toggleConfirmPasswordVisibility() {
            this.showConfirmPassword = !this.showConfirmPassword;
        },
        handleBackToLogin() {
            this.isForgotPasswordMode = false;
            this.resetEmail = '';
            this.newPassword = '';
            this.confirmPassword = '';
            this.resetCaptcha = '';
            this.resetCaptchaState();
            this.clearLoginError();
        },
        resetCaptchaState() {
            this.captcha = '';
            this.resetCaptcha = '';
            this.captchaVerificationToken = '';
            this.captchaToken = '';
            this.captchaVerified = false;
            this.captchaBackgroundImage = '';
            this.captchaSliderImage = '';
            this.captchaModalVisible = false;
            this.captchaScene = 'signup';
            this.captchaChecking = false;
            this.sliderLeft = 0;
            this.sliderButtonLeft = 0;
            this.isDraggingSlider = false;
            this.captchaHintText = 'Click "Request Code" to load the slider captcha.';
        },
        clearLoginError() {
            this.loginErrorMessage = '';
        },
        handleResetPassword() {
            console.log('Reset password attempt:', {
                email: this.resetEmail,
                password: this.newPassword,
            });
            this.clearLoginError();
            if (!this.resetEmail || !this.newPassword) {
                this.loginErrorMessage = 'Please complete all required fields';
                return;
            }
            if (!this.captchaVerificationToken) {
                this.loginErrorMessage = 'Please complete the slider verification first';
                return;
            }
            if (this.newPassword.length < 6) {
                this.loginErrorMessage = 'Password must be at least 6 characters';
                return;
            }

            axios.post(getForgetPasswordUrl(), {
                email: this.resetEmail,
                NewPassword: this.newPassword,
                token: this.captchaVerificationToken,
            })
            .then(response => {
                const { code, message } = response.data;
                if (code === 400) {
                    this.loginErrorMessage = message || 'Reset password failed';
                    return;
                }
                this.loginErrorMessage = message || 'Password reset successful!';
                setTimeout(() => {
                    this.handleBackToLogin();
                }, 3000);
            })
            .catch(error => {
                console.error('Reset Password error:', error);
                if (error.response && error.response.data) {
                    const { code, message } = error.response.data;
                    if (code === 400) {
                        this.loginErrorMessage = message || 'Reset password failed';
                        return;
                    }
                    this.loginErrorMessage = message || 'Reset password failed';
                } else {
                    this.loginErrorMessage = 'Network error, please try again';
                }
            });
        },
        handleRequestResetCode() {
            console.log('Request Reset Code clicked');
            if (!this.resetEmail) {
                this.loginErrorMessage = 'Please enter your email first';
                return;
            }
            this.clearLoginError();
            this.fetchCaptcha('reset');
        },
        handleLogin() {
            console.log('Login attempt:', { email: this.email, password: this.password });
            this.clearLoginError();
            if (!this.email || !this.password) {
                this.loginErrorMessage = 'Please enter your email and password';
                return;
            }

            axios.post(getLoginUrl(), {
                email: this.email,
                password: this.password,
            })
            .then(response => {
                console.log('Login response:', response.data);
                const { code, message } = response.data;
                if (code === 400 || message === 'email or password is wrong') {
                    this.loginErrorMessage = message || 'email or password is wrong';
                    return;
                }
                alert(message);
            })
            .catch(error => {
                console.error('Login error:', error);
                if (error.response && error.response.data) {
                    const { code, message } = error.response.data;
                    if (code === 400 || message === 'email or password is wrong') {
                        this.loginErrorMessage = message || 'email or password is wrong';
                        return;
                    }
                    this.loginErrorMessage = message || 'Login failed';
                } else {
                    this.loginErrorMessage = 'Network error, please try again';
                }
            });
        },
        toggleMode() {
            this.isSignUpMode = !this.isSignUpMode;
            this.clearLoginError();
            this.resetCaptchaState();
            console.log('Mode switched to:', this.isSignUpMode ? 'Sign up' : 'Log in');
        },
        handleSignUpSubmit() {
            console.log('Sign up attempt:', {
                email: this.email,
                username: this.username,
                nickname: this.nickname,
                password: this.password,
                token: this.captchaVerificationToken,
            });
            if (!this.email || !this.username || !this.nickname || !this.password) {
                this.loginErrorMessage = 'Please complete all required fields';
                return;
            }
            if (!this.captchaVerificationToken) {
                this.loginErrorMessage = 'Please complete the slider verification first';
                return;
            }

            axios.post(getSignUpUrl(), {
                email: this.email,
                userName: this.username,
                nickName: this.nickname,
                password: this.password,
                token: this.captchaVerificationToken,
            })
            .then(response => {
                console.log('Sign up response:', response.data);
                const { code, message } = response.data;
                if (code === 400) {
                    this.loginErrorMessage = message || 'Sign up failed';
                    return;
                }
                alert(message || 'Sign up successful');
                this.email = '';
                this.username = '';
                this.nickname = '';
                this.password = '';
                this.resetCaptchaState();
                this.isSignUpMode = false;
            })
            .catch(error => {
                console.error('Sign up error:', error);
                if (error.response && error.response.data) {
                    const { message } = error.response.data;
                    this.loginErrorMessage = message || 'Sign up failed';
                } else {
                    this.loginErrorMessage = 'Network error, please try again';
                }
            });
        },
        handleRequestCode() {
            console.log('Request Code clicked');
            if (!this.email) {
                this.loginErrorMessage = 'Please enter your email first';
                return;
            }
            this.clearLoginError();
            this.fetchCaptcha('signup');
        },
        fetchCaptcha(scene = 'signup') {
            this.captchaLoading = true;
            this.captchaChecking = false;
            this.captchaVerified = false;
            this.captchaVerificationToken = '';
            this.captchaScene = scene;
            if (scene === 'reset') {
                this.resetCaptcha = '';
            } else {
                this.captcha = '';
            }
            this.captchaHintText = 'Loading slider captcha...';
            this.sliderLeft = 0;
            this.sliderButtonLeft = 0;

            axios.get(getCaptchaGetUrl())
            .then(response => {
                const { backgroundImage, sliderImage, token, msg } = response.data || {};
                if (!backgroundImage || !sliderImage || !token) {
                    this.captchaHintText = msg || 'Failed to load captcha';
                    return;
                }

                this.captchaBackgroundImage = backgroundImage;
                this.captchaSliderImage = sliderImage;
                this.captchaToken = token;
                this.captchaModalVisible = true;
                this.captchaHintText = 'Drag the slider until the puzzle piece is aligned.';
            })
            .catch(error => {
                console.error('Fetch captcha error:', error);
                this.captchaHintText = 'Failed to load captcha, please try again.';
            })
            .finally(() => {
                this.captchaLoading = false;
            });
        },
        refreshCaptcha() {
            this.fetchCaptcha(this.captchaScene);
        },
        closeCaptchaModal() {
            this.captchaModalVisible = false;
            this.isDraggingSlider = false;
        },
        extractClientX(event) {
            if (event.touches && event.touches.length > 0) {
                return event.touches[0].clientX;
            }
            if (event.changedTouches && event.changedTouches.length > 0) {
                return event.changedTouches[0].clientX;
            }
            return event.clientX;
        },
        updateSliderPosition(clientX, currentTarget) {
            const track = currentTarget || document.querySelector('.captcha-drag-track');
            if (!track || typeof clientX !== 'number') {
                return;
            }

            const rect = track.getBoundingClientRect();
            const maxButtonLeft = rect.width - CAPTCHA_TRACK_BUTTON_SIZE;
            const rawButtonLeft = clientX - rect.left - (CAPTCHA_TRACK_BUTTON_SIZE / 2);
            const buttonLeft = Math.min(Math.max(rawButtonLeft, 0), maxButtonLeft);
            const ratio = maxButtonLeft > 0 ? buttonLeft / maxButtonLeft : 0;

            this.sliderButtonLeft = buttonLeft;
            this.sliderLeft = Math.round(ratio * CAPTCHA_MAX_OFFSET);
        },
        handleSliderStart(event) {
            if (this.captchaChecking || this.captchaVerified || !this.captchaToken) {
                return;
            }
            this.isDraggingSlider = true;
            this.captchaHintText = 'Release the slider when the puzzle piece is aligned.';
            this.updateSliderPosition(this.extractClientX(event), event.currentTarget.parentElement);
        },
        handleSliderMove(event) {
            if (!this.isDraggingSlider || this.captchaChecking || this.captchaVerified) {
                return;
            }
            this.updateSliderPosition(this.extractClientX(event), event.currentTarget);
        },
        handleSliderEnd() {
            if (!this.isDraggingSlider || this.captchaChecking || this.captchaVerified) {
                this.isDraggingSlider = false;
                return;
            }

            this.isDraggingSlider = false;
            this.captchaChecking = true;
            this.captchaHintText = 'Checking captcha...';

            axios.post(getCaptchaCheckUrl(), {
                token: this.captchaToken,
                userX: String(this.sliderLeft),
            })
            .then(response => {
                const { success, captchaVerification, msg } = response.data || {};
                if (!success) {
                    this.captchaHintText = msg || 'Verification failed, please try again.';
                    this.sliderLeft = 0;
                    this.sliderButtonLeft = 0;
                    return;
                }

                this.captchaVerified = true;
                this.captchaVerificationToken = captchaVerification;
                if (this.captchaScene === 'reset') {
                    this.resetCaptcha = 'Verified';
                } else {
                    this.captcha = 'Verified';
                }
                this.captchaHintText = msg || 'Verification successful';
                setTimeout(() => {
                    this.captchaModalVisible = false;
                }, 500);
            })
            .catch(error => {
                console.error('Check captcha error:', error);
                this.captchaHintText = 'Verification failed, please refresh and try again.';
                this.sliderLeft = 0;
                this.sliderButtonLeft = 0;
            })
            .finally(() => {
                this.captchaChecking = false;
            });
        },
        handleJoinNow() {
            console.log('Join Now clicked');
            this.isSignUpMode = true;
        },
        handleLanguageChange() {
            const langMap = {
                en: 'English',
                zh: 'Chinese',
                ja: 'Japanese',
                ko: 'Korean',
                es: 'Spanish',
                fr: 'French',
                de: 'German',
                pt: 'Portuguese',
            };
            console.log('Language changed to:', langMap[this.selectedLanguage]);
        },
    },
    mounted() {
        console.log('Spotify register page mounted');
    },
}).mount('#app');
