package com.hotelbooking.userservice.service;

import com.hotelbooking.userservice.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class VerificationEmailService {
    private final JavaMailSender mailSender;
    private final String from;
    private final String verificationUrl;

    public VerificationEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String from,
            @Value("${app.auth.verification-url}") String verificationUrl
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.verificationUrl = verificationUrl;
    }

    public void send(String recipient, String fullName, String token) {
        if (!StringUtils.hasText(from)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_NOT_CONFIGURED",
                    "MAIL_USERNAME and MAIL_FROM must be configured");
        }
        String link = UriComponentsBuilder.fromUriString(verificationUrl)
                .queryParam("token", token)
                .build().encode().toUriString();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Verify your HotelHub account");
        message.setText("""
                Hello %s,

                Verify your account using this link:
                %s

                Or call POST /api/users/verify/email with:
                {"token":"%s"}

                This link expires in 24 hours.
                """.formatted(fullName, link, token));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_SEND_FAILED",
                    "Could not send verification email");
        }
    }
}
