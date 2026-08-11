package com.sep490.hdbhms.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.notification.application.port.in.usecase.SendNotificationUseCase;
import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class NotificationEventConsumerTest {

    @Test
    void malformedPayloadIsRethrownForKafkaRetry() {
        NotificationEventConsumer consumer = new NotificationEventConsumer(
                mock(SendNotificationUseCase.class),
                new ObjectMapper()
        );

        assertThrows(AppException.class, () -> consumer.handle("not-json"));
    }
}
