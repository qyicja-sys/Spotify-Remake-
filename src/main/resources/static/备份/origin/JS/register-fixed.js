const { createApp } = Vue;

function getLoginUrl() {
    const { hostname, port } = window.location;

    if ((hostname === 'localhost' || hostname === '127.0.0.1') && port === '90') {
        return '/api/login';
    }

    return 'http://localhost:8080/login';
}

function getSignUpUrl() {
    const { hostname, port } = window.location;

    if ((hostname === 'localhost' || hostname === '127.0.0.1') && port === '90') {
        return '/api/signup';
    }

    return 'http://localhost:8080/signup';
}

function getForgetPasswordUrl() {
    const { hostname, port } = window.location;

    if ((hostname === 'localhost' || hostname === '127.0.0.1') && port === '90') {
        return '/api/login/forgetPassword';
    }

    return 'http://localhost:8080/login/forgetPassword';
}

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
        };
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
            this.clearLoginError();
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
            if (this.newPassword.length < 6) {
                this.loginErrorMessage = 'Password must be at least 6 characters';
                return;
            }
            
            axios.post(getForgetPasswordUrl(), {
                email: this.resetEmail,
                NewPassword: this.newPassword
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
                alert('Please enter your email first');
                return;
            }
        },
        clearLoginError() {
            this.loginErrorMessage = '';
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
                password: this.password
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
            console.log('Mode switched to:', this.isSignUpMode ? 'Sign up' : 'Log in');
        },
        handleSignUpSubmit() {
            console.log('Sign up attempt:', {
                email: this.email,
                username: this.username,
                nickname: this.nickname,
                password: this.password,
                captcha: this.captcha,
            });
            if (!this.email || !this.username || !this.nickname || !this.password || !this.captcha) {
                this.loginErrorMessage = 'Please complete all required fields';
                return;
            }
            if (!this.generatedCode) {
                this.loginErrorMessage = 'Please request a verification code first';
                return;
            }
            if (this.captcha !== this.generatedCode) {
                this.loginErrorMessage = 'Invalid verification code';
                return;
            }
            
            axios.post(getSignUpUrl(), {
                email: this.email,
                userName: this.username,
                nickName: this.nickname,
                password: this.password
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
                this.captcha = '';
                this.generatedCode = '';
                this.isSignUpMode = false;
            })
            .catch(error => {
                console.error('Sign up error:', error);
                if (error.response && error.response.data) {
                    const { code, message } = error.response.data;
                    this.loginErrorMessage = message || 'Sign up failed';
                } else {
                    this.loginErrorMessage = 'Network error, please try again';
                }
            });
        },
        handleRequestCode() {
            console.log('Request Code clicked');
            if (!this.email) {
                alert('Please enter your email first');
                return;
            }
        },
        handleJoinNow() {
            console.log('Join Now clicked');
            this.isSignUpMode = true;
        },
        handleLanguageChange() {
            const langMap = {
                en: 'English',
                zh: '中文',
                ja: '日本語',
                ko: '한국어',
                es: 'Español',
                fr: 'Français',
                de: 'Deutsch',
                pt: 'Português',
            };
            console.log('Language changed to:', langMap[this.selectedLanguage]);
        },
    },
    mounted() {
        console.log('Spotify register page mounted');
    },
}).mount('#app');