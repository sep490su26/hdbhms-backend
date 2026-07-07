package com.sep490.hdbhms.notification.infrastructure.dispatcher;

import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.valueObjects.OutboxStatus;
import com.sep490.hdbhms.notification.infrastructure.processor.NotificationOutboxProcessor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationOutboxDispatcher {

    NotificationOutboxRepository notificationOutboxRepository;
    NotificationOutboxProcessor processor;

    @Scheduled(fixedDelayString = "${app.notification.outbox.poll-interval-ms:60000}")
    public void dispatch() {
        List<NotificationOutbox> pending = notificationOutboxRepository
                .findByStatusAndNextRetryAtBefore(OutboxStatus.PENDING, LocalDateTime.now());

        if (pending.isEmpty()) return;

        log.info("Dispatching {} pending notification outbox entries", pending.size());

        for (NotificationOutbox outbox : pending) {
            try {
                processor.process(outbox.getId());
            } catch (Exception e) {
                log.error("Failed to process outbox id={} channel={}: {}",
                        outbox.getId(), outbox.getChannel(), e.getMessage(), e);
            }
        }
    }
}
