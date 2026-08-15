package com.backend.service.notif;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.email-queue:emailQueue}")
    private String emailQueue;

    public void sendEmailOtp(String email, String otpCode) {
        log.info("Initiating OTP email request for: {}", email);

        try {
            String htmlTemplate = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta name="color-scheme" content="light">
                <meta name="supported-color-schemes" content="light">
                <title>Sportik — Verify Your Email</title>
                <!-- وارد کردن فونت مدرن با فال‌بک امن -->
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;800&display=swap" rel="stylesheet">
            </head>
            <body style="margin: 0; padding: 0; background-color: #f3f4f6; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
            
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="width: 100%%; background-color: #f3f4f6;">
                    <tr>
                        <td align="center" style="padding: 40px 16px;">
            
                            <!-- MAIN CARD -->
                            <table width="600" cellpadding="0" cellspacing="0" border="0" style="width: 100%%; max-width: 600px; background-color: #ffffff; border: 1px solid #e5e7eb; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);">
            
                                <!-- HEADER -->
                                <tr>
                                    <td align="center" style="padding: 40px 30px; background-color: #1e3a8a; background-image: linear-gradient(135deg, #1e3a8a 0%%, #3b82f6 100%%);">
                                        <table cellpadding="0" cellspacing="0" border="0">
                                            <tr>
                                                <!-- SPORTIK LOGO ICON -->
                                                <td align="center" valign="middle" style="width: 48px; height: 48px; background-color: #ffffff; border-radius: 12px; color: #1e3a8a; font-size: 24px; font-weight: 800; line-height: 48px;">
                                                    S
                                                </td>
                                                <td style="width: 16px;"></td>
                                                <!-- SPORTIK TEXT -->
                                                <td valign="middle" style="color: #ffffff; font-size: 24px; font-weight: 800; letter-spacing: 3px;">
                                                    SPORTIK
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="height: 6px; background-color: #60a5fa;"></td>
                                </tr>
        
                                <!-- BODY CONTENT -->
                                <tr>
                                    <td align="center" style="padding: 40px 40px 30px 40px;">
                                        <h1 style="margin: 0 0 16px 0; color: #111827; font-size: 24px; font-weight: 700; text-align: center;">
                                            Verify your email
                                        </h1>
            
                                        <p style="margin: 0 0 32px 0; color: #4b5563; font-size: 15px; line-height: 1.6; text-align: center;">
                                            Enter the verification code below to securely continue to your
                                            <strong style="color: #1f2937;">Sportik</strong> account.
                                        </p>
        
                                        <!-- OTP BOX -->
                                        <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                            <tr>
                                                <td align="center" style="padding: 30px 20px; border: 2px dashed #cbd5e1; border-radius: 12px; background-color: #f8fafc;">
                                                    <p style="margin: 0 0 10px 0; color: #64748b; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 2px;">
                                                        Verification Code
                                                    </p>
                                                    <p style="margin: 0; color: #2563eb; font-family: 'Courier New', Courier, monospace; font-size: 44px; font-weight: 800; letter-spacing: 12px; padding-left: 12px;">
                                                        %s
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
        
                                        <p style="margin: 20px 0 30px 0; color: #94a3b8; font-size: 13px; text-align: center;">
                                            This code is valid for a limited time.
                                        </p>
        
                                        <!-- SECURITY NOTICE -->
                                        <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                            <tr>
                                                <td style="padding: 16px; background-color: #eff6ff; border-left: 4px solid #3b82f6; border-radius: 0 8px 8px 0;">
                                                    <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                                        <tr>
                                                            <td valign="top" style="width: 24px; font-size: 16px; padding-top: 2px;">
                                                                🛡
                                                            </td>
                                                            <td style="padding-left: 12px; color: #1e40af; font-size: 13px; line-height: 1.5;">
                                                                <strong>Security Notice:</strong> Sportik will never ask you to share your password or verification code.
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        </table>
        
                                    </td>
                                </tr>
        
                                <!-- FOOTER -->
                                <tr>
                                    <td align="center" style="padding: 24px 30px; background-color: #f8fafc; border-top: 1px solid #e2e8f0;">
                                        <p style="margin: 0 0 8px 0; color: #64748b; font-size: 14px; font-weight: 700; letter-spacing: 1px;">
                                            SPORTIK
                                        </p>
                                        <p style="margin: 0 0 12px 0; color: #94a3b8; font-size: 12px; line-height: 1.5;">
                                            You received this email because a verification was requested for your account.<br>
                                            If you didn't request this code, you can safely ignore this email.
                                        </p>
                                        <p style="margin: 0; color: #cbd5e1; font-size: 11px;">
                                            &copy; 2026 Sportik Platform · All rights reserved
                                        </p>
                                    </td>
                                </tr>
                            </table>
        
                            <!-- AUTOMATED MESSAGE WARNING -->
                            <p style="margin: 20px 0 0 0; color: #9ca3af; font-size: 11px; text-align: center;">
                                This is an automated message. Please do not reply.
                            </p>
        
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;

            String formattedBody = String.format(htmlTemplate, otpCode);

            // payload
            Map<String, String> payload = Map.of(
                    "To", email,
                    "Subject", "Sportik Login Verification Code",
                    "Body", formattedBody
            );

            log.debug("Attempting to send message to RabbitMQ queue: [{}]", emailQueue);
            rabbitTemplate.convertAndSend("", emailQueue, payload);

            log.info("OTP Email request successfully sent to RabbitMQ for: {}", email);

        } catch (AmqpException e) {
            log.error("Failed to send OTP email request to RabbitMQ for email: {}. Reason: {}", email, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error occurred while preparing/sending OTP email for: {}", email, e);
            throw e;
        }
    }
}