package com.sep490.hdbhms.permissiongrant.application.service;

import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.permissiongrant.application.port.out.PermissionAccessAuditLogRepository;
import com.sep490.hdbhms.permissiongrant.application.port.out.PermissionGrantRepository;
import com.sep490.hdbhms.permissiongrant.domain.model.PermissionAccessAuditLog;
import com.sep490.hdbhms.permissiongrant.domain.model.PermissionGrant;
import com.sep490.hdbhms.permissiongrant.domain.value_objects.PermissionAccessAction;
import com.sep490.hdbhms.permissiongrant.domain.value_objects.PermissionGrantDurationCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionGrantService {
    PermissionGrantRepository permissionGrantRepository;
    PermissionAccessAuditLogRepository auditLogRepository;

    @Transactional
    public PermissionGrant grantTenantProfileAccess(ChangeRequest request, Long ownerId, String durationCode) {
        if (request.getRequestType() != RequestType.TENANT_PROFILE_ACCESS
                || request.getTargetType() != TargetType.TENANT_PROFILE
                || request.getRequesterId() == null
                || request.getTargetId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenant profile access request.");
        }

        PermissionGrantDurationCode duration = resolveDuration(durationCode);
        Optional<PermissionGrant> existingGrant = findActiveTenantProfileGrant(request.getRequesterId(), request.getTargetId());
        PermissionGrant grant = existingGrant.orElseGet(() -> PermissionGrant.tenantProfileGrant(
                request.getRequesterId(),
                request.getTargetId(),
                request.getId(),
                ownerId,
                request.getDescription(),
                duration
        ));
        if (existingGrant.isPresent()) {
            grant.renew(request.getId(), ownerId, request.getDescription(), duration);
        }
        return permissionGrantRepository.save(grant);
    }

    public Optional<PermissionGrant> findActiveTenantProfileGrant(Long managerId, Long profileId) {
        if (managerId == null || profileId == null) {
            return Optional.empty();
        }
        return permissionGrantRepository.findActive(managerId, TargetType.TENANT_PROFILE, profileId, LocalDateTime.now());
    }

    public Optional<PermissionGrant> findLatestTenantProfileGrant(Long managerId, Long profileId) {
        if (managerId == null || profileId == null) {
            return Optional.empty();
        }
        return permissionGrantRepository.findLatest(managerId, TargetType.TENANT_PROFILE, profileId);
    }

    public List<PermissionGrant> listGrants(TargetType targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return List.of();
        }
        return permissionGrantRepository.findAllByTarget(targetType, targetId);
    }

    @Transactional
    public PermissionGrant revokeGrant(Long grantId, Long ownerId, String reason) {
        PermissionGrant grant = permissionGrantRepository.findById(grantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission grant not found."));
        grant.revoke(ownerId, reason);
        return permissionGrantRepository.save(grant);
    }

    public void recordAccess(PermissionGrant grant, Long viewerId, TargetType targetType, Long targetId, PermissionAccessAction action) {
        if (grant == null || grant.getId() == null || viewerId == null || targetType == null || targetId == null || action == null) {
            return;
        }
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String ipAddress = attributes == null ? null : attributes.getRequest().getRemoteAddr();
            String userAgent = attributes == null ? null : attributes.getRequest().getHeader("User-Agent");
            auditLogRepository.save(PermissionAccessAuditLog.record(
                    grant.getId(),
                    viewerId,
                    targetType,
                    targetId,
                    action,
                    ipAddress,
                    userAgent
            ));
        } catch (RuntimeException ex) {
            log.warn("Could not record permission grant audit. grantId={}, targetType={}, targetId={}",
                    grant.getId(), targetType, targetId, ex);
        }
    }

    public String statusOf(PermissionGrant grant) {
        if (grant == null) {
            return "NONE";
        }
        if (grant.getRevokedAt() != null) {
            return "REVOKED";
        }
        return grant.isActive(LocalDateTime.now()) ? "APPROVED" : "EXPIRED";
    }

    private PermissionGrantDurationCode resolveDuration(String durationCode) {
        try {
            return PermissionGrantDurationCode.fromNullable(durationCode);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid permission grant duration.");
        }
    }
}
