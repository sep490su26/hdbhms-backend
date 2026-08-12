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
        message.setSubject("Xác minh tài khoản");
        message.setText(String.format(
                "Để xác minh tài khoản, hãy sao chép mã này: %s rồi nhập vào ô xác nhận mã." +
                        "\nMã có hiệu lực trong 3 phút.",
                otpCode
        ));
        mailSender.send(message);
    }
}
