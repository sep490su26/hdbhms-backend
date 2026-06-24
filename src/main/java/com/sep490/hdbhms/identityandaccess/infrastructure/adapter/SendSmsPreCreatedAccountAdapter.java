package com.sep490.hdbhms.identityandaccess.infrastructure.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.SendPreCreatedAccountPort;
import com.sep490.hdbhms.shared.application.port.out.SmsPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SendSmsPreCreatedAccountAdapter implements SendPreCreatedAccountPort {
    SmsPort smsPort;

    @Override
    @Async("emailExecutor")
    public void sendAccountInformation(
            String email,
            String fullName,
            String phone,
            String randomPassword
    ) {
        String message = String.format(
                """
                        [Nhà trọ Hải Đăng] Gửi thông tin tài khoản người dùng
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
        );
        smsPort.send(phone, message);
    }

    @Override
    public void sendAccountInformationBatch(
            String email,
            String recipientFullName,
            String phone,
            List<AccountCredential> credentials
    ) {
        if (credentials == null || credentials.isEmpty()) {
            return;
        }
        String message = String.format(
                """
                        [Nhà trọ Hải Đăng] Gửi thông tin tài khoản người dùng
                        
                        Kính gửi Anh/Chị %s,
                        Hệ thống đã tạo tài khoản đăng nhập ứng dụng HDBHMS cho các khách thuê trong hợp đồng.
                        Thông tin đăng nhập:
                        
                        %s
                        
                        Vui lòng đăng nhập ứng dụng mobile, đổi mật khẩu sau lần đăng nhập đầu tiên và hoàn tất cập nhật CCCD/ảnh chân dung nếu hệ thống yêu cầu.
                        Trân trọng.
                        """,
                recipientFullName,
                buildCredentialLines(credentials)
        );
        smsPort.send(phone, message);
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
