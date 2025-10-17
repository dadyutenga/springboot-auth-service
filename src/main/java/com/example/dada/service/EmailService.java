package com.example.dada.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Async
    public void sendOtpEmail(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject("Email Verification - OTP");

            String htmlContent = buildOtpEmailContent(otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", to, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    private String buildOtpEmailContent(String otp) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }");
        html.append(".content { background-color: #f9f9f9; padding: 20px; border-radius: 5px; }");
        html.append(".otp-code { font-size: 32px; font-weight: bold; color: #4CAF50; text-align: center; ");
        html.append("padding: 20px; background-color: white; border-radius: 5px; margin: 20px 0; letter-spacing: 5px; }");
        html.append(".footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }");
        html.append(".warning { color: #d32f2f; font-size: 14px; margin-top: 10px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>Email Verification</h1>");
        html.append("</div>");
        html.append("<div class='content'>");
        html.append("<h2>Hello!</h2>");
        html.append("<p>Thank you for registering. Please use the following One-Time Password (OTP) to verify your email address:</p>");
        html.append("<div class='otp-code'>").append(otp).append("</div>");
        html.append("<p>This OTP is valid for <strong>5 minutes</strong>.</p>");
        html.append("<p class='warning'>Warning: If you did not request this code, please ignore this email.</p>");
        html.append("</div>");
        html.append("<div class='footer'>");
        html.append("<p>This is an automated email. Please do not reply.</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }
}
