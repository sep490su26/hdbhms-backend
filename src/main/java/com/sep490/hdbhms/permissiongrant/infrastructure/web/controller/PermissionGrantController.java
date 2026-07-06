package com.sep490.hdbhms.permissiongrant.infrastructure.web.controller;

import com.sep490.hdbhms.changerequest.domain.valueObjects.TargetType;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.permissiongrant.application.service.PermissionGrantService;
import com.sep490.hdbhms.permissiongrant.domain.model.PermissionGrant;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/permission-grants")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionGrantController {
    PermissionGrantService permissionGrantService;
    PersonProfileRepository personProfileRepository;

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<List<PermissionGrantResponse>> getGrants(
            @RequestParam TargetType targetType,
            @RequestParam Long targetId
    ) {
        return ApiResponse.<List<PermissionGrantResponse>>builder()
                .data(permissionGrantService.listGrants(targetType, targetId).stream()
                        .map(this::toResponse)
                        .toList())
                .build();
    }

    @PostMapping("/{grantId}/revoke")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<PermissionGrantResponse> revokeGrant(
            @PathVariable Long grantId,
            @Valid @RequestBody(required = false) RevokeGrantRequest request
    ) {
        PermissionGrant grant = permissionGrantService.revokeGrant(
                grantId,
                AuthUtils.getCurrentAuthenticationId(),
                request == null ? null : request.reason()
        );
        return ApiResponse.<PermissionGrantResponse>builder()
                .data(toResponse(grant))
                .build();
    }

    private PermissionGrantResponse toResponse(PermissionGrant grant) {
        PersonProfile granteeProfile = profileOf(grant.getGranteeUserId());
        return new PermissionGrantResponse(
                grant.getId(),
                grant.getGranteeUserId(),
                granteeProfile == null ? null : granteeProfile.getFullName(),
                granteeProfile == null ? null : granteeProfile.getPhone(),
                granteeProfile == null ? null : granteeProfile.getEmail(),
                grant.getTargetType(),
                grant.getTargetId(),
                grant.getSourceChangeRequestId(),
                grant.getGrantedBy(),
                grant.getReason(),
                grant.getDurationCode() == null ? null : grant.getDurationCode().name(),
                grant.getGrantedAt(),
                grant.getExpiresAt(),
                grant.getRevokedAt(),
                grant.getRevokedBy(),
                grant.getRevokeReason(),
                permissionGrantService.statusOf(grant)
        );
    }

    private PersonProfile profileOf(Long userId) {
        return userId == null ? null : personProfileRepository.findByUserId(userId).orElse(null);
    }

    public record RevokeGrantRequest(
            @Size(max = 1000, message = "Lý do thu hồi không được vượt quá 1000 ký tự.")
            String reason
    ) {
    }

    public record PermissionGrantResponse(
            Long id,
            Long granteeUserId,
            String granteeFullName,
            String granteePhone,
            String granteeEmail,
            TargetType targetType,
            Long targetId,
            Long sourceChangeRequestId,
            Long grantedBy,
            String reason,
            String durationCode,
            LocalDateTime grantedAt,
            LocalDateTime expiresAt,
            LocalDateTime revokedAt,
            Long revokedBy,
            String revokeReason,
            String status
    ) {
    }
}
