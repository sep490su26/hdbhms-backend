package com.sep490.hdbhms.identityandaccess.infrastructure.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.SendPreCreatedAccountPort;
import com.sep490.hdbhms.identityandaccess.domain.event.PreCreatedAccountNotificationRequestedEvent;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SmartAccountNotificationAdapter implements SendPreCreatedAccountPort {
    
    // Pattern for validating email addresses
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    static final String SUBJECT = "[Nhà trọ Hải Đăng] Gửi thông tin tài khoản người dùng";

    ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void sendAccountInformation(
            Long contractId,
            Long tenantProfileId,
            Long recipientUserId,
            String email,
            String fullName,
            String phone,
            String randomPassword
    ) {
        applicationEventPublisher.publishEvent(
                new PreCreatedAccountNotificationRequestedEvent(
                        contractId,
                        tenantProfileId,
                        recipientUserId,
                        normalizeEmail(email),
                        fullName,
                        phone,
                        resolvePreferredChannel(email, phone),
                        SUBJECT,
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
                        ),
                        false,
                        null
                )
        );
    }

    @Override
    public void sendAccountInformationBatch(
            Long contractId,
            Long recipientProfileId,
            Long recipientUserId,
            String email,
            String recipientFullName,
            String phone,
            List<AccountCredential> credentials
    ) {
        applicationEventPublisher.publishEvent(
                new PreCreatedAccountNotificationRequestedEvent(
                        contractId,
                        recipientProfileId,
                        recipientUserId,
                        normalizeEmail(email),
                        recipientFullName,
                        phone,
                        resolvePreferredChannel(email, phone),
                        SUBJECT,
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
                        ),
                        true,
                        credentials
                )
        );
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

    private NotificationChannel resolvePreferredChannel(String email, String phone) {
        if (isValidEmail(email)) {
            return NotificationChannel.EMAIL;
        }
        if (phone != null && !phone.trim().isEmpty()) {
            return NotificationChannel.SMS;
        }
        return null;
    }

    private String normalizeEmail(String email) {
        return isValidEmail(email) ? email.trim() : null;
    }

    /**
     * Validates if the email is not null, not empty, and matches email pattern.
     * Also rejects synthetic tenant emails (e.g., tenant.hdbhms.local domain).
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String trimmedEmail = email.trim();

        if (trimmedEmail.toLowerCase().endsWith("@tenant.hdbhms.local") ||
            trimmedEmail.toLowerCase().endsWith("tenant.hdbhms.local")) {
            return false;
        }

        return EMAIL_PATTERN.matcher(trimmedEmail).matches();
    }
}
