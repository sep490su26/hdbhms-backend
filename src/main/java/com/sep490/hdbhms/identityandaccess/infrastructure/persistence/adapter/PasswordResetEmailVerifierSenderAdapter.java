package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.identityandaccess.application.port.out.PasswordResetEmailVerifierSender;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;
import com.sep490.hdbhms.notification.infrastructure.processor.NotificationOutboxProcessor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordResetEmailVerifierSenderAdapter implements PasswordResetEmailVerifierSender {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    NotificationOutboxRepository notificationOutboxRepository;
    NotificationOutboxProcessor notificationOutboxProcessor;
    ObjectMapper objectMapper;

    @Override
    public void sendResetPasswordVerifier(
            Long userId,
            String email,
            String phone,
            String passwordResetCode,
            String resetLink
    ) {
        NotificationChannel channel = resolvePreferredChannel(email, phone);
        NotificationOutbox savedOutbox = notificationOutboxRepository.save(
                buildOutbox(userId, email, phone, passwordResetCode, resetLink, channel)
        );

        if (savedOutbox.getStatus() == OutboxStatus.PENDING) {
            dispatchAfterCommit(savedOutbox.getId());
        }
    }

    private void dispatchAfterCommit(Long outboxId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationOutboxProcessor.process(outboxId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationOutboxProcessor.process(outboxId);
            }
        });
    }

    private NotificationOutbox buildOutbox(
            Long userId,
            String email,
            String phone,
            String passwordResetCode,
            String resetLink,
            NotificationChannel channel
    ) {
        LocalDateTime now = LocalDateTime.now();
        NotificationChannel finalChannel = channel == null ? NotificationChannel.EMAIL : channel;

        return NotificationOutbox.builder()
                .eventType("PASSWORD_RESET_REQUESTED")
                .targetType("USER_ACCOUNT")
                .targetId(userId)
                .recipientUserId(userId)
                .recipientEmail(normalizeEmail(email))
                .recipientPhone(normalizePhone(phone))
                .channel(finalChannel)
                .title("[Nhà Trọ Hải Đăng] Đặt lại mật khẩu")
                .body(buildBody(finalChannel, passwordResetCode, resetLink))
                .payload(buildPayload(userId, email, phone, finalChannel, resetLink))
                .status(channel == null ? OutboxStatus.DEAD_LETTER : OutboxStatus.PENDING)
                .maxRetries(finalChannel == NotificationChannel.SMS ? 1 : 3)
                .lastError(channel == null ? "Account has no email or phone for password reset" : null)
                .isRead(false)
                .scheduledAt(now)
                .nextRetryAt(channel == null ? null : now)
                .createdAt(now)
                .build();
    }

    private String buildBody(NotificationChannel channel, String passwordResetCode, String resetLink) {
        if (channel == NotificationChannel.SMS) {
            return String.format(
                    "Ma dat lai mat khau Nha Tro Hai Dang: %s. Link: %s. Hieu luc 15 phut.",
                    passwordResetCode,
                    resetLink
            );
        }

        return String.format(
                """
                                Mã đổi mật khẩu: %s
                                Để thay đổi mật khẩu, quý khách vui lòng ấn link sau: %s
                                Link này sẽ hết hạn trong 15 phút.
                        """,
                passwordResetCode,
                resetLink
        );
    }

    private String buildPayload(
            Long userId,
            String email,
            String phone,
            NotificationChannel channel,
            String resetLink
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("recipientEmail", normalizeEmail(email));
        payload.put("recipientPhone", normalizePhone(phone));
        payload.put("preferredChannel", channel);
        payload.put("resetLink", resetLink);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize password reset notification payload. userId={}", userId, exception);
            return null;
        }
    }

    private NotificationChannel resolvePreferredChannel(String email, String phone) {
        if (isValidEmail(email)) {
            return NotificationChannel.EMAIL;
        }
        if (normalizePhone(phone) != null) {
            return NotificationChannel.SMS;
        }
        return null;
    }

    private String normalizeEmail(String email) {
        return isValidEmail(email) ? email.trim() : null;
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.trim().isEmpty() ? null : phone.trim();
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String trimmedEmail = email.trim();
        String lowerEmail = trimmedEmail.toLowerCase();
        if (lowerEmail.endsWith("@tenant.hdbhms.local") || lowerEmail.endsWith("tenant.hdbhms.local")) {
            return false;
        }

        return EMAIL_PATTERN.matcher(trimmedEmail).matches();
    }
}
