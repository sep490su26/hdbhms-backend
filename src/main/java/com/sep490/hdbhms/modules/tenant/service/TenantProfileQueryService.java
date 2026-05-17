package com.sep490.hdbhms.modules.tenant.service;

import com.sep490.hdbhms.common.exception.ApiException;
import com.sep490.hdbhms.modules.tenant.dto.TenantProfileResponse;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class TenantProfileQueryService {

    private final JdbcTemplate jdbcTemplate;

    public TenantProfileQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TenantProfileResponse getMyProfile(Long userId, Long tenantId) {
        TenantRow tenant = queryNullable("""
                SELECT t.id, t.user_id, u.status
                FROM tenants t
                JOIN users u ON u.id = t.user_id
                WHERE t.id = ?
                  AND t.user_id = ?
                  AND t.deleted_at IS NULL
                  AND u.deleted_at IS NULL
                """, rs -> new TenantRow(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("status")
        ), tenantId, userId);

        if (tenant == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem hồ sơ này");
        }

        PersonRow person = findPersonProfile(userId, tenantId);
        if (person == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "PROFILE_NOT_FOUND",
                    "Chưa có hồ sơ cá nhân"
            );
        }

        TenantProfileResponse.IdentityDocumentDto identityDocument = getIdentityDocument(person.id());
        List<TenantProfileResponse.VehicleDto> vehicles = getVehicles(person.id());
        List<TenantProfileResponse.EmergencyContactDto> emergencyContacts = getEmergencyContacts(tenantId);

        return new TenantProfileResponse(
                tenant.id(),
                tenant.status(),
                new TenantProfileResponse.PersonProfileDto(
                        person.fullName(),
                        person.phone(),
                        person.email(),
                        person.permanentAddress(),
                        fileUrl(person.portraitFileId())
                ),
                identityDocument,
                vehicles,
                emergencyContacts
        );
    }

    private PersonRow findPersonProfile(Long userId, Long tenantId) {
        return queryNullable("""
                SELECT pp.id,
                       pp.full_name,
                       pp.phone,
                       pp.email,
                       pp.permanent_address,
                       pp.portrait_file_id
                FROM person_profiles pp
                JOIN users u ON u.id = ?
                WHERE pp.deleted_at IS NULL
                  AND (
                    pp.phone = u.phone
                    OR LOWER(pp.email) = LOWER(u.email)
                    OR pp.id IN (
                        SELECT lc.primary_tenant_profile_id
                        FROM lease_contracts lc
                        JOIN contract_occupants co ON co.contract_id = lc.id
                        WHERE co.tenant_id = ?
                          AND lc.deleted_at IS NULL
                    )
                    OR pp.id IN (
                        SELECT da.depositor_person_profile_id
                        FROM deposit_agreements da
                        WHERE da.tenant_id = ?
                          AND da.depositor_person_profile_id IS NOT NULL
                    )
                  )
                ORDER BY
                  CASE
                    WHEN pp.phone = u.phone OR LOWER(pp.email) = LOWER(u.email) THEN 0
                    ELSE 1
                  END,
                  pp.id DESC
                LIMIT 1
                """, rs -> new PersonRow(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("permanent_address"),
                nullableLong(rs, "portrait_file_id")
        ), userId, tenantId, tenantId);
    }

    private TenantProfileResponse.IdentityDocumentDto getIdentityDocument(Long personProfileId) {
        return queryNullable("""
                SELECT doc_type, doc_number, issued_date, issued_place
                FROM identity_documents
                WHERE profile_id = ?
                  AND status = 'ACTIVE'
                ORDER BY id DESC
                LIMIT 1
                """, rs -> new TenantProfileResponse.IdentityDocumentDto(
                rs.getString("doc_type"),
                rs.getString("doc_number"),
                nullableLocalDate(rs, "issued_date"),
                rs.getString("issued_place")
        ), personProfileId);
    }

    private List<TenantProfileResponse.VehicleDto> getVehicles(Long personProfileId) {
        return jdbcTemplate.query("""
                SELECT id, vehicle_type, license_plate, image_file_id
                FROM vehicles
                WHERE profile_id = ?
                  AND status = 'ACTIVE'
                  AND deleted_at IS NULL
                ORDER BY id
                """, (rs, rowNum) -> new TenantProfileResponse.VehicleDto(
                rs.getLong("id"),
                rs.getString("vehicle_type"),
                rs.getString("license_plate"),
                fileUrl(nullableLong(rs, "image_file_id"))
        ), personProfileId);
    }

    private List<TenantProfileResponse.EmergencyContactDto> getEmergencyContacts(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT full_name, relationship, phone
                FROM emergency_contacts
                WHERE tenant_profile_id = ?
                ORDER BY id
                """, (rs, rowNum) -> new TenantProfileResponse.EmergencyContactDto(
                rs.getString("full_name"),
                rs.getString("relationship"),
                rs.getString("phone")
        ), tenantId);
    }

    private String fileUrl(Long fileId) {
        if (fileId == null) {
            return null;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/{fileId}")
                .buildAndExpand(fileId)
                .toUriString();
    }

    private Long nullableLong(ResultSet resultSet, String column) throws java.sql.SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private LocalDate nullableLocalDate(ResultSet resultSet, String column) throws java.sql.SQLException {
        return resultSet.getDate(column) == null ? null : resultSet.getDate(column).toLocalDate();
    }

    private <T> T queryNullable(String sql, RowMapperFunction<T> mapper, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapper.map(rs), args);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private record TenantRow(Long id, Long userId, String status) {
    }

    private record PersonRow(
            Long id,
            String fullName,
            String phone,
            String email,
            String permanentAddress,
            Long portraitFileId
    ) {
    }

    @FunctionalInterface
    private interface RowMapperFunction<T> {
        T map(ResultSet resultSet) throws java.sql.SQLException;
    }
}
