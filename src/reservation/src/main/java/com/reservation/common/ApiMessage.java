package com.reservation.common;

import lombok.Getter;

@Getter
public enum ApiMessage {

    UNAUTHORIZED(
            "Unauthorized",
            "Authentication is required to access this resource. Please provide a valid token.",
            "عدم احراز هویت",
            "برای دسترسی به این بخش باید وارد حساب کاربری خود شوید و یا توکن شما منقضی شده است.",
            401
    ),
    ACCESS_DENIED(
            "Access Denied",
            "You don't have access to this part.",
            "عدم دسترسی",
            "شما دسترسی به این بخش ندارید.",
            403
    ),
    RESOURCE_NOT_FOUND(
            "Not Found",
            "The requested endpoint or resource does not exist",
            "یافت نشد",
            "مسیر یا منبع درخواستی یافت نشد",
            404
    ),
    VALIDATION_FAILED(
            "Validation failed",
            null,
            "خطای اعتبار سنجی",
            null,
            400 // Bad Request
    ),
    INTERNAL_SERVER_ERROR(
            "internal server error happened",
            "it's not your fault",
            "خطای داخلی سرور",
            "خطای غیرمنتظره‌ای در سرور رخ داده است. لطفاً دقایقی دیگر تلاش کنید.",
            500 // Internal Server Error
    ),

    TEMP(
            null,
            null,
            null,
            null,
            200
    );
    // ================================================

    private final String title;
    private final String message;
    private final String titleFa;
    private final String messageFa;
    private final int statusCode;

    ApiMessage(String title, String message, String titleFa, String messageFa, int statusCode) {
        this.title = title;
        this.message = message;
        this.titleFa = titleFa;
        this.messageFa = messageFa;
        this.statusCode = statusCode;
    }
}