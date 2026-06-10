package com.backend.common;

import lombok.Getter;

@Getter
public enum ApiMessage {

    // Success messages
    SUCCESS_LOGIN(
            "Logged in successfully",
            null,
            "با موفقیت وارد شدید",
            null
    ),

    // Auth Errors
    BAD_CREDENTIALS(
            "Username or password is wrong",
            "Check your entries and try again",
            "نام کاربری یا رمز عبور اشتباه است",
            "ورودی های خود را بررسی و دوباره تلاش کنید"
    ),

    ACCOUNT_LOCKED(
            "Account is locked",
            "Try again later",
            "اکانت قفل شده است",
            "بعدا دوباره تلاش کنید"
    ),

    PASSWORD_EXPIRED(
            "Your password has expired",
            "Use password recovery to reset your password",
            "رمز عبور شما منقضی شده است",
            "از بازیابی رمز عبور برای ریست کردن رمز خود استفاده کنید"
    ),

    ACCOUNT_SUSPENDED(
            "Your account is suspended",
            null,
            "اکانت شما مسدود شده است",
            null
    ),



    // ======================================
    //           Validation Errors
    // ======================================
    VALIDATION_FAILED(
            "Validation failed",
            "Please clear the errors below",
            "خطای اعتبار سنجی",
            "لطفاً خطاهای زیر را برطرف کنید"
    ),
    // Login Validation Errors
    LOGIN_VALIDATION_FAILED(
            "Login failed",
            "Please clear the errors below",
            "وارد نشدید",
            "لطفاً خطاهای زیر را برطرف کنید"
    ),
    LOGIN_USERNAME_REQUIRED(
            "Username is required",
            "Username cannot be empty",
            "نام کاربری اجباری است",
            "نام کاربری نمی‌تواند خالی باشد"
    ),
    LOGIN_USERNAME_SIZE(
            "Invalid username length",
            "Username must be between 3 and 65 characters",
            "طول نام کاربری نامعتبر",
            "نام کاربری باید بین ۳ تا ۶۵ کاراکتر باشد"
    ),
    LOGIN_PASSWORD_REQUIRED(
            "Password is required",
            "Password cannot be empty",
            "رمز عبور اجباری است",
            "رمز عبور نمی‌تواند خالی باشد"
    ),
    LOGIN_PASSWORD_SIZE(
            "Invalid password length",
            "Password must be less than 32 characters",
            "طول رمز عبور نامعتبر",
            "رمز عبور باید کمتر از ۳۲ کاراکتر باشد"
    );

    private final String title;
    private final String message;
    private final String titleFa;
    private final String messageFa;

    ApiMessage(String title, String message, String titleFa, String messageFa) {
        this.title = title;
        this.message = message;
        this.titleFa = titleFa;
        this.messageFa = messageFa;
    }
}