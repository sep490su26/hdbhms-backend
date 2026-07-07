package com.sep490.hdbhms.notification.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.notification.application.port.in.query.NotificationQueryUseCase;
import com.sep490.hdbhms.notification.application.port.in.usecase.ManageNotificationUseCase;
import com.sep490.hdbhms.notification.application.port.in.usecase.SendNotificationUseCase;
import com.sep490.hdbhms.notification.application.port.out.NotificationDeliveryRepository;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.application.port.out.NotificationTemplateRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.model.NotificationTemplate;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;
import com.sep490.hdbhms.notification.domain.value_objects.TemplateStatus;
import com.sep490.hdbhms.shared.event.NotificationEvent;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService implements SendNotificationUseCase, NotificationQueryUseCase, ManageNotificationUseCase {
    NotificationTemplateRepository templateRepository;
    NotificationOutboxRepository outboxRepository;
    NotificationDeliveryRepository deliveryRepository;
    ObjectMapper objectMapper;
    TemplateEngine stringTemplateEngine = new TemplateEngine();

    @PostConstruct
    public void init() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setCacheable(false);
        stringTemplateEngine.setTemplateResolver(resolver);
    }

    @Override
    public void queueNotification(NotificationEvent event) {
        List<NotificationTemplate> templates = templateRepository.findByTemplateKeyAndStatus(
                event.getEventType(), TemplateStatus.ACTIVE);

        if (templates.isEmpty()) {
            log.warn("No active templates found for event type: {}", event.getEventType());
            return;
        }

        Context context = new Context();
        if (event.getData() != null) {
            context.setVariables(event.getData());
        }

        for (NotificationTemplate template : templates) {
            String title = stringTemplateEngine.process(template.getTitleTemplate(), context);
            String body = stringTemplateEngine.process(template.getBodyTemplate(), context);

            NotificationOutbox outbox = NotificationOutbox.builder()
                    .eventType(event.getEventType())
                    .targetType(event.getTargetType())
                    .targetId(event.getTargetId())
                    .recipientUserId(event.getUserId())
                    .channel(template.getChannel())
                    .title(title)
                    .body(body)
                    .payload(toPayload(event))
                    .status(OutboxStatus.PENDING)
                    .maxRetries(3)
                    .isRead(false)
                    .scheduledAt(LocalDateTime.now())
                    .nextRetryAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxRepository.save(outbox);
        }
    }

    private List<NotificationTemplate> resolveTemplates(String eventType) {
        List<NotificationTemplate> dbTemplates = templateRepository.findByTemplateKeyAndStatus(
                        eventType, TemplateStatus.ACTIVE)
                .stream()
                .filter(template -> !isLegacyRoomTransferTemplate(template))
                .toList();
        List<NotificationTemplate> defaults = defaultTemplates(eventType);
        if (defaults.isEmpty()) {
            return dbTemplates;
        }
        if (dbTemplates.isEmpty()) {
            return defaults;
        }

        Map<NotificationChannel, NotificationTemplate> templatesByChannel = new LinkedHashMap<>();
        defaults.forEach(template -> templatesByChannel.put(template.getChannel(), template));
        dbTemplates.forEach(template -> templatesByChannel.put(template.getChannel(), template));
        return List.copyOf(templatesByChannel.values());
    }

    private List<NotificationTemplate> defaultTemplates(String eventType) {
        return switch (eventType) {
            case "ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED" -> allChannelTemplates(
                    eventType,
                    "Bạn được đề cử làm người đại diện phòng",
                    "Yêu cầu chuyển phòng [[${requestCode}]] cần bạn xác nhận làm người đại diện mới của [[${oldRoomName}]] sau khi người hiện tại chuyển đi. Vui lòng phản hồi để quản lý tiếp tục xử lý."
            );
            case "ROOM_TRANSFER_TARGET_HOLDER_APPROVAL_REQUESTED" -> allChannelTemplates(
                    eventType,
                    "Có người muốn chuyển vào phòng của bạn",
                    "Yêu cầu [[${requestCode}]]: khách từ [[${oldRoomName}]] muốn chuyển vào [[${targetRoomName}]]. Ngày dự kiến chuyển là [[${expectedTransferDate}]]. Vui lòng xác nhận nếu bạn đồng ý."
            );
            case "ROOM_TRANSFER_MANAGER_ACTION_REQUIRED" -> allChannelTemplates(
                    eventType,
                    "Yêu cầu chuyển phòng cần xử lý",
                    "Yêu cầu [[${requestCode}]] đang cần quản lý xử lý: [[${actionLabel}]]. Chuyển từ [[${oldRoomName}]] sang [[${targetRoomName}]], ngày dự kiến chuyển [[${expectedTransferDate}]]."
            );
            default -> List.of();
        };
    }

    private boolean isLegacyRoomTransferTemplate(NotificationTemplate template) {
        return "ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED".equals(template.getTemplateKey())
                && "Xac nhan holder moi".equals(template.getTitleTemplate());
    }

    private List<NotificationTemplate> allChannelTemplates(String key, String title, String body) {
        return Arrays.stream(NotificationChannel.values())
                .map(channel -> NotificationTemplate.builder()
                        .templateKey(key)
                        .channel(channel)
                        .titleTemplate(title)
                        .bodyTemplate(body)
                        .status(TemplateStatus.ACTIVE)
                        .build())
                .toList();
    }

    private String toPayload(NotificationEvent event) {
        if (event.getData() == null || event.getData().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(event.getData());
        } catch (Exception exception) {
            log.warn("Failed to serialize notification payload for event type {}", event.getEventType(), exception);
            return null;
        }
    }

    @Override
    public Page<NotificationOutbox> getNotificationsWeb(Long userId, NotificationChannel channel, Pageable pageable) {
        return outboxRepository.findByRecipientUserIdAndChannelOrderByCreatedAtDesc(userId, channel, pageable);
    }

    @Override
    public List<NotificationOutbox> getNotificationsMobile(Long userId, NotificationChannel channel, long after, int limit) {
        return outboxRepository.findNextNotificationsCursor(userId, channel, after, limit);
    }

    @Override
    public long getUnreadCount(Long userId, NotificationChannel channel) {
        return outboxRepository.countByRecipientUserIdAndChannelAndIsReadFalse(userId, channel);
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        NotificationOutbox outbox = outboxRepository.findById(id)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        if (!Objects.equals(outbox.getRecipientUserId(), userId)) {
            throw new AppException(ApiErrorCode.UNAUTHORIZED);
        }

        LocalDateTime readAt = LocalDateTime.now();
        outbox.markAsRead(readAt);
        outboxRepository.save(outbox);
        deliveryRepository.markReadByOutboxId(id, readAt);
    }

    @Override
    public void markAllAsRead(Long userId) {
        LocalDateTime readAt = LocalDateTime.now();
        outboxRepository.markAllAsRead(userId, readAt);
        deliveryRepository.markReadByRecipientUserId(userId, readAt);
    }

    @Override
    public void markTargetAsRead(Long userId, String targetType, Long targetId) {
        if (targetType == null || targetType.isBlank() || targetId == null || targetId <= 0) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
        String normalizedTargetType = targetType.trim().toUpperCase(Locale.ROOT);
        LocalDateTime readAt = LocalDateTime.now();
        outboxRepository.markTargetAsRead(userId, normalizedTargetType, targetId, readAt);
        deliveryRepository.markReadByRecipientUserIdAndTarget(userId, normalizedTargetType, targetId, readAt);
    }

    public void markAllAsRead(Long userId, NotificationChannel channel) {
        outboxRepository.markAllAsRead(userId, channel);
    }
}
