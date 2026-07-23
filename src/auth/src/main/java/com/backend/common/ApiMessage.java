package com.backend.common;

import lombok.Getter;

@Getter
public enum ApiMessage {
    // ==============================
    //           General
    // ==============================
    RESOURCE_NOT_FOUND(
            "Not Found",
            "The requested endpoint or resource does not exist",
            "یافت نشد",
            "مسیر یا منبع درخواستی یافت نشد",
            404
    ),
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
    LOGIN_EMAIL_OTP_SENT(
            "If the email exists, a verification code has been sent.",
            "Please check your inbox and spam folder. If you haven't received it, your account may be locked or suspended. You can try logging in with a password or contact support.",
            "اگر این ایمیل وجود داشته باشد، یک کد تأیید برای آن ارسال شده است.",
            "لطفاً صندوق ورودی و پوشه Spam خود را بررسی کنید. اگر ایمیلی دریافت نکردید، ممکن است حساب شما قفل یا مسدود شده باشد. می‌توانید ورود با رمز عبور را امتحان کرده یا با پشتیبانی تماس بگیرید.",
            200
    ),
    LOGIN_PHONE_OTP_SENT(
            "If the phone number exists, a verification code has been sent.",
            "If you haven't received it, your account may be locked or suspended. You can try logging in with a password or contact support.",
            "اگر این شماره وجود داشته باشد، یک کد تأیید برای آن ارسال شده است.",
            "اگر پیامکی دریافت نکردید، ممکن است حساب شما قفل یا مسدود شده باشد. می‌توانید ورود با رمز عبور را امتحان کرده یا با پشتیبانی تماس بگیرید.",
            200
    ),
    LOGIN_SUCCESS_NEED_2FA_EMAIL(
            "Verification code sent to your email",
            "Two-factor authentication is enabled for your account. If you haven't received it, Please check your inbox and spam folder.",
            "کد تایید به ایمیل شما ارسال شد",
            "احراز هویت دومرحله‌ای برای حساب شما فعال است. اگر ایمیلی دریافت نکردید، لطفاً صندوق ورودی و پوشه Spam خود را بررسی کنید.",
            200
    ),
    LOGIN_SUCCESS_NEED_2FA_PHONE(
            "Verification code sent to your phone",
            "Two-factor authentication is enabled for your account. Please check your email and enter the code.",
            "کد تایید به شماره شما ارسال شد",
            "احراز هویت دومرحله‌ای برای حساب شما فعال است. لطفا پیامک خود را بررسی کرده و کد تایید را وارد کنید.",
            200
    ),
    SIGNUP_SUCCESS(
            "Registration successful",
            "Please sign in to continue.",
            "ثبت‌نام با موفقیت انجام شد",
            "برای ادامه وارد حساب کاربری خود شوید.",
            200
    ),
    RESET_PASSWORD_SUCCESS(
            "Password Reset Successfully",
            "You have been logged out from all devices. Please log in with your new password.",
            "رمز عبور با موفقیت تغییر یافت",
            "از تمامی دستگاه‌ها خارج شدید. لطفاً با رمز عبور جدید وارد شوید.",
            200
    ),
    SIGNUP_INITIATE_EMAIL_SUCCESS(
            "Verification code sent to your email",
            "If you haven't received it, Please check your inbox and spam folder.",
            "کد تایید به ایمیل شما ارسال شد",
            "اگر ایمیلی دریافت نکردید، لطفاً صندوق ورودی و پوشه Spam خود را بررسی کنید.",
            200
    ),
    RESET_PASSWORD_INITIATE_EMAIL_SUCCESS(
            "Verification code sent to your email",
            "If you haven't received it, Please check your inbox and spam folder.",
            "کد تایید به ایمیل شما ارسال شد",
            "اگر ایمیلی دریافت نکردید، لطفاً صندوق ورودی و پوشه Spam خود را بررسی کنید.",
            200
    ),
    SIGNUP_VERIFY_OTP_SUCCESS(
            "Email Verified Successfully",
            "Your email has been verified. You can now complete your registration process.",
            "ایمیل با موفقیت تایید شد",
            "ایمیل شما با موفقیت تایید شد. اکنون می‌توانید مراحل ثبت‌نام خود را تکمیل کنید.",
            200
    ),
    RESET_PASSWORD_VERIFY_OTP_SUCCESS(
            "Email Verified Successfully",
            "Your email has been verified. You can set your new password.",
            "ایمیل با موفقیت تایید شد",
            "ایمیل شما با موفقیت تایید شد. اکنون می‌توانید رمز جدید خود را تعیین کنید.",
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
            "Your account has been locked.",
            "اکانت قفل شده است",
            "حساب شما قفل شده است.",
            429 // Too Many Requests
    ),

