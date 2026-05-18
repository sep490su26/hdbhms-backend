package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.PasswordResetEmailVerifierSender;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordResetEmailVerifierSenderAdapter implements PasswordResetEmailVerifierSender {
    JavaMailSender mailSender;

    @Async
    @Override
    public void sendResetPasswordVerifier(String email, String resetLink) {
        var message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset Verification");
        message.setText("To reset your password, click the link:\n" + resetLink +
                "\nThis link expires in 1 hour.");
        mailSender.send(message);
    }
}
