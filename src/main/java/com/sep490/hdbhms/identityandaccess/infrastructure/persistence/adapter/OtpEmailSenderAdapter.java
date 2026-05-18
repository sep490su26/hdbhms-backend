package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.OtpEmailSender;
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
public class OtpEmailSenderAdapter implements OtpEmailSender {
    JavaMailSender mailSender;

    @Async
    @Override
    public void sendOTPEmail(String email, String otpCode) {
        var message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Account Verification");
        message.setText(String.format(
                "To confirm verification, copy this code: %s then paste to confirm the verification code" +
                        "\nThis code expires in 3 minutes.",
                otpCode
        ));
        mailSender.send(message);
    }
}
