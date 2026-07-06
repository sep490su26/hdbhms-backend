package com.sep490.hdbhms.notification.infrastructure.web.controller;

import com.sep490.hdbhms.notification.application.port.in.query.NotificationQueryUseCase;
import com.sep490.hdbhms.notification.application.port.in.usecase.ManageDeviceTokenUseCase;
import com.sep490.hdbhms.notification.application.port.in.usecase.ManageNotificationUseCase;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.valueObjects.NotificationChannel;
import com.sep490.hdbhms.notification.infrastructure.web.dto.request.RegisterDeviceTokenRequest;
import com.sep490.hdbhms.notification.infrastructure.web.dto.response.NotificationResponse;
import com.sep490.hdbhms.notification.infrastructure.web.dto.response.NotificationScrollResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

    NotificationQueryUseCase notificationQueryUseCase;
    ManageNotificationUseCase manageNotificationUseCase;
    ManageDeviceTokenUseCase manageDeviceTokenUseCase;

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@RequestHeader("X-Client-Type") String clientType) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        NotificationChannel channel = resolveChannel(clientType);
        return ApiResponse.<Long>builder().data(notificationQueryUseCase.getUnreadCount(userId, channel)).build();
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getNotificationsWeb(
            @RequestHeader("X-Client-Type") String clientType,
            @PageableDefault(size = 20) Pageable pageable) {

        Long userId = AuthUtils.getCurrentAuthenticationId();
        NotificationChannel channel = resolveChannel(clientType);

        Page<NotificationOutbox> page = notificationQueryUseCase.getNotificationsWeb(userId, channel, pageable);
        Page<NotificationResponse> responsePage = page.map(this::toResponse);

        return ApiResponse.<PageResponse<NotificationResponse>>builder().data(PageResponse.fromPageToPageResponse(responsePage)).build();
    }

    @PostMapping("/me/mobile-device-tokens")
    public ApiResponse<Void> registerDeviceToken(
            @RequestHeader("X-Client-Type") String clientType,
            @RequestBody RegisterDeviceTokenRequest request) {

        if (!"mobile".equalsIgnoreCase(clientType)) {
            throw new AppException(ApiErrorCode.UNAUTHORIZED);
        }

        Long userId = AuthUtils.getCurrentAuthenticationId();
        manageDeviceTokenUseCase.registerDeviceToken(userId, request.getToken(), request.getPlatform());
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/scroll")
    public ApiResponse<NotificationScrollResponse> getNotificationsMobile(
            @RequestHeader("X-Client-Type") String clientType,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") long after) {

        Long userId = AuthUtils.getCurrentAuthenticationId();
        NotificationChannel channel = resolveChannel(clientType);

        List<NotificationOutbox> rows = notificationQueryUseCase.getNotificationsMobile(userId, channel, after, limit + 1);

        boolean hasMore = rows.size() > limit;
        if (hasMore) {
            rows = rows.subList(0, limit);
        }

        List<NotificationResponse> items = rows.stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.<NotificationScrollResponse>builder().data(new NotificationScrollResponse(hasMore, items)).build();
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        manageNotificationUseCase.markAsRead(id, userId);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@RequestHeader("X-Client-Type") String clientType) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        NotificationChannel channel = resolveChannel(clientType);
        manageNotificationUseCase.markAllAsRead(userId, channel);
        return ApiResponse.<Void>builder().build();
    }

    private NotificationResponse toResponse(NotificationOutbox domain) {
        return NotificationResponse.builder()
                .id(domain.getId())
                .title(domain.getTitle())
                .body(domain.getBody())
                .eventType(domain.getEventType())
                .targetType(domain.getTargetType())
                .targetId(domain.getTargetId())
                .createdAt(domain.getCreatedAt())
                .isRead(domain.getIsRead())
                .build();
    }

    private NotificationChannel resolveChannel(String clientType) {
        if ("mobile".equalsIgnoreCase(clientType)) {
            return NotificationChannel.PUSH;
        }
        return NotificationChannel.WEB;
    }
}
