package com.sep490.hdbhms.notification.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.notification.application.port.out.NotificationDeliveryRepository;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.application.port.out.NotificationTemplateRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationDelivery;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.model.NotificationTemplate;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;
import com.sep490.hdbhms.notification.domain.value_objects.TemplateStatus;
import com.sep490.hdbhms.shared.event.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceTest {

    @Test
    void roomTransferDefaultTemplatesFillMissingDbChannels() {
        RecordingOutboxRepository outboxRepository = new RecordingOutboxRepository();
        NotificationService service = new NotificationService(
                new FixedTemplateRepository(List.of(NotificationTemplate.builder()
                        .templateKey("ROOM_TRANSFER_MANAGER_ACTION_REQUIRED")
                        .channel(NotificationChannel.WEB)
                        .titleTemplate("DB title")
                        .bodyTemplate("DB [[${requestCode}]]")
                        .status(TemplateStatus.ACTIVE)
                .build())),
                outboxRepository,
                new FailingDeliveryRepository(),
                new ObjectMapper()
        );
        service.init();

        service.queueNotification(NotificationEvent.builder()
                .eventType("ROOM_TRANSFER_MANAGER_ACTION_REQUIRED")
                .userId(15L)
                .targetType("ROOM_TRANSFER")
                .targetId(9L)
                .data(Map.of(
                        "requestCode", "TR-1",
                        "actionLabel", "Tải bản hợp đồng đã ký trực tiếp",
                        "oldRoomName", "Phòng 104",
                        "targetRoomName", "Phòng 206",
                        "requestedTransferDate", "2026-07-07"
                ))
                .build());

        assertEquals(NotificationChannel.values().length, outboxRepository.saved.size());
        NotificationOutbox web = outboxRepository.findSavedByChannel(NotificationChannel.WEB);
        NotificationOutbox push = outboxRepository.findSavedByChannel(NotificationChannel.PUSH);
        assertEquals("DB title", web.getTitle());
        assertEquals("DB TR-1", web.getBody());
        assertEquals("Yêu cầu chuyển phòng cần xử lý", push.getTitle());
        assertTrue(push.getBody().contains("TR-1"));
        assertTrue(push.getPayload().contains("\"requestCode\":\"TR-1\""));
    }

    @Test
    void legacyRoomTransferSeedDoesNotOverrideDefaultVietnameseTemplate() {
        RecordingOutboxRepository outboxRepository = new RecordingOutboxRepository();
        NotificationService service = new NotificationService(
                new FixedTemplateRepository(List.of(NotificationTemplate.builder()
                        .templateKey("ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED")
                        .channel(NotificationChannel.WEB)
                        .titleTemplate("Xac nhan holder moi")
                        .bodyTemplate("Ban duoc de cu lam holder moi cho phong [[${oldRoomName}]].")
                        .status(TemplateStatus.ACTIVE)
                .build())),
                outboxRepository,
                new FailingDeliveryRepository(),
                new ObjectMapper()
        );
        service.init();

        service.queueNotification(NotificationEvent.builder()
                .eventType("ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED")
                .userId(15L)
                .targetType("ROOM_TRANSFER")
                .targetId(9L)
                .data(Map.of(
                        "requestCode", "TR-2",
                        "oldRoomName", "Phòng 104"
                ))
                .build());

        NotificationOutbox web = outboxRepository.findSavedByChannel(NotificationChannel.WEB);
        assertEquals("Bạn được đề cử làm người đại diện phòng", web.getTitle());
        assertTrue(web.getBody().contains("Yêu cầu chuyển phòng TR-2"));
    }

    @Test
    void markAsReadStoresReadAtAndMarksDeliveryReadAt() {
        ReadTrackingOutboxRepository outboxRepository = new ReadTrackingOutboxRepository(
                NotificationOutbox.builder()
                        .id(21L)
                        .recipientUserId(15L)
                        .isRead(false)
                        .build()
        );
        RecordingDeliveryRepository deliveryRepository = new RecordingDeliveryRepository();
        NotificationService service = new NotificationService(
                new FixedTemplateRepository(List.of()),
                outboxRepository,
                deliveryRepository,
                new ObjectMapper()
        );

        service.markAsRead(21L, 15L);

        assertTrue(outboxRepository.saved.getIsRead());
        assertNotNull(outboxRepository.saved.getReadAt());
        assertEquals(21L, deliveryRepository.readOutboxId);
        assertEquals(outboxRepository.saved.getReadAt(), deliveryRepository.readAt);
    }

    @Test
    void markTargetAsReadMarksOutboxAndDeliveryRowsForTarget() {
        TargetReadTrackingOutboxRepository outboxRepository = new TargetReadTrackingOutboxRepository();
        RecordingDeliveryRepository deliveryRepository = new RecordingDeliveryRepository();
        NotificationService service = new NotificationService(
                new FixedTemplateRepository(List.of()),
                outboxRepository,
                deliveryRepository,
                new ObjectMapper()
        );

        service.markTargetAsRead(15L, " room_transfer ", 9L);

        assertEquals(15L, outboxRepository.userId);
        assertEquals("ROOM_TRANSFER", outboxRepository.targetType);
        assertEquals(9L, outboxRepository.targetId);
        assertNotNull(outboxRepository.readAt);
        assertEquals(15L, deliveryRepository.targetReadUserId);
        assertEquals("ROOM_TRANSFER", deliveryRepository.targetReadType);
        assertEquals(9L, deliveryRepository.targetReadTargetId);
        assertEquals(outboxRepository.readAt, deliveryRepository.targetReadAt);
    }

    private record FixedTemplateRepository(
            List<NotificationTemplate> templates
    ) implements NotificationTemplateRepository {
        @Override
        public NotificationTemplate save(NotificationTemplate notificationTemplate) {
            throw unexpected("NotificationTemplateRepository.save");
        }

        @Override
        public List<NotificationTemplate> findByTemplateKeyAndStatus(String templateKey, TemplateStatus status) {
            return templates.stream()
                    .filter(template -> templateKey.equals(template.getTemplateKey()))
                    .filter(template -> status == template.getStatus())
                    .toList();
        }
    }

    private static final class RecordingOutboxRepository implements NotificationOutboxRepository {
        private final List<NotificationOutbox> saved = new ArrayList<>();

        @Override
        public NotificationOutbox save(NotificationOutbox notificationOutbox) {
            saved.add(notificationOutbox);
            return notificationOutbox;
        }

        private NotificationOutbox findSavedByChannel(NotificationChannel channel) {
            return saved.stream()
                    .filter(outbox -> outbox.getChannel() == channel)
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public Optional<NotificationOutbox> findById(Long id) {
            throw unexpected("NotificationOutboxRepository.findById");
        }

        @Override
        public List<NotificationOutbox> findByStatusAndNextRetryAtBefore(
                OutboxStatus outboxStatus,
                LocalDateTime localDateTime
        ) {
            throw unexpected("NotificationOutboxRepository.findByStatusAndNextRetryAtBefore");
        }

        @Override
        public Page<NotificationOutbox> findByRecipientUserIdAndChannelOrderByCreatedAtDesc(
                Long userId,
                NotificationChannel channel,
                Pageable pageable
        ) {
            throw unexpected("NotificationOutboxRepository.findByRecipientUserIdAndChannelOrderByCreatedAtDesc");
        }

        @Override
        public List<NotificationOutbox> findNextNotificationsCursor(
                Long userId,
                NotificationChannel channel,
                long after,
                int limit
        ) {
            throw unexpected("NotificationOutboxRepository.findNextNotificationsCursor");
        }

        @Override
        public long countByRecipientUserIdAndIsReadFalse(Long userId) {
            throw unexpected("NotificationOutboxRepository.countByRecipientUserIdAndIsReadFalse");
        }

        @Override
        public void markAllAsRead(Long userId, LocalDateTime readAt) {
            throw unexpected("NotificationOutboxRepository.markAllAsRead");
        }

        @Override
        public void markTargetAsRead(Long userId, String targetType, Long targetId, LocalDateTime readAt) {
            throw unexpected("NotificationOutboxRepository.markTargetAsRead");
        }

        @Override
        public boolean markAsProcessing(Long id) {
            throw unexpected("NotificationOutboxRepository.markAsProcessing");
        }
    }

    private static final class ReadTrackingOutboxRepository implements NotificationOutboxRepository {
        private final NotificationOutbox outbox;
        private NotificationOutbox saved;

        private ReadTrackingOutboxRepository(NotificationOutbox outbox) {
            this.outbox = outbox;
        }

        @Override
        public NotificationOutbox save(NotificationOutbox notificationOutbox) {
            saved = notificationOutbox;
            return notificationOutbox;
        }

        @Override
        public Optional<NotificationOutbox> findById(Long id) {
            return outbox.getId().equals(id) ? Optional.of(outbox) : Optional.empty();
        }

        @Override
        public List<NotificationOutbox> findByStatusAndNextRetryAtBefore(
                OutboxStatus outboxStatus,
                LocalDateTime localDateTime
        ) {
            throw unexpected("NotificationOutboxRepository.findByStatusAndNextRetryAtBefore");
        }

        @Override
        public Page<NotificationOutbox> findByRecipientUserIdAndChannelOrderByCreatedAtDesc(
                Long userId,
                NotificationChannel channel,
                Pageable pageable
        ) {
            throw unexpected("NotificationOutboxRepository.findByRecipientUserIdAndChannelOrderByCreatedAtDesc");
        }

        @Override
        public List<NotificationOutbox> findNextNotificationsCursor(
                Long userId,
                NotificationChannel channel,
                long after,
                int limit
        ) {
            throw unexpected("NotificationOutboxRepository.findNextNotificationsCursor");
        }

        @Override
        public long countByRecipientUserIdAndIsReadFalse(Long userId) {
            throw unexpected("NotificationOutboxRepository.countByRecipientUserIdAndIsReadFalse");
        }

        @Override
        public void markAllAsRead(Long userId, LocalDateTime readAt) {
            throw unexpected("NotificationOutboxRepository.markAllAsRead");
        }

        @Override
        public void markTargetAsRead(Long userId, String targetType, Long targetId, LocalDateTime readAt) {
            throw unexpected("NotificationOutboxRepository.markTargetAsRead");
        }

        @Override
        public boolean markAsProcessing(Long id) {
            throw unexpected("NotificationOutboxRepository.markAsProcessing");
        }
    }

    private static final class TargetReadTrackingOutboxRepository implements NotificationOutboxRepository {
        private Long userId;
        private String targetType;
        private Long targetId;
        private LocalDateTime readAt;

        @Override
        public NotificationOutbox save(NotificationOutbox notificationOutbox) {
            throw unexpected("NotificationOutboxRepository.save");
        }

        @Override
        public Optional<NotificationOutbox> findById(Long id) {
            throw unexpected("NotificationOutboxRepository.findById");
        }

        @Override
        public List<NotificationOutbox> findByStatusAndNextRetryAtBefore(
                OutboxStatus outboxStatus,
                LocalDateTime localDateTime
        ) {
            throw unexpected("NotificationOutboxRepository.findByStatusAndNextRetryAtBefore");
        }

        @Override
        public Page<NotificationOutbox> findByRecipientUserIdAndChannelOrderByCreatedAtDesc(
                Long userId,
                NotificationChannel channel,
                Pageable pageable
        ) {
            throw unexpected("NotificationOutboxRepository.findByRecipientUserIdAndChannelOrderByCreatedAtDesc");
        }

        @Override
        public List<NotificationOutbox> findNextNotificationsCursor(
                Long userId,
                NotificationChannel channel,
                long after,
                int limit
        ) {
            throw unexpected("NotificationOutboxRepository.findNextNotificationsCursor");
        }

        @Override
        public long countByRecipientUserIdAndIsReadFalse(Long userId) {
            throw unexpected("NotificationOutboxRepository.countByRecipientUserIdAndIsReadFalse");
        }

        @Override
        public void markAllAsRead(Long userId, LocalDateTime readAt) {
            throw unexpected("NotificationOutboxRepository.markAllAsRead");
        }

        @Override
        public void markTargetAsRead(Long userId, String targetType, Long targetId, LocalDateTime readAt) {
            this.userId = userId;
            this.targetType = targetType;
            this.targetId = targetId;
            this.readAt = readAt;
        }

        @Override
        public boolean markAsProcessing(Long id) {
            throw unexpected("NotificationOutboxRepository.markAsProcessing");
        }
    }

    private static final class RecordingDeliveryRepository implements NotificationDeliveryRepository {
        private Long readOutboxId;
        private LocalDateTime readAt;
        private Long targetReadUserId;
        private String targetReadType;
        private Long targetReadTargetId;
        private LocalDateTime targetReadAt;

        @Override
        public NotificationDelivery save(NotificationDelivery notificationDelivery) {
            throw unexpected("NotificationDeliveryRepository.save");
        }

        @Override
        public void markReadByOutboxId(Long outboxId, LocalDateTime readAt) {
            this.readOutboxId = outboxId;
            this.readAt = readAt;
        }

        @Override
        public void markReadByRecipientUserId(Long userId, LocalDateTime readAt) {
            throw unexpected("NotificationDeliveryRepository.markReadByRecipientUserId");
        }

        @Override
        public void markReadByRecipientUserIdAndTarget(
                Long userId,
                String targetType,
                Long targetId,
                LocalDateTime readAt
        ) {
            this.targetReadUserId = userId;
            this.targetReadType = targetType;
            this.targetReadTargetId = targetId;
            this.targetReadAt = readAt;
        }
    }

    private record FailingDeliveryRepository() implements NotificationDeliveryRepository {
        @Override
        public NotificationDelivery save(NotificationDelivery notificationDelivery) {
            throw unexpected("NotificationDeliveryRepository.save");
        }

        @Override
        public void markReadByOutboxId(Long outboxId, LocalDateTime readAt) {
            throw unexpected("NotificationDeliveryRepository.markReadByOutboxId");
        }

        @Override
        public void markReadByRecipientUserId(Long userId, LocalDateTime readAt) {
            throw unexpected("NotificationDeliveryRepository.markReadByRecipientUserId");
        }

        @Override
        public void markReadByRecipientUserIdAndTarget(
                Long userId,
                String targetType,
                Long targetId,
                LocalDateTime readAt
        ) {
            throw unexpected("NotificationDeliveryRepository.markReadByRecipientUserIdAndTarget");
        }
    }

    private static UnsupportedOperationException unexpected(String operation) {
        return new UnsupportedOperationException(operation + " should not be called");
    }
}