    LOGIN_MFA_COOLDOWN(
            "Please Wait!",
            "A verification code was recently sent.",
            "لطفا صبر کنید!",
            "کد تایید به تازگی ارسال شده است.",
            429 // Too Many Requests
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
    SIGNUP_FAILED(
            "Signup Failed",
            "We couldn't complete your registration. Please try again later.",
            "ثبت‌نام انجام نشد",
            "ثبت‌نام شما انجام نشد. لطفاً دوباره تلاش کنید.",
            400
    ),
    RESET_PASSWORD_FAILED(
            "Reset Password Failed",
            "We couldn't complete your request. Please try again later.",
            "تغییر رمز عبور انجام نشد",
            "لطفاً بعدا تلاش کنید.",
            400
    ),
    SIGNUP_INVALID_TEMP_TOKEN(
            "Session Expired",
            "Your session has expired. Please try again.",
            "نشست شما به پایان رسیده است",
            "مهلت انجام این مرحله به پایان رسیده است. لطفاً دوباره تلاش کنید.",
            400
    ),
    EMAIL_NOT_FOUND(
            "Email not found",
            "this email is not exist.",
            "ایمیل وجود ندارد",
            "این ایمیل موجود نیست",
            400
    ),
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
    LOGIN_EMAIL_REQUIRED(
            "Email is required",
            "email can't be empty",
            "ایمیل اجباری است",
            "ایمیل نمی‌تواند خالی باشد",
            400
    ),
    LOGIN_IDENTIFIER_SIZE(
            "Invalid email or phone length",
            "it must be between 3 and 255 characters",
            "طول ایمیل یا شماره نامعتبر",
            "باید بین ۳ تا ۲۵۵ کاراکتر باشد",
            400
    ),
    LOGIN_EMAIL_SIZE(
            "Invalid email length",
            "email must be between 3 and 255 characters",
            "طول ایمیل نامعتبر است",
            "ایمیل باید بین ۳ تا ۲۵۵ کاراکتر باشد",
            400
    ),
    LOGIN_PHONE_REQUIRED(
            "Phone is required",
            "phone number can't be empty",
            "شماره اجباری است",
            "شماره نمی‌تواند خالی باشد",
            400
    ),
    LOGIN_PHONE_SIZE(
            "Invalid phone number",
            "Invalid phone number",
            "شماره نامعتبر است",
            "شماره نامعتبر است",
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

    LOGIN_MFA_REQUIRED(
            "MFA token is required",
            "it can't be empty",
            "توکن MFA اجباری است",
            "این بخش نمی‌تواند خالی باشد",
            400
    ),
    LOGIN_MFA_SIZE(
            "Invalid MFA length",
            "it must be smaller than 255 characters",
            "طول توکن mfa نامعتبر",
            "باید کمتر از ۲۵۵ کاراکتر باشد",
            400
    ),
    LOGIN_OTP_REQUIRED(
            "Validation code is required",
            "OTP code can't be empty",
            "کد تایید اجباری است",
            "کد تایید نمی‌تواند خالی باشد",
            400
    ),
    LOGIN_OTP_SIZE(
            "Invalid validation code length",
            "OTP must be smaller than 6 digits",
            "طول کد تایید نامعتبر",
            "باید کمتر از ۶ رقم باشد",
            400
    ),
    LOGIN_MFA_EXPIRED(
            "Session Expired",
            "Your verification session has expired or is invalid. Please try logging in again.",
            "پایان مهلت تایید",
            "زمان تایید کد به پایان رسیده و یا درخواست نامعتبر است. لطفاً مجدداً وارد شوید.",
            400
    ),
    LOGIN_OTP_WRONG(
            "Invalid or Expired Verification Code",
            "The verification code is invalid or has expired. Please request a new code.",
            "کد تأیید نامعتبر یا پایان‌یافته",
            "کد وارد شده نادرست است یا دیگر اعتبار ندارد. لطفاً کد جدیدی درخواست کنید.",
            400
    ),

    //========================================================================================
    //                             Signup Validation Errors
    //========================================================================================
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
    SIGNUP_TEMP_TOKEN_REQUIRED(
            "Temp token is required",
            "Temp token cannot be empty",
            "توکن موقت اجباری است",
            "توکن موقت نمی‌تواند خالی باشد",
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
            "This phone number has been registered before. you can login with this phone",
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
    ),

    // ================================================
    REFRESH_SUCCESS(
            "Successful Refresh",
            null,
            "رفرش موفق",
            null,
            200
    ),
    REFRESH_TOKEN_NOT_EXIST(
            "Refresh Token is not exist or revoked",
            "Refresh Token is not exist or revoked",
            "رفرش توکن وجود ندارد یا باطل شده است",
            "رفرش توکن وجود ندارد یا باطل شده است",
            400
    ),
    REFRESH_TOKEN_EXPIRED(
            "Refresh Token is expired",
            "login again",
            "رفرش توکن منقضی شده است",
            "دوباره وارد شوید",
            400
    ),
    REFRESH_TOKEN_INVALID_SIZE(
            "Refresh Token length should be 36",
            "Refresh Token length should be 36",
            "طول رفرش توکن باید ۳۶ باشد",
            "طول رفرش توکن باید ۳۶ باشد",
            400
    ),
    REFRESH_TOKEN_REQUIRED(
            "Refresh Token is required",
            "Refresh Token is required",
            "رفرش توکن اجباری است",
            "رفرش توکن اجباری است",
            400
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