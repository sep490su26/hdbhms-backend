package com.sep490.hdbhms.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.booking.domain.event.DepositInformationNotificationRequestedEvent;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.application.service.NotificationTemplateManagementService;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.infrastructure.processor.NotificationOutboxProcessor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepositInformationNotificationListenerTest {

    @Test
    void prefersEmailWhenGuestEmailIsValid() {
        NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
        NotificationOutboxProcessor processor = mock(NotificationOutboxProcessor.class);
        NotificationTemplateManagementService templates = mock(NotificationTemplateManagementService.class);
        DepositInformationNotificationListener listener =
                new DepositInformationNotificationListener(repository, templates, processor, new ObjectMapper());
        NotificationOutbox saved = NotificationOutbox.builder().id(10L).build();
        when(repository.save(any(NotificationOutbox.class))).thenReturn(saved);

        listener.handle(event("guest@example.com", "0900000000"));

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(repository).save(captor.capture());
        assertEquals(com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel.EMAIL,
                captor.getValue().getChannel());
        verify(processor).process(10L);
    }

    @Test
    void fallsBackToSmsWhenEmailIsSynthetic() {
        NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
        NotificationOutboxProcessor processor = mock(NotificationOutboxProcessor.class);
        NotificationTemplateManagementService templates = mock(NotificationTemplateManagementService.class);
        DepositInformationNotificationListener listener =
                new DepositInformationNotificationListener(repository, templates, processor, new ObjectMapper());
        NotificationOutbox saved = NotificationOutbox.builder().id(11L).build();
        when(repository.save(any(NotificationOutbox.class))).thenReturn(saved);

        listener.handle(event("guest@tenant.hdbhms.local", "0900000000"));

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(repository).save(captor.capture());
        assertEquals(com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel.SMS,
                captor.getValue().getChannel());
        verify(processor).process(11L);
    }

    private DepositInformationNotificationRequestedEvent event(String email, String phone) {
        return new DepositInformationNotificationRequestedEvent(
                7L,
                null,
                null,
                email,
                "Guest Test",
                phone,
                "Deposit confirmed",
                "Deposit details",
                Map.of("status", "PAID")
        );
    }
}
