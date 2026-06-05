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

import java.util.List;

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

    @Override
    @Async("emailExecutor")
    public void sendAccountInformationBatch(
            String email,
            String recipientFullName,
            List<AccountCredential> credentials
    ) {
        if (credentials == null || credentials.isEmpty()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("[Nhà trọ Hải Đăng] Gửi thông tin tài khoản người dùng");
        message.setTo(email);
        message.setText(
                String.format(
                        """
                                Kính gửi Anh/Chị %s,

                                Hệ thống đã tạo tài khoản đăng nhập ứng dụng HDBHMS cho các khách thuê trong hợp đồng.

                                Thông tin đăng nhập:

                                %s

                                Vui lòng đăng nhập ứng dụng mobile, đổi mật khẩu sau lần đăng nhập đầu tiên và hoàn tất cập nhật CCCD/ảnh chân dung nếu hệ thống yêu cầu.
                                Trân trọng.
                                """,
                        recipientFullName,
                        buildCredentialLines(credentials)
                )
        );
        javaMailSender.send(message);
    }

    private String buildCredentialLines(List<AccountCredential> credentials) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < credentials.size(); i++) {
            AccountCredential credential = credentials.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(credential.fullName())
                    .append(" - ")
                    .append(roleLabel(credential.roomRole()))
                    .append(System.lineSeparator())
                    .append("Tên đăng nhập: ")
                    .append(credential.phone())
                    .append(System.lineSeparator())
                    .append("Mật khẩu tạm thời: ")
                    .append(credential.randomPassword());
            if (i < credentials.size() - 1) {
                builder.append(System.lineSeparator()).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    private String roleLabel(String roomRole) {
        return "PRIMARY".equalsIgnoreCase(roomRole) ? "Người ký chính" : "Người ở cùng";
    }
}
