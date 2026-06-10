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