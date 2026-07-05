package com.backend.common;

import lombok.Getter;

@Getter
public enum ApiMessage {
    // ==============================
    //           General
    // ==============================
    TOO_MANY_REQUESTS(
            "Too many requests",
            "Please try again later",
            "درخواست‌های بیش از حد",
            "تعداد درخواست‌های شما بیش از حد مجاز است. لطفاً چند دقیقه دیگر تلاش کنید",
            429 // Too Many Requests
    ),
    INTERNAL_SERVER_ERROR(
            "internal server error happened",
            "it's not your fault",
            "خطای داخلی سرور",
            "خطای غیرمنتظره‌ای در سرور رخ داده است. لطفاً دقایقی دیگر تلاش کنید.",
            500 // Internal Server Error
    ),

    // =========================
    //     Success messages
    // =========================
    LOGIN_SUCCESS(
            "Logged in successfully",
            null,
            "با موفقیت وارد شدید",
            null,
            200 // OK
    ),
    SIGNUP_SUCCESS(
            "Registered successfully",
            "now go to the login page and login with your username and password",
            "با موفقیت ثبت نام شدید",
            "حال به صفحه ورود بروید و با یوزرنیم و پسورد خود وارد شوید",
            200
    ),

    // Auth Errors
    BAD_CREDENTIALS(
            "Username or password is wrong",
            "Check your entries and try again",
            "نام کاربری یا رمز عبور اشتباه است",
            "ورودی های خود را بررسی و دوباره تلاش کنید",
            401 // Unauthorized
    ),

    ACCOUNT_LOCKED(
            "Account is locked",
            "Try again later",
            "اکانت قفل شده است",
            "بعدا دوباره تلاش کنید",
            403 // Forbidden
    ),

    PASSWORD_EXPIRED(
            "Your password has expired",
            "Use password recovery to reset your password",
            "رمز عبور شما منقضی شده است",
            "از بازیابی رمز عبور برای ریست کردن رمز خود استفاده کنید",
            403 // Forbidden
    ),

    ACCOUNT_SUSPENDED(
            "Your account is suspended",
            null,
            "اکانت شما مسدود شده است",
            null,
            403 // Forbidden
    ),

    // ======================================
    //               Errors
    // ======================================
    VALIDATION_FAILED(
            "Validation failed",
            null,
            "خطای اعتبار سنجی",
            null,
            400 // Bad Request
    ),
    AUTH_SERVICE_UNAVAILABLE(
            "Authentication service unavailable",
            "We are experiencing technical difficulties. Please try again later.",
            "سرویس احراز هویت در دسترس نیست",
            "ارتباط با سرور کاربران موقتاً قطع شده است. لطفاً دقایقی دیگر تلاش کنید.",
            500 // Internal Server Error
    ),

    // Login Errors
    LOGIN_VALIDATION_FAILED(
            "Login failed",
            "Please clear the errors below",
            "وارد نشدید",
            "لطفاً خطاهای زیر را برطرف کنید",
            400
    ),
    LOGIN_IDENTIFIER_REQUIRED(
            "email or phone number is required",
            "it can't be empty",
            "ایمیل یا شماره اجباری است",
            "این بخش نمی‌تواند خالی باشد",
            400
    ),
    LOGIN_IDENTIFIER_SIZE(
            "Invalid email or phone length",
            "it must be between 3 and 255 characters",
            "طول ایمیل یا شماره نامعتبر",
            "باید بین ۳ تا ۲۵۵ کاراکتر باشد",
            400
    ),
    LOGIN_PASSWORD_REQUIRED(
            "Password is required",
            "Password cannot be empty",
            "رمز عبور اجباری است",
            "رمز عبور نمی‌تواند خالی باشد",
            400
    ),
    LOGIN_PASSWORD_SIZE(
            "Invalid password length",
            "Password must be less than 32 characters",
            "طول رمز عبور نامعتبر",
            "رمز عبور باید کمتر از ۳۲ کاراکتر باشد",
            400
    ),

