package com.sep490.hdbhms.notification.infrastructure.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.notification.application.port.out.NotificationDeliveryRepository;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.application.port.out.UserMobileDeviceTokenRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationDelivery;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.DeliveryStatus;
import com.sep490.hdbhms.notification.infrastructure.external.EmailNotificationService;
import com.sep490.hdbhms.notification.infrastructure.external.PushNotificationService;
import com.sep490.hdbhms.notification.infrastructure.external.SmsNotificationService;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationOutboxProcessor {
    EmailNotificationService emailNotificationService;
    SmsNotificationService smsNotificationService;
    PushNotificationService pushNotificationService;
    UserRepository userRepository;
    NotificationOutboxRepository notificationOutboxRepository;
    UserMobileDeviceTokenRepository userMobileDeviceTokenRepository;
    NotificationDeliveryRepository notificationDeliveryRepository;
    ObjectMapper objectMapper;

    @Async
    public void process(Long outboxId) {
        // Step 1: Atomic lock - only one instance succeeds
        boolean locked = notificationOutboxRepository.markAsProcessing(outboxId);
        if (!locked) {
            log.debug("Outbox {} grabbed by another instance or not PENDING, skipping", outboxId);
            return;
        }

        NotificationOutbox outbox = notificationOutboxRepository.findById(outboxId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        switch (outbox.getChannel()) {
            case PUSH    -> processPush(outbox);
            case EMAIL   -> processEmail(outbox);
            case SMS     -> processSms(outbox);
            case IN_APP  -> processInApp(outbox);
            case WEB     -> processWeb(outbox);
            default      -> log.warn("Unknown channel {} for outbox {}", outbox.getChannel(), outboxId);
        }

        notificationOutboxRepository.save(outbox);
    }

    private void processPush(NotificationOutbox outbox) {
        List<String> tokens = userMobileDeviceTokenRepository
                .findActiveTokenByUserId(outbox.getRecipientUserId());

        if (tokens.isEmpty()) {
            log.warn("No active device tokens for user {}, outbox {}", outbox.getRecipientUserId(), outbox.getId());
            outbox.markRetry("No active device tokens");
            return;
        }

        Map<String, String> data = new HashMap<>();
        if (outbox.getPayload() != null && !outbox.getPayload().isEmpty()) {
            try {
                Map<String, Object> parsedPayload = objectMapper.readValue(
                        outbox.getPayload(),
                        new TypeReference<>() {}
                );
                // Convert all values to String for FCM
                parsedPayload.forEach((k, v) -> {
                    if (v != null) data.put(k, v.toString());
                });
            } catch (Exception e) {
                log.warn("Failed to parse outbox {} payload JSON: {}", outbox.getId(), e.getMessage());
            }
        }

        try {
            String messageIds = pushNotificationService.send(
                    outbox.getTitle(),
                    outbox.getBody(),
                    data,
                    tokens
            );
            
            if (messageIds != null && !messageIds.isEmpty()) {
                outbox.setSent();
                createDeliveryRecord(outbox, messageIds);
                log.info("Push sent for outbox {}: success", outbox.getId());
            } else {
                outbox.markRetry("Failed to send push notification via FCM");
            }

        } catch (FirebaseMessagingException e) {
            log.error("FCM failed for outbox {}: {}", outbox.getId(), e.getMessage());
            outbox.markRetry(e.getMessage());
        }
    }

    private void processEmail(NotificationOutbox outbox) {
        String recipientEmail = resolveRecipientEmail(outbox);
        if (StringUtils.isEmpty(recipientEmail)) {
            log.warn("No email address found for outbox {}", outbox.getId());
            outbox.markDeadLetter("Recipient has no email address");
            return;
        }

        try {
            emailNotificationService.send(recipientEmail, outbox.getTitle(), outbox.getBody());

            log.info("Email sent to {} for outbox {}", recipientEmail, outbox.getId());
            outbox.setSent();
            createDeliveryRecord(outbox, null);

        } catch (Exception e) {
            log.error("Email failed for outbox {}: {}", outbox.getId(), e.getMessage());
            outbox.markRetry(e.getMessage());
        }
    }

    private void processSms(NotificationOutbox outbox) {
        String recipientPhone = resolveRecipientPhone(outbox);
        if (StringUtils.isEmpty(recipientPhone)) {
            log.warn("No phone number found for outbox {}", outbox.getId());
            outbox.markDeadLetter("Recipient has no phone number");
            return;
        }

        try {
            smsNotificationService.send(recipientPhone, outbox.getBody());

            log.info("SMS sent to {} for outbox {}", recipientPhone, outbox.getId());
            outbox.setSent();
            createDeliveryRecord(outbox, null);

        } catch (Exception e) {
            log.error("SMS failed for outbox {}: {}", outbox.getId(), e.getMessage());
            outbox.markRetry(e.getMessage());
        }
    }

    private void processInApp(NotificationOutbox outbox) {
        outbox.setSent();
        createDeliveryRecord(outbox, null);
        log.debug("IN_APP outbox {} marked sent", outbox.getId());
    }

    private void processWeb(NotificationOutbox outbox) {
        outbox.setSent();
        createDeliveryRecord(outbox, null);
        log.debug("WEB outbox {} marked sent", outbox.getId());
    }

    private String resolveRecipientEmail(NotificationOutbox outbox) {
        if (!StringUtils.isEmpty(outbox.getRecipientEmail())) {
            return outbox.getRecipientEmail();
        }
        if (outbox.getRecipientUserId() == null) {
            return null;
        }

        User recipient = userRepository.findById(outbox.getRecipientUserId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        return recipient.getEmail();
    }

    private String resolveRecipientPhone(NotificationOutbox outbox) {
        if (!StringUtils.isEmpty(outbox.getRecipientPhone())) {
            return outbox.getRecipientPhone();
        }
        if (outbox.getRecipientUserId() == null) {
            return null;
        }

        User recipient = userRepository.findById(outbox.getRecipientUserId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        return recipient.getPhone();
    }

    private void createDeliveryRecord(NotificationOutbox outbox, String providerMessageId) {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .outboxId(outbox.getId())
                .providerMessageId(providerMessageId)
                .deliveryStatus(DeliveryStatus.SENT)
                .createdAt(LocalDateTime.now())
                .deliveredAt(LocalDateTime.now())
                .build();
        notificationDeliveryRepository.save(delivery);
    }
}
