package com.sep490.hdbhms.common.email;

import java.time.LocalDateTime;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    private final Environment environment;
    private final JavaMailSender mailSender;

    public ConsoleEmailService(Environment environment, JavaMailSender mailSender) {
        this.environment = environment;
        this.mailSender = mailSender;
    }

    @Override
    public void sendTemporaryAccount(String to, String loginId, String temporaryPassword) {
        boolean devProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (devProfile) {
            log.info("""
                    DEV EMAIL - Temporary tenant account
                    To: {}
                    Login ID: {}
                    Temporary password: {}
                    Guide: Open the HDBHMS mobile app and sign in, then change password.
                    TODO: Configure SMTP provider for non-dev delivery.
                    """, to, loginId, temporaryPassword);
            return;
        }

        log.info("Temporary tenant account email queued for {} with login ID {}. Password is not logged.", to, loginId);
    }

    @Override
    public void sendForgotPasswordOtp(String to, String otp, LocalDateTime expiresAt) {
        String from = environment.getProperty("spring.mail.username");
        if (!StringUtils.hasText(from)) {
            throw new IllegalStateException("GMAIL_USERNAME chưa được cấu hình");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Mã OTP đặt lại mật khẩu TroManager");
        message.setText("""
                Xin chào,

                Mã OTP đặt lại mật khẩu của bạn là: %s
                Mã có hiệu lực trong 5 phút.

                Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                """.formatted(otp));
        mailSender.send(message);
        log.info("Forgot password OTP email sent to {}. OTP is not logged.", to);
    }
}