    // Signup Validation Errors
    SIGNUP_VALIDATION_FAILED(
            "Signup failed",
            "Please clear the errors below",
            "ثبت نام نشدید",
            "لطفاً خطاهای زیر را برطرف کنید",
            400
    ),
    SIGNUP_INTERNAL_ERROR(
            "Signup failed",
            "Registration is currently unavailable. Please try again later.",
            "ثبت نام انجام نشد",
            "امکان ثبت نام شما در حال حاضر وجود ندارد. لطفاً بعدا دوباره امتحان کنید.",
            500 // Bad Request
    ),
    SIGNUP_USERNAME_REQUIRED(
            "Username is required",
            "Username cannot be empty",
            "نام کاربری اجباری است",
            "نام کاربری نمی‌تواند خالی باشد",
            400
    ),
    SIGNUP_USERNAME_SIZE(
            "Invalid username length",
            "Username must be between 3 and 65 characters",
            "طول نام کاربری نامعتبر",
            "نام کاربری باید بین ۳ تا ۶۵ کاراکتر باشد",
            400
    ),
    SIGNUP_FIRSTNAME_REQUIRED(
            "First name is required",
            "First name cannot be empty",
            "نام اجباری است",
            "نام نمی‌تواند خالی باشد",
            400
    ),
    SIGNUP_FIRSTNAME_SIZE(
            "Invalid first name length",
            "First name must be between 2 and 50 characters",
            "طول نام نامعتبر",
            "نام باید بین ۲ تا ۵۰ کاراکتر باشد",
            400
    ),
    SIGNUP_LASTNAME_REQUIRED(
            "Last name is required",
            "Last name cannot be empty",
            "نام خانوادگی اجباری است",
            "نام خانوادگی نمی‌تواند خالی باشد",
            400
    ),
    SIGNUP_LASTNAME_SIZE(
            "Invalid last name length",
            "Last name must be between 2 and 50 characters",
            "طول نام خانوادگی نامعتبر",
            "نام خانوادگی باید بین ۲ تا ۵۰ کاراکتر باشد",
            400
    ),
    SIGNUP_PASSWORD_REQUIRED(
            "Password is required",
            "Password cannot be empty",
            "رمز عبور اجباری است",
            "رمز عبور نمی‌تواند خالی باشد",
            400
    ),
    SIGNUP_PASSWORD_SIZE(
            "Invalid password length",
            "Password must be between 8 and 32 characters",
            "طول رمز عبور نامعتبر",
            "رمز عبور باید بین ۸ تا ۳۲ کاراکتر باشد",
            400
    ),
    SIGNUP_USERNAME_FORMAT(
            "Invalid username format",
            "Username can only contain English letters, numbers, and underscores",
            "فرمت نام کاربری نامعتبر",
            "نام کاربری فقط می‌تواند شامل حروف انگلیسی، اعداد و خط تیره پایین (_) باشد",
            400
    ),
    SIGNUP_PASSWORD_WEAK(
            "Password is too weak",
            "Password must contain at least one uppercase, one lowercase, and one number",
            "رمز عبور ضعیف است",
            "رمز عبور باید حداقل شامل یک حرف بزرگ، یک حرف کوچک و یک عدد باشد",
            400
    ),
    SIGNUP_EMAIL_TAKEN(
            "Email is taken",
            "This email has been registered before. you can login with this email",
            "این ایمیل قبلا ثبت شده است",
            "می‌توانید از بخش ورود با این ایمیل وارد شوید",
            409
    ),
    SIGNUP_PHONE_TAKEN(
            "Phone number is taken",
            "This phone number has been registered before. you can login with this email",
            "این شماره قبلا ثبت شده است",
            "می‌توانید از بخش ورود با این شماره وارد شوید",
            409
    ),
    SIGNUP_EMAIL_REQUIRED(
            "Email is required",
            "Email address cannot be empty",
            "ایمیل اجباری است",
            "آدرس ایمیل نمی‌تواند خالی باشد",
            400
    ),
    SIGNUP_EMAIL_FORMAT(
            "Invalid email format",
            "Please enter a valid email address",
            "فرمت ایمیل نامعتبر",
            "لطفاً یک آدرس ایمیل معتبر وارد کنید",
            400
    ),
    SIGNUP_EMAIL_SIZE(
            "Invalid email length",
            "Email must be less than 255 characters",
            "طول ایمیل نامعتبر",
            "ایمیل باید کمتر از ۲۵۵ کاراکتر باشد",
            400
    ),
    SIGNUP_PHONE_REQUIRED(
            "Phone number is required",
            "Phone number cannot be empty",
            "شماره موبایل اجباری است",
            "شماره موبایل نمی‌تواند خالی باشد",
            400
    ),
    SIGNUP_PHONE_FORMAT(
            "Invalid phone number format",
            "Phone number must be valid format (e.g. +989120000000 or 09120000000)",
            "فرمت شماره موبایل نامعتبر",
            "شماره موبایل باید معتبر باشد (مثلاً 09120000000)",
            400
    ),
    SIGNUP_NAME_FORMAT(
            "Invalid name format",
            "Name can only contain letters and spaces",
            "فرمت نام نامعتبر",
            "نام فقط می‌تواند شامل حروف و فاصله باشد",
            400
    );
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