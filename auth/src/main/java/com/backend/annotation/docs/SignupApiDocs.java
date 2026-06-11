package com.backend.annotation.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for Swagger documentation of the Signup endpoint.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(
        summary = "Register new user",
        description = "Register new user with basic data like username, password, name and last name"
)
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "User registered successfully",
                content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                        {
                          "success": true,
                          "status": 200,
                          "title": "Registered successfully",
                          "message": "now go to the login page and login with your username and password",
                          "titleFa": "با موفقیت ثبت نام شدید",
                          "messageFa": "حال به صفحه ورود بروید و با یوزرنیم و پسورد خود وارد شوید",
                          "data": {
                            "userId": 1
                          },
                          "timestamp": "2026-06-11T16:01:00"
                        }
                        """))
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Validation Error, Format Error, or Username Taken",
                content = @Content(mediaType = "application/json", examples = {
                        @ExampleObject(name = "Validation Error", value = """
                        {
                          "success": false,
                          "status": 400,
                          "title": "Signup failed",
                          "message": "Please clear the errors below",
                          "titleFa": "ثبت نام نشدید",
                          "messageFa": "لطفاً خطاهای زیر را برطرف کنید",
                          "data": {
                            "password": [
                              {
                                "message": "Password cannot be empty",
                                "messageFa": "رمز عبور نمی‌تواند خالی باشد"
                              }
                            ]
                          },
                          "timestamp": "2026-06-11T16:02:00"
                        }
                        """),
                        @ExampleObject(name = "Format Error", value = """
                        {
                          "success": false,
                          "status": 400,
                          "title": "Password is too weak",
                          "message": "Password must contain at least one uppercase, one lowercase, and one number",
                          "titleFa": "رمز عبور ضعیف است",
                          "messageFa": "رمز عبور باید حداقل شامل یک حرف بزرگ، یک حرف کوچک و یک عدد باشد",
                          "data": {
                            "field": "password"
                          },
                          "timestamp": "2026-06-11T16:02:30"
                        }
                        """),
                        @ExampleObject(name = "Username Taken Error", value = """
                        {
                          "success": false,
                          "status": 400,
                          "title": "Username is already taken",
                          "message": "Please choose another username",
                          "titleFa": "نام کاربری تکراری است",
                          "messageFa": "این نام کاربری قبلاً توسط شخص دیگری انتخاب شده است. یک نام کاربری دیگر را امتحان کنید",
                          "data": {
                            "field": "username"
                          },
                          "timestamp": "2026-06-11T16:03:00"
                        }
                        """)
                })
        ),
        @ApiResponse(
                responseCode = "429",
                description = "Too many requests (Rate Limit)",
                content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                        {
                          "success": false,
                          "status": 429,
                          "title": "Too many requests",
                          "message": "Please try again later",
                          "titleFa": "درخواست‌های بیش از حد",
                          "messageFa": "تعداد درخواست‌های شما بیش از حد مجاز است. لطفاً چند دقیقه دیگر تلاش کنید",
                          "data": null,
                          "timestamp": "2026-06-11T16:04:00"
                        }
                        """))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Unexpected Internal Server Error",
                content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                        {
                          "success": false,
                          "status": 500,
                          "title": "internal server error happened",
                          "message": "it's not your fault",
                          "titleFa": "خطای داخلی سرور",
                          "messageFa": "خطای غیرمنتظره‌ای در سرور رخ داده است. لطفاً دقایقی دیگر تلاش کنید.",
                          "data": null,
                          "timestamp": "2026-06-11T16:05:00"
                        }
                        """))
        )
})
public @interface SignupApiDocs {
}