package com.sep490.hdbhms.notification.infrastructure.web.controller;

import com.sep490.hdbhms.notification.application.service.NotificationBroadcastService;
import com.sep490.hdbhms.notification.infrastructure.web.dto.request.SendNotificationBroadcastRequest;
import com.sep490.hdbhms.notification.infrastructure.web.dto.response.NotificationBroadcastResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification-broadcasts")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationBroadcastController {
    NotificationBroadcastService notificationBroadcastService;

    @PostMapping("/preview-recipients")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<NotificationBroadcastResponse> previewRecipients(
            @RequestBody(required = false) SendNotificationBroadcastRequest request
    ) {
        return ApiResponse.<NotificationBroadcastResponse>builder()
                .data(toResponse(notificationBroadcastService.previewRecipients(request)))
                .build();
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<NotificationBroadcastResponse> send(
            @RequestBody SendNotificationBroadcastRequest request
    ) {
        return ApiResponse.<NotificationBroadcastResponse>builder()
                .data(toResponse(notificationBroadcastService.send(request, AuthUtils.getCurrentAuthenticationId())))
                .build();
    }

    private NotificationBroadcastResponse toResponse(NotificationBroadcastService.BroadcastResult result) {
        return NotificationBroadcastResponse.builder()
                .scopeType(result.scopeType())
                .roles(result.roles())
                .channels(result.channels())
                .recipientCount(result.recipientCount())
                .outboxCount(result.outboxCount())
                .build();
    }
}
