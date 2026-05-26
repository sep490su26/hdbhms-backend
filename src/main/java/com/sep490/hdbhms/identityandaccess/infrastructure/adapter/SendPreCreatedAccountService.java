package com.sep490.hdbhms.identityandaccess.infrastructure.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.SendPreCreatedAccountPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SendPreCreatedAccountService implements SendPreCreatedAccountPort {
    JavaMailSender javaMailSender;

    @Override
    @Async("emailExecutor")
    public void sendAccountInformation(
            String email,
            String fullName,
            String phone,
            String randomPassword
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("[Nhà trọ Hải Đăng] Gửi thông tin tài khoản người dùng");
        message.setTo(email);
        message.setText(
                String.format(
                        """
                                Kính gửi Anh/Chị %s,
                                
                                Hệ thống đã tạo tài khoản cho Anh/Chị thành công.
                                
                                Thông tin đăng nhập:
                                
                                Tên đăng nhập: %s
                                Mật khẩu tạm thời: %s
                                
                                
                                Vui lòng đăng nhập và đổi mật khẩu sau lần đăng nhập đầu tiên để đảm bảo an toàn tài khoản.
                                Trân trọng.
                                """,
                        fullName,
                        phone,
                        randomPassword
                )
        );
        javaMailSender.send(message);
    }
}
