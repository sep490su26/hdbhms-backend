package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('OWNER') or hasRole('MANAGER')")
    public ApiResponse<PageResponse<TenantProfileSummaryResponse>> getTenantProfiles(
            @PageableDefault(size = 10) Pageable pageable
    ) {
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

        Map<Long, List<TenantProfileRow>> rowsByContract = new LinkedHashMap<>();
        for (TenantProfileRow row : rows) {
            rowsByContract.computeIfAbsent(row.contractId(), ignored -> new ArrayList<>()).add(row);
        }

        List<TenantProfileSummaryResponse> response = new ArrayList<>();
        for (TenantProfileRow row : rows) {
            List<TenantProfileRow> roomRows = rowsByContract.getOrDefault(row.contractId(), List.of());
            IdentityDocumentResponse identityDocument = getIdentityDocument(row.profileId());
            List<VehicleResponse> vehicles = getVehicles(row.profileId());
            List<EmergencyContactResponse> emergencyContacts = getEmergencyContacts(row.profileId());
            ProfileStatus profileStatus = resolveProfileStatus(row, identityDocument, emergencyContacts);
            List<RoommateResponse> roommates = roomRows.stream()
                    .filter(roommate -> !Objects.equals(roommate.profileId(), row.profileId())
                            || !Objects.equals(roommate.phone(), row.phone()))
                    .map(this::toRoommateResponse)
                    .toList();

            response.add(new TenantProfileSummaryResponse(
                    row.profileId(),
                    row.userId(),
                    row.fullName(),
                    row.dob(),
                    row.gender(),
                    row.phone(),
                    row.email(),
                    row.permanentAddress(),
                    fileUrl(row.portraitFileId()),
                    row.portraitFileId(),
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
                    roommates
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
            List<RoommateResponse> roommates
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
