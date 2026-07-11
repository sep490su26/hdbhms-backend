package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.AssignedRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequesterRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;
import com.sep490.hdbhms.permissiongrant.application.service.PermissionGrantService;
import com.sep490.hdbhms.permissiongrant.domain.model.PermissionGrant;
import com.sep490.hdbhms.permissiongrant.domain.value_objects.PermissionAccessAction;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.id.SnowflakeIdGenerator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenant-profiles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantProfileManagementController {
    JdbcTemplate jdbcTemplate;
    ChangeRequestRepository changeRequestRepository;
    NotificationOutboxRepository notificationOutboxRepository;
    ObjectMapper objectMapper;
    SnowflakeIdGenerator snowflakeIdGenerator;
    PermissionGrantService permissionGrantService;

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('OWNER') or hasRole('MANAGER')")
    public ApiResponse<PageResponse<TenantProfileSummaryResponse>> getTenantProfiles(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        UserPrincipal principal = requireCurrentPrincipal();
        boolean isOwner = principal.getRole() == Role.OWNER;
        boolean isManager = principal.getRole() == Role.MANAGER;

        List<TenantProfileRow> rows = jdbcTemplate.query("""
                        SELECT *
                        FROM (
                            SELECT lc.lease_contract_id AS contract_id,
                                   lc.contract_code,
                                   lc.status AS contract_status,
                                   lc.start_date,
                                   lc.end_date,
                                   lc.monthly_rent,
                                   lc.deposit_amount,
                                   r.room_id AS room_id,
                                   r.room_code,
                                   r.max_occupants,
                                   p.property_id AS property_id,
                                   p.name AS property_name,
                                   p.address_line AS property_address,
                                   pp.person_profile_id AS profile_id,
                                   pp.user_id,
                                   pp.full_name,
                                   pp.dob,
                                   pp.gender,
                                   pp.phone,
                                   pp.email,
                                   pp.permanent_address,
                                   pp.portrait_file_id,
                                   u.status AS app_status,
                                   co.occupant_role AS room_role,
                                   co.move_in_date,
                                   co.move_out_date,
                                   'RENTING' AS residence_status
                            FROM lease_contracts lc
                            JOIN rooms r ON r.room_id = lc.room_id
                            JOIN properties p ON p.property_id = r.property_id
                            JOIN contract_occupants co ON co.contract_id = lc.lease_contract_id AND co.status = 'ACTIVE'
                            JOIN person_profiles pp ON pp.person_profile_id = co.tenant_profile_id
                            LEFT JOIN users u ON u.user_id = pp.user_id AND u.deleted_at IS NULL
                            WHERE lc.deleted_at IS NULL
                              AND lc.status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
                              AND pp.deleted_at IS NULL

                            UNION ALL

                            SELECT lc.lease_contract_id AS contract_id,
                                   lc.contract_code,
                                   lc.status AS contract_status,
                                   lc.start_date,
                                   lc.end_date,
                                   lc.monthly_rent,
                                   lc.deposit_amount,
                                   r.room_id AS room_id,
                                   r.room_code,
                                   r.max_occupants,
                                   p.property_id AS property_id,
                                   p.name AS property_name,
                                   p.address_line AS property_address,
                                   pp.person_profile_id AS profile_id,
                                   pp.user_id,
                                   pp.full_name,
                                   pp.dob,
                                   pp.gender,
                                   pp.phone,
                                   pp.email,
                                   pp.permanent_address,
                                   pp.portrait_file_id,
                                   u.status AS app_status,
                                   'PRIMARY' AS room_role,
                                   lc.start_date AS move_in_date,
                                   NULL AS move_out_date,
                                   'RENTING' AS residence_status
                            FROM lease_contracts lc
                            JOIN rooms r ON r.room_id = lc.room_id
                            JOIN properties p ON p.property_id = r.property_id
                            JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                            LEFT JOIN users u ON u.user_id = pp.user_id AND u.deleted_at IS NULL
                            WHERE lc.deleted_at IS NULL
                              AND lc.status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
                              AND pp.deleted_at IS NULL
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM contract_occupants co_primary
                                  WHERE co_primary.contract_id = lc.lease_contract_id
                                    AND co_primary.tenant_profile_id = pp.person_profile_id
                                    AND co_primary.status = 'ACTIVE'
                              )
                        ) tenant_profiles
                        ORDER BY property_name, room_code, contract_id, room_role DESC, full_name
                        """,
                (rs, rowNum) -> mapTenantProfileRow(rs)
        );

        if (isManager) {
            rows = rows.stream()
                    .filter(row -> isAssignedManager(principal.getId(), row.propertyId()))
                    .toList();
        }

        Map<Long, List<TenantProfileRow>> rowsByContract = new LinkedHashMap<>();
        for (TenantProfileRow row : rows) {
            rowsByContract.computeIfAbsent(row.contractId(), ignored -> new ArrayList<>()).add(row);
        }

        List<TenantProfileSummaryResponse> response = new ArrayList<>();
        for (TenantProfileRow row : rows) {
            List<TenantProfileRow> roomRows = rowsByContract.getOrDefault(row.contractId(), List.of());
            ProfileAccessDecision accessDecision = resolveProfileAccess(row.profileId(), principal, isOwner);
            boolean canViewSensitiveProfile = accessDecision.canViewSensitiveProfile();
            if (canViewSensitiveProfile && isManager && accessDecision.grantId() != null) {
                permissionGrantService.recordAccess(
                        PermissionGrant.builder().id(accessDecision.grantId()).build(),
                        principal.getId(),
                        TargetType.TENANT_PROFILE,
                        row.profileId(),
                        PermissionAccessAction.VIEW_TENANT_PROFILE
                );
            }
            IdentityDocumentResponse identityDocument = canViewSensitiveProfile ? getIdentityDocument(row.profileId()) : null;
            List<VehicleResponse> vehicles = canViewSensitiveProfile ? getVehicles(row.profileId()) : List.of();
            List<EmergencyContactResponse> emergencyContacts = canViewSensitiveProfile
                    ? getEmergencyContacts(row.profileId())
                    : List.of();
            ProfileStatus profileStatus = canViewSensitiveProfile
                    ? resolveProfileStatus(row, identityDocument, emergencyContacts)
                    : restrictedProfileStatus(accessDecision);
            List<RoommateResponse> roommates = canViewSensitiveProfile
                    ? roomRows.stream()
                    .filter(roommate -> !Objects.equals(roommate.profileId(), row.profileId())
                            || !Objects.equals(roommate.phone(), row.phone()))
                    .map(this::toRoommateResponse)
                    .toList()
                    : List.of();

            response.add(new TenantProfileSummaryResponse(
                    row.profileId(),
                    row.userId(),
                    row.fullName(),
                    row.dob(),
                    row.gender(),
                    canViewSensitiveProfile ? row.phone() : maskPhone(row.phone()),
                    canViewSensitiveProfile ? row.email() : maskEmail(row.email()),
                    canViewSensitiveProfile ? row.permanentAddress() : null,
                    canViewSensitiveProfile ? fileUrl(row.portraitFileId()) : null,
                    canViewSensitiveProfile ? row.portraitFileId() : null,
                    identityDocument,
                    row.propertyId(),
                    row.propertyName(),
                    row.propertyAddress(),
                    row.roomId(),
                    row.roomCode(),
                    row.roomRole(),
                    roomRows.size(),
                    row.maxOccupants(),
                    row.moveInDate(),
                    row.moveOutDate(),
                    row.residenceStatus(),
                    row.appStatus(),
                    profileStatus.code(),
                    profileStatus.label(),
                    row.contractId(),
                    row.contractCode(),
                    row.contractStatus(),
                    row.startDate(),
                    row.endDate(),
                    row.monthlyRent(),
                    row.depositAmount(),
                    vehicles,
                    emergencyContacts,
                    roommates,
                    accessDecision.status(),
                    accessDecision.requestId(),
                    accessDecision.canViewSensitiveProfile(),
                    accessDecision.grantId(),
                    accessDecision.expiresAt(),
                    accessDecision.durationCode()
            ));
        }

        response.sort(Comparator
                .comparing(TenantProfileSummaryResponse::propertyName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(TenantProfileSummaryResponse::roomCode, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparingInt(profile -> "PRIMARY".equalsIgnoreCase(profile.roomRole()) ? 0 : 1)
                .thenComparing(TenantProfileSummaryResponse::fullName, Comparator.nullsLast(String::compareToIgnoreCase)));

        List<TenantProfileSummaryResponse> pagedResponse = response.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();

        return ApiResponse.<PageResponse<TenantProfileSummaryResponse>>builder()
                .data(PageResponse.fromPageToPageResponse(new PageImpl<>(pagedResponse, pageable, response.size())))
                .build();
    }

    @PostMapping("/{profileId}/access-requests")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<TenantProfileAccessRequestResponse> requestTenantProfileAccess(
            @PathVariable Long profileId,
            @Valid @RequestBody(required = false) TenantProfileAccessRequest request
    ) {
        UserPrincipal principal = requireCurrentPrincipal();
        TenantProfileAccessContext context = getTenantProfileAccessContext(profileId);
        if (!isAssignedManager(principal.getId(), context.propertyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager is not assigned to this property.");
        }

        ProfileAccessDecision existingAccess = resolveProfileAccess(profileId, principal, false);
        if (existingAccess.canViewSensitiveProfile() || "PENDING".equals(existingAccess.status())) {
            return ApiResponse.<TenantProfileAccessRequestResponse>builder()
                    .data(new TenantProfileAccessRequestResponse(
                            existingAccess.requestId(),
                            existingAccess.status(),
                            existingAccess.canViewSensitiveProfile()
                    ))
                    .build();
        }

        String reason = trimToNull(request == null ? null : request.reason());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantProfileId", context.profileId());
        payload.put("contractId", context.contractId());
        payload.put("contractCode", context.contractCode());
        payload.put("propertyId", context.propertyId());
        payload.put("roomCode", context.roomCode());
        payload.put("propertyName", context.propertyName());
        payload.put("fullName", context.fullName());
        payload.put("reason", reason);

        ChangeRequest changeRequest = ChangeRequest.builder()
                .requestCode("CR-" + snowflakeIdGenerator.next())
                .requestType(RequestType.TENANT_PROFILE_ACCESS)
                .requesterId(principal.getId())
                .requesterRole(RequesterRole.MANAGER)
                .targetType(TargetType.TENANT_PROFILE)
                .targetId(profileId)
                .title("Yêu cầu xem hồ sơ " + context.fullName())
                .description(reason == null ? "Manager yêu cầu xem hồ sơ khách thuê." : reason)
                .requestPayload(toJson(payload))
                .assignedRole(AssignedRole.OWNER)
                .status(RequestStatus.PENDING)
                .build();
        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);
        notifyOwnersProfileAccessRequested(savedRequest, context, reason);

        return ApiResponse.<TenantProfileAccessRequestResponse>builder()
                .data(new TenantProfileAccessRequestResponse(savedRequest.getId(), savedRequest.getStatus().name(), false))
                .build();
    }

    private void notifyOwnersProfileAccessRequested(
            ChangeRequest request,
            TenantProfileAccessContext context,
            String reason
    ) {
        List<Long> ownerIds = jdbcTemplate.queryForList("""
                        SELECT user_id
                        FROM users
                        WHERE role = 'OWNER'
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                        """,
                Long.class
        );
        if (ownerIds.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("changeRequestId", request.getId());
        payload.put("tenantProfileId", context.profileId());
        payload.put("contractId", context.contractId());
        payload.put("contractCode", context.contractCode());
        payload.put("propertyId", context.propertyId());
        payload.put("roomCode", context.roomCode());
        payload.put("propertyName", context.propertyName());
        payload.put("fullName", context.fullName());
        payload.put("reason", reason);
        String payloadJson = toJson(payload);

        String roomLabel = context.roomCode() == null ? "" : " - Phòng " + context.roomCode();
        for (Long ownerId : ownerIds) {
            notificationOutboxRepository.save(NotificationOutbox.builder()
                    .eventType("TENANT_PROFILE_ACCESS_REQUESTED")
                    .targetType("CHANGE_REQUEST")
                    .targetId(request.getId())
                    .recipientUserId(ownerId)
                    .channel(NotificationChannel.WEB)
                    .title("Yêu cầu xem hồ sơ khách thuê")
                    .body(context.fullName() + roomLabel + " đang chờ duyệt quyền xem hồ sơ.")
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .maxRetries(3)
                    .isRead(false)
                    .scheduledAt(LocalDateTime.now())
                    .nextRetryAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    private UserPrincipal requireCurrentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated.");
        }
        return principal;
    }

    private TenantProfileAccessContext getTenantProfileAccessContext(Long profileId) {
        List<TenantProfileAccessContext> contexts = jdbcTemplate.query("""
                        SELECT pp.person_profile_id AS profile_id,
                               pp.full_name,
                               lc.lease_contract_id AS contract_id,
                               lc.contract_code,
                               r.room_code,
                               p.property_id,
                               p.name AS property_name
                        FROM person_profiles pp
                        JOIN lease_contracts lc
                          ON lc.deleted_at IS NULL
                         AND lc.status IN ('ACTIVE','EXPIRING_SOON','TERMINATION_PENDING')
                         AND (
                             lc.primary_tenant_profile_id = pp.person_profile_id
                             OR EXISTS (
                                 SELECT 1
                                 FROM contract_occupants co
                                 WHERE co.contract_id = lc.lease_contract_id
                                   AND co.tenant_profile_id = pp.person_profile_id
                                   AND co.status = 'ACTIVE'
                             )
                         )
                        JOIN rooms r ON r.room_id = lc.room_id
                        JOIN properties p ON p.property_id = r.property_id
                        WHERE pp.person_profile_id = ?
                          AND pp.deleted_at IS NULL
                        ORDER BY lc.start_date DESC, lc.lease_contract_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new TenantProfileAccessContext(
                        nullableLong(rs, "profile_id"),
                        rs.getString("full_name"),
                        nullableLong(rs, "contract_id"),
                        rs.getString("contract_code"),
                        rs.getString("room_code"),
                        nullableLong(rs, "property_id"),
                        rs.getString("property_name")
                ),
                profileId
        );
        if (contexts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant profile not found.");
        }
        return contexts.getFirst();
    }

    private boolean isAssignedManager(Long managerId, Long propertyId) {
        if (managerId == null || propertyId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM role_promotions
                        WHERE user_id = ?
                          AND property_id = ?
                          AND role = 'MANAGER'
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                managerId,
                propertyId
        );
        return count != null && count > 0;
    }

    private ProfileAccessDecision resolveProfileAccess(Long profileId, UserPrincipal principal, boolean isOwner) {
        if (isOwner) {
            return new ProfileAccessDecision("APPROVED", null, true, null, null, null);
        }
        if (profileId == null || principal == null || principal.getRole() != Role.MANAGER) {
            return new ProfileAccessDecision("NONE", null, false, null, null, null);
        }

        var activeGrant = permissionGrantService.findActiveTenantProfileGrant(principal.getId(), profileId);
        if (activeGrant.isPresent()) {
            PermissionGrant grant = activeGrant.get();
            return new ProfileAccessDecision(
                    "APPROVED",
                    grant.getSourceChangeRequestId(),
                    true,
                    grant.getId(),
                    grant.getExpiresAt(),
                    grant.getDurationCode() == null ? null : grant.getDurationCode().name()
            );
        }

        List<ProfileAccessDecision> decisions = jdbcTemplate.query("""
                        SELECT change_request_id AS request_id,
                               status
                        FROM change_requests
                        WHERE request_type = 'TENANT_PROFILE_ACCESS'
                          AND target_type = 'TENANT_PROFILE'
                          AND target_id = ?
                          AND requester_id = ?
                        ORDER BY created_at DESC,
                                 change_request_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> {
                    String status = rs.getString("status");
                    return new ProfileAccessDecision(
                            status,
                            nullableLong(rs, "request_id"),
                            false,
                            null,
                            null,
                            null
                    );
                },
                profileId,
                principal.getId()
        );
        ProfileAccessDecision latestRequest = decisions.isEmpty() ? null : decisions.getFirst();
        if (latestRequest != null && ("PENDING".equals(latestRequest.status()) || "REJECTED".equals(latestRequest.status()))) {
            return latestRequest;
        }

        return permissionGrantService.findLatestTenantProfileGrant(principal.getId(), profileId)
                .map(grant -> new ProfileAccessDecision(
                        permissionGrantService.statusOf(grant),
                        grant.getSourceChangeRequestId(),
                        false,
                        grant.getId(),
                        grant.getExpiresAt(),
                        grant.getDurationCode() == null ? null : grant.getDurationCode().name()
                ))
                .orElseGet(() -> latestRequest == null
                        ? new ProfileAccessDecision("NONE", null, false, null, null, null)
                        : latestRequest);
    }

    private ProfileStatus restrictedProfileStatus(ProfileAccessDecision accessDecision) {
        return switch (accessDecision.status()) {
            case "EXPIRED" -> new ProfileStatus("ACCESS_EXPIRED", "Quyền xem đã hết hạn");
            case "REVOKED" -> new ProfileStatus("ACCESS_REVOKED", "Quyền xem đã bị thu hồi");
            case "PENDING" -> new ProfileStatus("ACCESS_PENDING", "Chờ chủ trọ duyệt");
            case "REJECTED" -> new ProfileStatus("ACCESS_REJECTED", "Yêu cầu bị từ chối");
            default -> new ProfileStatus("ACCESS_REQUIRED", "Cần gửi yêu cầu");
        };
    }

    private String maskPhone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "***";
        }
        return "*** *** " + digits.substring(digits.length() - 3);
    }

    private String maskEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int atIndex = value.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(atIndex);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not serialize request payload.");
        }
    }

    private TenantProfileRow mapTenantProfileRow(ResultSet rs) throws SQLException {
        return new TenantProfileRow(
                rs.getLong("contract_id"),
                rs.getString("contract_code"),
                rs.getString("contract_status"),
                nullableLocalDate(rs, "start_date"),
                nullableLocalDate(rs, "end_date"),
                nullableLong(rs, "monthly_rent"),
                nullableLong(rs, "deposit_amount"),
                nullableLong(rs, "room_id"),
                rs.getString("room_code"),
                nullableInt(rs, "max_occupants"),
                nullableLong(rs, "property_id"),
                rs.getString("property_name"),
                rs.getString("property_address"),
                nullableLong(rs, "profile_id"),
                nullableLong(rs, "user_id"),
                rs.getString("full_name"),
                nullableLocalDate(rs, "dob"),
                rs.getString("gender"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("permanent_address"),
                nullableLong(rs, "portrait_file_id"),
                rs.getString("app_status"),
                rs.getString("room_role"),
                nullableLocalDate(rs, "move_in_date"),
                nullableLocalDate(rs, "move_out_date"),
                rs.getString("residence_status")
        );
    }

    private IdentityDocumentResponse getIdentityDocument(Long profileId) {
        if (profileId == null) {
            return null;
        }

        List<IdentityDocumentResponse> documents = jdbcTemplate.query("""
                        SELECT identity_document_id AS id,
                               doc_type,
                               doc_number,
                               issued_date,
                               issued_place,
                               expiry_date,
                               front_file_id,
                               back_file_id,
                               status
                        FROM identity_documents
                        WHERE profile_id = ?
                          AND status = 'ACTIVE'
                        ORDER BY updated_at DESC, identity_document_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new IdentityDocumentResponse(
                        nullableLong(rs, "id"),
                        rs.getString("doc_type"),
                        normalizeIdentityNumber(rs.getString("doc_number")),
                        nullableLocalDate(rs, "issued_date"),
                        rs.getString("issued_place"),
                        nullableLocalDate(rs, "expiry_date"),
                        nullableLong(rs, "front_file_id"),
                        nullableLong(rs, "back_file_id"),
                        fileUrl(nullableLong(rs, "front_file_id")),
                        fileUrl(nullableLong(rs, "back_file_id")),
                        rs.getString("status")
                ),
                profileId
        );

        return documents.isEmpty() ? null : documents.getFirst();
    }

    private List<VehicleResponse> getVehicles(Long profileId) {
        if (profileId == null) {
            return List.of();
        }

        return jdbcTemplate.query("""
                        SELECT vehicle_id AS id,
                               vehicle_type,
                               license_plate,
                               image_file_id,
                               status
                        FROM vehicles
                        WHERE profile_id = ?
                          AND deleted_at IS NULL
                          AND status = 'ACTIVE'
                        ORDER BY vehicle_id
                        """,
                (rs, rowNum) -> new VehicleResponse(
                        nullableLong(rs, "id"),
                        rs.getString("vehicle_type"),
                        rs.getString("license_plate"),
                        nullableLong(rs, "image_file_id"),
                        fileUrl(nullableLong(rs, "image_file_id")),
                        rs.getString("status")
                ),
                profileId
        );
    }

    private List<EmergencyContactResponse> getEmergencyContacts(Long profileId) {
        if (profileId == null) {
            return List.of();
        }

        return jdbcTemplate.query("""
                        SELECT emergency_contact_id AS id,
                               full_name,
                               relationship,
                               phone
                        FROM emergency_contacts
                        WHERE tenant_profile_id = ?
                        ORDER BY emergency_contact_id
                        """,
                (rs, rowNum) -> new EmergencyContactResponse(
                        nullableLong(rs, "id"),
                        rs.getString("full_name"),
                        rs.getString("relationship"),
                        rs.getString("phone")
                ),
                profileId
        );
    }

    private RoommateResponse toRoommateResponse(TenantProfileRow row) {
        return new RoommateResponse(
                row.profileId(),
                row.fullName(),
                row.dob(),
                row.phone(),
                row.roomRole()
        );
    }

    private ProfileStatus resolveProfileStatus(
            TenantProfileRow row,
            IdentityDocumentResponse identityDocument,
            List<EmergencyContactResponse> emergencyContacts
    ) {
        if (identityDocument == null
                || identityDocument.docNumber() == null
                || identityDocument.docNumber().isBlank()
                || identityDocument.frontFileId() == null
                || identityDocument.backFileId() == null) {
            return new ProfileStatus("MISSING_CCCD", "Thiếu CCCD");
        }
        if (row.portraitFileId() == null) {
            return new ProfileStatus("MISSING_PORTRAIT", "Thiếu ảnh chân dung");
        }
        if (emergencyContacts == null || emergencyContacts.isEmpty()) {
            return new ProfileStatus("MISSING_EMERGENCY_CONTACT", "Thiếu liên hệ khẩn cấp");
        }
        return new ProfileStatus("COMPLETED", "Hồ sơ đủ");
    }

    private String fileUrl(Long fileId) {
        return fileId == null ? null : "/api/v1/files/private/" + fileId;
    }

    private String normalizeIdentityNumber(String value) {
        if (value == null || value.isBlank() || value.startsWith("PENDING-")) {
            return null;
        }
        return value;
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDate nullableLocalDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private record TenantProfileRow(
            Long contractId,
            String contractCode,
            String contractStatus,
            LocalDate startDate,
            LocalDate endDate,
            Long monthlyRent,
            Long depositAmount,
            Long roomId,
            String roomCode,
            Integer maxOccupants,
            Long propertyId,
            String propertyName,
            String propertyAddress,
            Long profileId,
            Long userId,
            String fullName,
            LocalDate dob,
            String gender,
            String phone,
            String email,
            String permanentAddress,
            Long portraitFileId,
            String appStatus,
            String roomRole,
            LocalDate moveInDate,
            LocalDate moveOutDate,
            String residenceStatus
    ) {
    }

    private record ProfileStatus(String code, String label) {
    }

    private record ProfileAccessDecision(
            String status,
            Long requestId,
            boolean canViewSensitiveProfile,
            Long grantId,
            LocalDateTime expiresAt,
            String durationCode
    ) {
    }

    private record TenantProfileAccessContext(
            Long profileId,
            String fullName,
            Long contractId,
            String contractCode,
            String roomCode,
            Long propertyId,
            String propertyName
    ) {
    }

    public record TenantProfileAccessRequest(
            @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự.")
            String reason
    ) {
    }

    public record TenantProfileAccessRequestResponse(
            Long requestId,
            String status,
            Boolean canViewSensitiveProfile
    ) {
    }

    public record TenantProfileSummaryResponse(
            Long id,
            Long userId,
            String fullName,
            LocalDate dob,
            String gender,
            String phone,
            String email,
            String permanentAddress,
            String portraitUrl,
            Long portraitFileId,
            IdentityDocumentResponse identityDocument,
            Long propertyId,
            String propertyName,
            String propertyAddress,
            Long roomId,
            String roomCode,
            String roomRole,
            Integer roomOccupantCount,
            Integer roomMaxOccupants,
            LocalDate moveInDate,
            LocalDate moveOutDate,
            String residenceStatus,
            String appStatus,
            String profileStatus,
            String profileStatusLabel,
            Long contractId,
            String contractCode,
            String contractStatus,
            LocalDate contractStartDate,
            LocalDate contractEndDate,
            Long monthlyRent,
            Long depositAmount,
            List<VehicleResponse> vehicles,
            List<EmergencyContactResponse> emergencyContacts,
            List<RoommateResponse> roommates,
            String profileAccessStatus,
            Long profileAccessRequestId,
            Boolean canViewSensitiveProfile,
            Long profileAccessGrantId,
            LocalDateTime profileAccessExpiresAt,
            String profileAccessDurationCode
    ) {
    }

    public record IdentityDocumentResponse(
            Long id,
            String docType,
            String docNumber,
            LocalDate issuedDate,
            String issuedPlace,
            LocalDate expiryDate,
            Long frontFileId,
            Long backFileId,
            String frontFileUrl,
            String backFileUrl,
            String status
    ) {
    }

    public record VehicleResponse(
            Long id,
            String vehicleType,
            String licensePlate,
            Long imageFileId,
            String imageUrl,
            String status
    ) {
    }

    public record EmergencyContactResponse(
            Long id,
            String fullName,
            String relationship,
            String phone
    ) {
    }

    public record RoommateResponse(
            Long id,
            String fullName,
            LocalDate dob,
            String phone,
            String roomRole
    ) {
    }
}
