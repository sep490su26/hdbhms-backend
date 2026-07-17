package com.sep490.hdbhms.notification.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;
import com.sep490.hdbhms.notification.infrastructure.web.dto.request.SendNotificationBroadcastRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationBroadcastServiceTest {

    @Test
    void sendCreatesOutboxPerRecipientAndChannel() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(List.of(10L, 20L));
        RecordingOutboxRepository outboxRepository = new RecordingOutboxRepository();
        NotificationBroadcastService service = new NotificationBroadcastService(
                jdbcTemplate,
                outboxRepository,
                new ObjectMapper()
        );

        NotificationBroadcastService.BroadcastResult result = service.send(
                SendNotificationBroadcastRequest.builder()
                        .scopeType("PROPERTY")
                        .scopeIds(List.of(1L))
                        .roles(List.of("TENANT", "MANAGER"))
                        .channels(List.of("WEB", "PUSH"))
                        .title("Thông báo bảo trì")
                        .body("Tạm ngưng nước từ 9h.")
                        .build(),
                99L
        );

        assertEquals(2, result.recipientCount());
        assertEquals(4, result.outboxCount());
        assertEquals(4, outboxRepository.saved.size());
        assertTrue(outboxRepository.saved.stream().allMatch(item -> "BROADCAST_ANNOUNCEMENT".equals(item.getEventType())));
        assertTrue(outboxRepository.saved.stream().anyMatch(item -> item.getChannel() == NotificationChannel.WEB));
        assertTrue(outboxRepository.saved.stream().anyMatch(item -> item.getChannel() == NotificationChannel.PUSH));
    }

    @Test
    void previewDoesNotSaveOutbox() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(List.of(10L));
        RecordingOutboxRepository outboxRepository = new RecordingOutboxRepository();
        NotificationBroadcastService service = new NotificationBroadcastService(
                jdbcTemplate,
                outboxRepository,
                new ObjectMapper()
        );

        NotificationBroadcastService.BroadcastResult result = service.previewRecipients(
                SendNotificationBroadcastRequest.builder()
                        .scopeType("SYSTEM")
                        .roles(List.of("TENANT"))
                        .channels(List.of("PUSH"))
                        .build()
        );

        assertEquals(1, result.recipientCount());
        assertEquals(1, result.outboxCount());
        verify(jdbcTemplate).queryForList(anyString(), any(MapSqlParameterSource.class), eq(Long.class));
        assertTrue(outboxRepository.saved.isEmpty());
    }

    @Test
    void systemWebWithoutExplicitRolesTargetsOnlyWebRoles() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        List<List<String>> queriedRoles = new ArrayList<>();
        when(jdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenAnswer(invocation -> {
                    MapSqlParameterSource params = invocation.getArgument(1);
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) params.getValue("roles");
                    queriedRoles.add(roles);
                    return List.of(10L, 20L);
                });
        RecordingOutboxRepository outboxRepository = new RecordingOutboxRepository();
        NotificationBroadcastService service = new NotificationBroadcastService(
                jdbcTemplate,
                outboxRepository,
                new ObjectMapper()
        );

        NotificationBroadcastService.BroadcastResult result = service.send(
                SendNotificationBroadcastRequest.builder()
                        .scopeType("SYSTEM")
                        .roles(List.of())
                        .channels(List.of("WEB"))
                        .title("Thong bao web")
                        .body("Noi dung")
                        .build(),
                99L
        );

        assertEquals(List.of("LEAD", "MANAGER", "ACCOUNTANT", "OWNER"), result.roles());
        assertEquals(2, result.recipientCount());
        assertEquals(2, result.outboxCount());
        assertEquals(1, queriedRoles.size());
        assertFalse(queriedRoles.getFirst().contains("TENANT"));
        assertTrue(outboxRepository.saved.stream().allMatch(item -> item.getChannel() == NotificationChannel.WEB));
    }

    private static final class RecordingOutboxRepository implements NotificationOutboxRepository {
        private final List<NotificationOutbox> saved = new ArrayList<>();

        @Override
        public NotificationOutbox save(NotificationOutbox notificationOutbox) {
            saved.add(notificationOutbox);
            return notificationOutbox;
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
        public long countByRecipientUserIdAndChannelAndIsReadFalse(Long userId, NotificationChannel channel) {
            throw unexpected("NotificationOutboxRepository.countByRecipientUserIdAndChannelAndIsReadFalse");
        }

        @Override
        public void markAllAsRead(Long userId, NotificationChannel channel) {
            throw unexpected("NotificationOutboxRepository.markAllAsRead");
        }

        @Override
        public void markAllAsRead(Long userId, NotificationChannel channel, LocalDateTime readAt) {
            throw unexpected("NotificationOutboxRepository.markAllAsRead");
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

    private static UnsupportedOperationException unexpected(String operation) {
        return new UnsupportedOperationException(operation + " should not be called");
    }
}
