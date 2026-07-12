package com.sep490.hdbhms.notification.infrastructure.web.controller;

import com.sep490.hdbhms.notification.application.service.NotificationTemplateDefaults;
import com.sep490.hdbhms.notification.application.service.NotificationTemplateManagementService;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.infrastructure.web.dto.request.PreviewNotificationTemplateRequest;
import com.sep490.hdbhms.notification.infrastructure.web.dto.request.UpdateNotificationTemplateRequest;
import com.sep490.hdbhms.notification.infrastructure.web.dto.response.NotificationTemplateDefinitionResponse;
import com.sep490.hdbhms.notification.infrastructure.web.dto.response.NotificationTemplateResponse;
import com.sep490.hdbhms.notification.infrastructure.web.dto.response.NotificationTemplateVariableResponse;
import com.sep490.hdbhms.notification.infrastructure.web.dto.response.PreviewNotificationTemplateResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationTemplateController {
    NotificationTemplateManagementService templateManagementService;

    @GetMapping("/notification-template-definitions")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<List<NotificationTemplateDefinitionResponse>> getDefinitions() {
        List<NotificationTemplateDefinitionResponse> responses = templateManagementService.getDefinitions()
                .stream()
                .map(this::toDefinitionResponse)
                .toList();
        return ApiResponse.<List<NotificationTemplateDefinitionResponse>>builder()
                .data(responses)
                .build();
    }

    @GetMapping("/notification-templates")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<List<NotificationTemplateResponse>> getTemplates(
            @RequestParam(required = false) String eventType
    ) {
        List<NotificationTemplateResponse> responses = templateManagementService.getEffectiveTemplates(eventType)
                .stream()
                .map(this::toTemplateResponse)
                .toList();
        return ApiResponse.<List<NotificationTemplateResponse>>builder()
                .data(responses)
                .build();
    }

    @PutMapping("/notification-templates/{eventType}/{channel}")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<NotificationTemplateResponse> upsertTemplate(
            @PathVariable String eventType,
            @PathVariable String channel,
            @RequestBody UpdateNotificationTemplateRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Request body is required");
        }

        NotificationTemplateManagementService.EffectiveTemplate template =
                templateManagementService.upsertCustomTemplate(
                        eventType,
                        resolveChannel(channel),
                        request.getTitleTemplate(),
                        request.getBodyTemplate(),
                        request.getStatus()
                );

        return ApiResponse.<NotificationTemplateResponse>builder()
                .data(toTemplateResponse(template))
                .build();
    }

    @PostMapping("/notification-templates/{eventType}/{channel}/reset")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<NotificationTemplateResponse> resetTemplate(
            @PathVariable String eventType,
            @PathVariable String channel
    ) {
        NotificationTemplateManagementService.EffectiveTemplate template =
                templateManagementService.resetCustomTemplate(eventType, resolveChannel(channel));

        return ApiResponse.<NotificationTemplateResponse>builder()
                .data(toTemplateResponse(template))
                .build();
    }

    @PostMapping("/notification-templates/{eventType}/{channel}/preview")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<PreviewNotificationTemplateResponse> preview(
            @PathVariable String eventType,
            @PathVariable String channel,
            @RequestBody(required = false) PreviewNotificationTemplateRequest request
    ) {
        PreviewNotificationTemplateRequest safeRequest = request == null
                ? new PreviewNotificationTemplateRequest()
                : request;

        NotificationTemplateManagementService.PreviewResult preview = templateManagementService.preview(
                        eventType,
                        resolveChannel(channel),
                        safeRequest.getTitleTemplate(),
                        safeRequest.getBodyTemplate(),
                        safeRequest.getData()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatusCode.valueOf(404),
                        "Notification template definition not found"
                ));

        return ApiResponse.<PreviewNotificationTemplateResponse>builder()
                .data(PreviewNotificationTemplateResponse.builder()
                        .title(preview.title())
                        .body(preview.body())
                        .build())
                .build();
    }

    private NotificationTemplateDefinitionResponse toDefinitionResponse(
            NotificationTemplateDefaults.Definition definition
    ) {
        return NotificationTemplateDefinitionResponse.builder()
                .eventType(definition.eventType())
                .displayName(definition.displayName())
                .description(definition.description())
                .targetType(definition.targetType())
                .allowedChannels(definition.allowedChannels())
                .variables(toVariableResponses(definition.variables()))
                .sampleData(definition.sampleData())
                .build();
    }

    private NotificationTemplateResponse toTemplateResponse(
            NotificationTemplateManagementService.EffectiveTemplate template
    ) {
        return NotificationTemplateResponse.builder()
                .eventType(template.eventType())
                .displayName(template.displayName())
                .targetType(template.targetType())
                .channel(template.channel())
                .source(template.source())
                .status(template.status())
                .titleTemplate(template.titleTemplate())
                .bodyTemplate(template.bodyTemplate())
                .variables(toVariableResponses(template.variables()))
                .updatedBy(template.updatedBy())
                .updatedAt(template.updatedAt())
                .build();
    }

    private List<NotificationTemplateVariableResponse> toVariableResponses(
            List<NotificationTemplateDefaults.Variable> variables
    ) {
        return variables.stream()
                .map(variable -> NotificationTemplateVariableResponse.builder()
                        .name(variable.name())
                        .required(variable.required())
                        .build())
                .toList();
    }

    private NotificationChannel resolveChannel(String channel) {
        try {
            return NotificationChannel.valueOf(channel.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Invalid notification channel");
        }
    }
}
