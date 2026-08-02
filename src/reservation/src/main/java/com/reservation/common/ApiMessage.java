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





    RESERVED_SUCCESSFULLY(
            "Reservation submitted successfully",
            null,
            "رزرو با موفقیت انجام شد",
            null,
            200
    ),
    RESERVE_SEAT_IDS_EMPTY(
            "Seat IDs cannot be empty",
            "You must select at least one seat to proceed with the reservation.",
            "لیست صندلی‌ها نامعتبر است",
            "برای ثبت رزرو، انتخاب حداقل یک صندلی الزامی است.",
            400
    ),
    RESERVE_SEAT_IDS_SIZE(
            "Invalid seat selection count",
            "The number of selected seats must be between 1 and 15.",
            "تعداد صندلی‌های انتخابی نامعتبر است",
            "تعداد صندلی‌های درخواستی باید بین ۱ تا ۱۵ صندلی باشد.",
            400
    ),
    RESERVE_SEAT_IDS_REPEATED(
            "Duplicate seat IDs found",
            "The request contains duplicate seat IDs. Each seat can only be selected once.",
            "آیدی صندلی‌ها تکراری است",
            "در لیست انتخابی شما صندلی تکراری وجود دارد. هر صندلی فقط یک‌بار قابل انتخاب است.",
            400
    ),
    SEATS_NOT_EXIST(
            "Seat Not Exist",
            "One or more selected seats are not exist.",
            "صندلی وجود ندارد",
            "یک یا چند صندلی انتخابی وجود ندارند.",
            400
    ),
    SEATS_NOT_AVAILABLE(
            "Seat Already Reserved",
            "One or more selected seats have already been reserved.",
            "صندلی‌ها قبلاً رزرو شده‌اند",
            "یک یا چند صندلی انتخابی قبلاً رزرو شده‌اند.",
            400
    ),
    MATCH_NOT_AVAILABLE(
            "Match Not Available",
            "The match has already ended.",
            "مسابقه به پایان رسیده است",
            "این مسابقه به پایان رسیده و دیگر قابل رزرو نیست.",
            400
    ),
    MATCH_PAYMENT_IS_NOT_OPEN(
            "Match Payment Is Not Open",
            "Reservation is currently not available for this match.",
            "رزرو برای این مسابقه در دسترس نیست",
            "در حال حاضر امکان رزرو برای این مسابقه وجود ندارد.",
            400
    ),
    SEATS_NOT_FOR_ONE_MATCH(
            "Seats must belong to the same match",
            "Selected seats belong to different matches.",
            "صندلی‌ها متعلق به یک مسابقه نیستند",
            "صندلی‌های انتخاب‌شده متعلق به مسابقه‌های متفاوت هستند.",
            400
    ),
    SEATS_ALREADY_RESERVED(
            "Seats Already Reserved",
            "One or more selected seats are currently unavailable.",
            "خطا در رزرو صندلی",
            "یک یا چند صندلی انتخابی در حال حاضر رزرو شده‌اند و قابل انتخاب نیستند.",
            409 // Conflict
    ),
    SEATS_NOT_FOUND(
            "Seats Not Found",
            "One or more selected seats do not exist in the system.",
            "صندلی یافت نشد",
            "برخی از صندلی‌های درخواستی در سیستم وجود ندارند.",
            404
    ),

    FIELD_EMPTY(
            "This part can't be empty",
            "This part can't be empty",
            "این بخش نمیتواند خالی باشد",
            "این بخش نمیتواند خالی باشد",
            400
    ),
    GET_RESERVES_PAGE_AMOUNT(
            "Page can't be smaller than zero",
            "Page can't be smaller than zero",
            "صفحه نمیتواند کوچکتر از صفر باشد",
            "صفحه نمیتواند کوچکتر از صفر باشد",
            400
    ),
        GET_RESERVES_PAGE_SIZE_AMOUNT(
            "Page Size should be between 1 and 50",
            "Page Size should be between 1 and 50",
            "مقدار سایز صفحه باید بین ۱ تا ۵۰ باشد",
            "مقدار سایز صفحه باید بین ۱ تا ۵۰ باشد",
            400
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