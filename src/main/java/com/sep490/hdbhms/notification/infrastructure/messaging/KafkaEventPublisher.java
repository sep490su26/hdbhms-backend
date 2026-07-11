package com.sep490.hdbhms.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.occupancy.domain.event.RoomTransferManagerActionRequiredEvent;
import com.sep490.hdbhms.occupancy.domain.event.RoomTransferTargetHolderApprovalRequestedEvent;
import com.sep490.hdbhms.occupancy.domain.event.RoomTransferHolderNominationRequestedEvent;
import com.sep490.hdbhms.shared.event.NotificationEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KafkaEventPublisher {
    KafkaTemplate<String, String> kafkaTemplate;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDomainEvent(Object domainEvent) {
        switch (domainEvent) {
            case RoomTransferHolderNominationRequestedEvent event ->
                    publishRoomTransferHolderNominationRequested(event);
            case RoomTransferTargetHolderApprovalRequestedEvent event ->
                    publishRoomTransferTargetHolderApprovalRequested(event);
            case RoomTransferManagerActionRequiredEvent event ->
                    publishRoomTransferManagerActionRequired(event);
            default -> {
            }
        }
    }

    private void publishRoomTransferHolderNominationRequested(RoomTransferHolderNominationRequestedEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestId", event.requestId());
        data.put("requestCode", event.requestCode());
        data.put("nominatorUserId", event.nominatorUserId());
        data.put("nominatedHolderProfileId", event.nominatedHolderProfileId());
        data.put("oldRoomId", event.oldRoomId());
        data.put("targetRoomId", event.targetRoomId());
        data.put("oldRoomName", event.oldRoomName());
        data.put("targetRoomName", event.targetRoomName());
        data.put("requestedTransferDate", event.requestedTransferDate());
        data.put("expectedTransferDate", event.requestedTransferDate());

        publishNotification(NotificationEvent.builder()
                .eventType("ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED")
                .userId(event.nominatedHolderUserId())
                .targetType("ROOM_TRANSFER")
                .targetId(event.requestId())
                .data(data)
                .build());
    }

    private void publishRoomTransferTargetHolderApprovalRequested(RoomTransferTargetHolderApprovalRequestedEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestId", event.requestId());
        data.put("requestCode", event.requestCode());
        data.put("requesterUserId", event.requesterUserId());
        data.put("oldRoomId", event.oldRoomId());
        data.put("targetRoomId", event.targetRoomId());
        data.put("oldRoomName", event.oldRoomName());
        data.put("targetRoomName", event.targetRoomName());
        data.put("targetContractId", event.targetContractId());
        data.put("requestedTransferDate", event.requestedTransferDate());
        data.put("expectedTransferDate", event.requestedTransferDate());

        publishNotification(NotificationEvent.builder()
                .eventType("ROOM_TRANSFER_TARGET_HOLDER_APPROVAL_REQUESTED")
                .userId(event.targetHolderUserId())
                .targetType("ROOM_TRANSFER")
                .targetId(event.requestId())
                .data(data)
                .build());
    }

    private void publishRoomTransferManagerActionRequired(RoomTransferManagerActionRequiredEvent event) {
        if (event.managerUserIds() == null || event.managerUserIds().isEmpty()) {
            return;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestId", event.requestId());
        data.put("requestCode", event.requestCode());
        data.put("actionType", event.actionType());
        data.put("actionLabel", event.actionLabel());
        data.put("oldRoomId", event.oldRoomId());
        data.put("targetRoomId", event.targetRoomId());
        data.put("oldRoomName", event.oldRoomName());
        data.put("targetRoomName", event.targetRoomName());
        data.put("requestedTransferDate", event.requestedTransferDate());
        data.put("expectedTransferDate", event.requestedTransferDate());

        for (Long managerUserId : event.managerUserIds()) {
            if (managerUserId == null) {
                continue;
            }
            publishNotification(NotificationEvent.builder()
                    .eventType("ROOM_TRANSFER_MANAGER_ACTION_REQUIRED")
                    .userId(managerUserId)
                    .targetType("ROOM_TRANSFER")
                    .targetId(event.requestId())
                    .data(data)
                    .build());
        }
    }

    private void publishNotification(NotificationEvent event) {
        try {
            kafkaTemplate.send("notification-events", objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            log.error(
                    "Failed to publish notification event type={} targetType={} targetId={}",
                    event.getEventType(),
                    event.getTargetType(),
                    event.getTargetId(),
                    exception
            );
        }
    }
}
