package com.sep490.hdbhms.notification.application.service;

import com.sep490.hdbhms.notification.application.port.in.query.NotificationQueryUseCase;
import com.sep490.hdbhms.notification.application.port.in.usecase.ManageNotificationUseCase;
import com.sep490.hdbhms.notification.application.port.in.usecase.SendNotificationUseCase;
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
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService implements SendNotificationUseCase, NotificationQueryUseCase, ManageNotificationUseCase {

    NotificationTemplateRepository templateRepository;
    NotificationOutboxRepository outboxRepository;
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

    @Override
    public Page<NotificationOutbox> getNotificationsWeb(Long userId, NotificationChannel channel, Pageable pageable) {
        return outboxRepository.findByRecipientUserIdAndChannelOrderByCreatedAtDesc(userId, channel, pageable);
    }

    @Override
    public List<NotificationOutbox> getNotificationsMobile(Long userId, NotificationChannel channel, long after, int limit) {
        return outboxRepository.findNextNotificationsCursor(userId, channel, after, limit);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return outboxRepository.countByRecipientUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        NotificationOutbox outbox = outboxRepository.findById(id)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        
        if (!outbox.getRecipientUserId().equals(userId)) {
            throw new AppException(ApiErrorCode.UNAUTHORIZED);
        }
        
        outbox.markAsRead();
        outboxRepository.save(outbox);
    }

    @Override
    public void markAllAsRead(Long userId) {
        outboxRepository.markAllAsRead(userId);
    }
}
