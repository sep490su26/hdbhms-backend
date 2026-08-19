package com.sep490.hdbhms.notification.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class NotificationOutboxPersistenceMapperTest {

    private final NotificationOutboxPersistenceMapper mapper =
            new NotificationOutboxPersistenceMapper(mock(JpaUserRepository.class));

    @Test
    void allowsGuestEmailWithoutRecipientUser() {
        NotificationOutbox outbox = NotificationOutbox.builder()
                .channel(NotificationChannel.EMAIL)
                .recipientEmail("guest@example.com")
                .build();

        assertDoesNotThrow(() -> mapper.toEntity(outbox));
    }

    @Test
    void rejectsInternalNotificationWithoutRecipientUser() {
        NotificationOutbox outbox = NotificationOutbox.builder()
                .channel(NotificationChannel.IN_APP)
                .build();

        assertThrows(IllegalStateException.class, () -> mapper.toEntity(outbox));
    }
}
