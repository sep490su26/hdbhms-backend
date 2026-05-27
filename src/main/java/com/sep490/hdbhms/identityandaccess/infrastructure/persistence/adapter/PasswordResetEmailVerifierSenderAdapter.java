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

    //TODO: Thêm thời hạn hữu dụng của link
    @Async
    @Override
    public void sendResetPasswordVerifier(String email, String resetLink) {
        var message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Nhà Trọ Hải Đăng] Đặt lại mật khẩu");
        message.setText("Để thay đổi mật khẩu, quý khách vui lòng ấn link sau:\n" + resetLink +
                "\nLink này sẽ hết hạn trong .");
        mailSender.send(message);
    }
}
