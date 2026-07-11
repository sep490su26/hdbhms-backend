package com.sep490.hdbhms.changerequest.application.service;

import com.sep490.hdbhms.changerequest.application.port.in.command.ApproveRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.command.RejectRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.usecase.ChangeRequestUseCase;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestDecisionHandler;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;
import com.sep490.hdbhms.permissiongrant.application.service.PermissionGrantService;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChangeRequestService implements ChangeRequestUseCase {
    ChangeRequestRepository repository;
    List<ChangeRequestDecisionHandler> decisionHandlers;
    NotificationOutboxRepository notificationOutboxRepository;
    PermissionGrantService permissionGrantService;

    @Override
    @Transactional
    public void approveRequest(ApproveRequestCommand command) {
        ChangeRequest request = repository.findById(command.requestId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        request.approve(command.managerId());
        if (request.getRequestType() == RequestType.TENANT_PROFILE_ACCESS) {
            permissionGrantService.grantTenantProfileAccess(request, command.managerId(), command.durationCode());
        }
        repository.save(request);
        dispatchApproved(request, command.managerId());
        notifyTenantProfileAccessResolved(request, true);
    }

    @Override
    @Transactional
    public void rejectRequest(RejectRequestCommand command) {
        ChangeRequest request = repository.findById(command.requestId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        request.reject(command.managerId(), command.resolutionNote());
        repository.save(request);
        dispatchRejected(request, command.managerId(), command.resolutionNote());
        notifyTenantProfileAccessResolved(request, false);
    }

    private void dispatchApproved(ChangeRequest request, Long managerId) {
        decisionHandlers.stream()
                .filter(handler -> handler.supports(request.getRequestType()))
                .forEach(handler -> handler.onApproved(request, managerId));
    }

    private void dispatchRejected(ChangeRequest request, Long managerId, String resolutionNote) {
        decisionHandlers.stream()
                .filter(handler -> handler.supports(request.getRequestType()))
                .forEach(handler -> handler.onRejected(request, managerId, resolutionNote));
    }

    private void notifyTenantProfileAccessResolved(ChangeRequest request, boolean approved) {
        if (request.getRequestType() != RequestType.TENANT_PROFILE_ACCESS || request.getRequesterId() == null) {
            return;
        }
        notificationOutboxRepository.save(NotificationOutbox.builder()
                .eventType(approved ? "TENANT_PROFILE_ACCESS_APPROVED" : "TENANT_PROFILE_ACCESS_REJECTED")
                .targetType("TENANT_PROFILE")
                .targetId(request.getTargetId())
                .recipientUserId(request.getRequesterId())
                .channel(NotificationChannel.WEB)
                .title(approved ? "Đã được duyệt xem hồ sơ" : "Yêu cầu xem hồ sơ bị từ chối")
                .body(approved
                        ? "Chủ trọ đã duyệt quyền xem hồ sơ khách thuê."
                        : "Chủ trọ đã từ chối yêu cầu xem hồ sơ khách thuê.")
                .payload(request.getRequestPayload())
                .status(OutboxStatus.PENDING)
                .maxRetries(3)
                .isRead(false)
                .scheduledAt(LocalDateTime.now())
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
    }
}
