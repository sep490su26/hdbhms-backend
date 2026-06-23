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
    public void sendResetPasswordVerifier(String email, String passwordResetCode, String resetLink) {
        var message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Nhà Trọ Hải Đăng] Đặt lại mật khẩu");
        message.setText(
                String.format(
                        """
                                        Mã đổi mật khẩu: %s
                                        Để thay đổi mật khẩu, quý khách vui lòng ấn link sau: %s
                                        Link này sẽ hết hạn trong 15 phút.
                                """,
                        passwordResetCode,
                        resetLink
                )
        );
        mailSender.send(message);
    }
}
