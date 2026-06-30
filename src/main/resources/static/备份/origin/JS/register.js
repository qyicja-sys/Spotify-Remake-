﻿﻿﻿﻿﻿const { createApp } = Vue;

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
            selectedLanguage: 'en',
            isForgotPasswordMode: false,
            resetEmail: '',
            newPassword: '',
            confirmPassword: '',
            resetCaptcha: '',
            showNewPassword: false,
            showConfirmPassword: false,
            generatedResetCode: '',
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
        clearLoginError() {
            this.loginErrorMessage = '';
        },
        handleLogin() {
            console.log('Login attempt:', { email: this.email, password: this.password });
            this.clearLoginError();
            if (!this.email || !this.password) {
                alert('Please enter your email and password');
                return;
            }
            
            axios.post('/login', {
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
                    alert(message || 'Login failed');
                } else {
                    alert('Network error, please try again');
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
                alert('Please complete all required fields');
                return;
            }
            if (!this.generatedCode) {
                alert('Please request a verification code first');
                return;
            }
            if (this.captcha !== this.generatedCode) {
                alert('Invalid verification code');
                return;
            }
            alert(`Sign up demo\nEmail: ${this.email}\nUsername: ${this.username}\nNickname: ${this.nickname}`);
        },
        handleRequestCode() {
            console.log('Request Code clicked');
            if (!this.email) {
                alert('Please enter your email first');
                return;
            }
            const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
            let code = '';
            for (let i = 0; i < 5; i++) {
                const randomIndex = Math.floor(Math.random() * chars.length);
                code += chars[randomIndex];
            }
            this.generatedCode = code;
            alert(`Your verification code is: ${code}`);
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
        toggleNewPasswordVisibility() {
            this.showNewPassword = !this.showNewPassword;
        },
        toggleConfirmPasswordVisibility() {
            this.showConfirmPassword = !this.showConfirmPassword;
        },
        handleBackToLogin() {
            console.log('Back to Login clicked');
            this.isForgotPasswordMode = false;
            this.clearLoginError();
            this.resetEmail = '';
            this.newPassword = '';
            this.confirmPassword = '';
            this.resetCaptcha = '';
            this.generatedResetCode = '';
        },
        handleRequestResetCode() {
            console.log('Request Reset Code clicked');
            if (!this.resetEmail) {
                alert('Please enter your email first');
                return;
            }
            const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
            let code = '';
            for (let i = 0; i < 5; i++) {
                const randomIndex = Math.floor(Math.random() * chars.length);
                code += chars[randomIndex];
            }
            this.generatedResetCode = code;
            alert(`Your verification code is: ${code}`);
        },
        handleResetPassword() {
            console.log('Reset Password attempt:', { email: this.resetEmail, password: this.newPassword });
            this.clearLoginError();
            if (!this.resetEmail || !this.newPassword) {
                alert('Please enter your email and new password');
                return;
            }
            if (!this.generatedResetCode) {
                alert('Please request a verification code first');
                return;
            }
            if (this.resetCaptcha !== this.generatedResetCode) {
                alert('Invalid verification code');
                return;
            }
            if (this.newPassword !== this.confirmPassword) {
                alert('Passwords do not match');
                return;
            }
            
            axios.post('/login/forgetPassword', {
                email: this.resetEmail,
                password: this.newPassword
            })
            .then(response => {
                console.log('Reset Password response:', response.data);
                const { code, message } = response.data;
                if (code === 400) {
                    this.loginErrorMessage = message || 'Reset password failed';
                    return;
                }
                alert(message || 'Password reset successfully');
                this.handleBackToLogin();
            })
            .catch(error => {
                console.error('Reset Password error:', error);
                if (error.response && error.response.data) {
                    const { code, message } = error.response.data;
                    if (code === 400) {
                        this.loginErrorMessage = message || 'Reset password failed';
                        return;
                    }
                    alert(message || 'Reset password failed');
                } else {
                    alert('Network error, please try again');
                }
            });
        },
    },
    mounted() {
        console.log('Spotify register page mounted');
    },
}).mount('#app');