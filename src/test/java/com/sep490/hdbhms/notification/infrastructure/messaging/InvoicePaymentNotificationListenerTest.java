package com.sep490.hdbhms.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.domain.event.InvoicePaymentNotificationRequestedEvent;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.application.service.NotificationTemplateManagementService;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.infrastructure.processor.NotificationOutboxProcessor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoicePaymentNotificationListenerTest {

    @Test
    void prefersEmailWhenTenantEmailIsValid() {
        NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
        NotificationOutboxProcessor processor = mock(NotificationOutboxProcessor.class);
        NotificationTemplateManagementService templates = mock(NotificationTemplateManagementService.class);
        InvoicePaymentNotificationListener listener =
                new InvoicePaymentNotificationListener(repository, templates, processor, new ObjectMapper());
        NotificationOutbox saved = NotificationOutbox.builder().id(10L).build();
        when(repository.save(any(NotificationOutbox.class))).thenReturn(saved);

        listener.handle(event("tenant@example.com", "0900000000"));

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(repository).save(captor.capture());
        assertEquals(NotificationChannel.EMAIL, captor.getValue().getChannel());
        verify(processor).process(10L);
    }

    @Test
    void fallsBackToSmsWhenTenantEmailIsSynthetic() {
        NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
        NotificationOutboxProcessor processor = mock(NotificationOutboxProcessor.class);
        NotificationTemplateManagementService templates = mock(NotificationTemplateManagementService.class);
        InvoicePaymentNotificationListener listener =
                new InvoicePaymentNotificationListener(repository, templates, processor, new ObjectMapper());
        NotificationOutbox saved = NotificationOutbox.builder().id(11L).build();
        when(repository.save(any(NotificationOutbox.class))).thenReturn(saved);

        listener.handle(event("tenant@tenant.hdbhms.local", "0900000000"));

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(repository).save(captor.capture());
        assertEquals(NotificationChannel.SMS, captor.getValue().getChannel());
        verify(processor).process(11L);
    }

    @Test
    void keepsSpecializedInvoiceEventTypeInOutbox() {
        NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
        NotificationOutboxProcessor processor = mock(NotificationOutboxProcessor.class);
        NotificationTemplateManagementService templates = mock(NotificationTemplateManagementService.class);
        InvoicePaymentNotificationListener listener =
                new InvoicePaymentNotificationListener(repository, templates, processor, new ObjectMapper());
        NotificationOutbox saved = NotificationOutbox.builder().id(12L).build();
        when(repository.save(any(NotificationOutbox.class))).thenReturn(saved);

        listener.handle(event("FINAL_SETTLEMENT_PAYMENT_SUCCESS", "tenant@example.com", "0900000000"));

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(repository).save(captor.capture());
        assertEquals("FINAL_SETTLEMENT_PAYMENT_SUCCESS", captor.getValue().getEventType());
        verify(templates).preview(
                eq("FINAL_SETTLEMENT_PAYMENT_SUCCESS"),
                eq(NotificationChannel.EMAIL),
                isNull(),
                isNull(),
                anyMap()
        );
    }

    private InvoicePaymentNotificationRequestedEvent event(String email, String phone) {
        return event("INVOICE_PAYMENT_SUCCESS", email, phone);
    }

    private InvoicePaymentNotificationRequestedEvent event(String eventType, String email, String phone) {
        return new InvoicePaymentNotificationRequestedEvent(
                eventType,
                7L,
                21L,
                email,
                phone,
                "Thanh toán thành công",
                "Hóa đơn đã được thanh toán",
                Map.of("invoiceCode", "INV-2026-07-001", "paymentAmount", 100000L)
        );
    }
}
