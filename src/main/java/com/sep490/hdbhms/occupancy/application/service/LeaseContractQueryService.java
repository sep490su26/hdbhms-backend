package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.TenantAccountProvisioningStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.domain.valueObjects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.OccupantRole;
import com.sep490.hdbhms.occupancy.domain.valueObjects.OccupantStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractQueryDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractQueryItemResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomRentalHistoryResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractQueryService {
    JdbcTemplate jdbcTemplate;

    public List<LeaseContractQueryItemResponse> findContracts(
            Long tenantId,
            LeaseStatus status,
            Long roomId,
            Long propertyId,
            Long tenantProfileId,
            LocalDate dateFrom,
            LocalDate dateTo,
            String keyword
    ) {
        Scope scope = resolveScope(tenantId);
        StringBuilder sql = new StringBuilder("""
                SELECT
                    lc.lease_contract_id AS contract_id,
                    lc.contract_code,
                    r.room_id AS room_id,
                    r.room_code,
                    p.property_id AS property_id,
                    p.name AS property_name,
                    pp.full_name AS primary_tenant_name,
                    GREATEST(
                        (
                            SELECT COUNT(*)
                            FROM contract_occupants co_count
                            WHERE co_count.contract_id = lc.lease_contract_id
                              AND co_count.status = 'ACTIVE'
                        ),
                        COALESCE(df.occupant_count, 1),
                        1 + COALESCE((
                            SELECT COUNT(*)
                            FROM deposit_form_co_occupants dco_count
                            WHERE dco_count.deposit_form_id = df.deposit_form_id
                        ), 0)
                    ) AS occupants_count,
                    lc.start_date,
                    lc.end_date,
                    lc.rent_start_date,
                    lc.monthly_rent,
                    lc.payment_cycle_months,
                    lc.status,
                    lc.signed_at,
                    lc.contract_file_id,
                    fm.original_name AS contract_file_name
                FROM lease_contracts lc
                JOIN rooms r ON r.room_id = lc.room_id
                JOIN properties p ON p.property_id = r.property_id
                JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                LEFT JOIN deposit_agreements da ON da.deposit_agreement_id = lc.deposit_agreement_id
                LEFT JOIN deposit_forms df ON df.deposit_form_id = da.deposit_form_id
                LEFT JOIN file_metadata fm ON fm.file_metadata_id = lc.contract_file_id
                WHERE lc.deleted_at IS NULL
                  AND p.property_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(scope.propertyId());
        appendReadableScope(sql, params, scope);

        if (status != null) {
            sql.append(" AND lc.status = ?\n");
            params.add(status.name());
        }
        if (roomId != null) {
            sql.append(" AND r.room_id = ?\n");
            params.add(roomId);
        }
        if (propertyId != null) {
            sql.append(" AND p.property_id = ?\n");
            params.add(propertyId);
        }
        if (tenantProfileId != null) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM contract_occupants co_filter
                         WHERE co_filter.contract_id = lc.lease_contract_id
                           AND co_filter.tenant_profile_id = ?
                     )
                    """);
            params.add(tenantProfileId);
        }
        if (dateFrom != null) {
            sql.append(" AND lc.start_date >= ?\n");
            params.add(dateFrom);
        }
        if (dateTo != null) {
            sql.append(" AND lc.end_date <= ?\n");
            params.add(dateTo);
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            sql.append("""
                     AND (
                         lc.contract_code LIKE ?
                         OR r.room_code LIKE ?
                         OR EXISTS (
                             SELECT 1
                             FROM contract_occupants co_keyword
                             LEFT JOIN person_profiles opp ON opp.person_profile_id = co_keyword.tenant_profile_id
                             WHERE co_keyword.contract_id = lc.lease_contract_id
                               AND (opp.full_name LIKE ? OR opp.phone LIKE ?)
                         )
                     )
                    """);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        sql.append(" ORDER BY lc.start_date DESC, lc.lease_contract_id DESC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> toItem(rs), params.toArray());
    }

    public LeaseContractQueryDetailsResponse getContractDetails(Long tenantId, Long contractId) {
        Scope scope = resolveScope(tenantId);
        LeaseContractQueryDetailsResponse details = jdbcTemplate.query("""
                        SELECT
                            lc.lease_contract_id AS contract_id,
                            lc.contract_code,
                            lc.deposit_agreement_id,
                            da.signed_file_id AS deposit_signed_file_id,
                            lc.start_date,
                            lc.end_date,
                            lc.rent_start_date,
                            lc.monthly_rent,
                            lc.payment_cycle_months,
                            lc.deposit_amount,
                            lc.status,
                            lc.signed_at,
                            lc.previous_contract_id,
                            previous_contract.contract_code AS previous_contract_code,
                            lc.tenant_intention,
                            lc.expected_vacant_date,
                            lc.intention_recorded_at,
                            (
                                SELECT renewed.lease_contract_id
                                FROM lease_contracts renewed
                                WHERE renewed.previous_contract_id = lc.lease_contract_id
                                  AND renewed.deleted_at IS NULL
                                ORDER BY renewed.lease_contract_id DESC
                                LIMIT 1
                            ) AS renewed_contract_id,
                            (
                                SELECT renewed.contract_code
                                FROM lease_contracts renewed
                                WHERE renewed.previous_contract_id = lc.lease_contract_id
                                  AND renewed.deleted_at IS NULL
                                ORDER BY renewed.lease_contract_id DESC
                                LIMIT 1
                            ) AS renewed_contract_code,
                            lc.contract_file_id,
                            fm.original_name AS contract_file_name,
                            r.room_id AS room_id,
                            r.room_code,
                            r.name AS room_name,
                            p.property_id AS property_id,
                            p.name AS property_name,
                            p.address_line AS property_address,
                            pp.person_profile_id AS primary_tenant_profile_id,
                            pp.full_name AS primary_full_name,
                            pp.phone AS primary_phone,
                            pp.email AS primary_email,
                            pp.dob AS primary_dob,
                            pp.permanent_address AS primary_permanent_address,
                            COALESCE(
                                (
                                    SELECT idoc.doc_number
                                    FROM identity_documents idoc
                                    WHERE idoc.profile_id = pp.person_profile_id
                                      AND idoc.status = 'ACTIVE'
                                    ORDER BY idoc.updated_at DESC, idoc.identity_document_id DESC
                                    LIMIT 1
                                ),
                                df.id_number
                            ) AS primary_citizen_id,
                            COALESCE(
                                (
                                    SELECT idoc.issued_date
                                    FROM identity_documents idoc
                                    WHERE idoc.profile_id = pp.person_profile_id
                                      AND idoc.status = 'ACTIVE'
                                    ORDER BY idoc.updated_at DESC, idoc.identity_document_id DESC
                                    LIMIT 1
                                ),
                                df.id_issue_date
                            ) AS primary_identity_issued_date,
                            COALESCE(
                                (
                                    SELECT idoc.issued_place
                                    FROM identity_documents idoc
                                    WHERE idoc.profile_id = pp.person_profile_id
                                      AND idoc.status = 'ACTIVE'
                                    ORDER BY idoc.updated_at DESC, idoc.identity_document_id DESC
                                    LIMIT 1
                                ),
                                df.id_issue_place
                            ) AS primary_identity_issued_place
                        FROM lease_contracts lc
                        JOIN rooms r ON r.room_id = lc.room_id
                        JOIN properties p ON p.property_id = r.property_id
                        JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                        LEFT JOIN lease_contracts previous_contract ON previous_contract.lease_contract_id = lc.previous_contract_id
                        LEFT JOIN deposit_agreements da ON da.deposit_agreement_id = lc.deposit_agreement_id
                        LEFT JOIN deposit_forms df ON df.deposit_form_id = da.deposit_form_id
                        LEFT JOIN file_metadata fm ON fm.file_metadata_id = lc.contract_file_id
                        WHERE lc.deleted_at IS NULL
                          AND lc.lease_contract_id = ?
                          AND p.property_id = ?
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
                    }
                    return toDetailsShell(rs, List.of(), List.of());
                },
                contractId,
                scope.propertyId()
        );
        assertCanReadContract(scope, contractId);
        return toDetailsShell(
                details,
                findOccupants(contractId),
                findEvents(contractId)
        );
    }

    public RoomRentalHistoryResponse getRoomRentalHistory(Long tenantId, Long roomId) {
        Scope scope = resolveScope(tenantId);
        RoomInfoRow room = jdbcTemplate.query("""
                        SELECT r.room_id AS id, r.room_code, r.name
                        FROM rooms r
                        JOIN properties p ON p.property_id = r.property_id
                        WHERE r.room_id = ?
                          AND p.property_id = ?
                          AND r.deleted_at IS NULL
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay phong.");
                    }
                    return new RoomInfoRow(
                            rs.getLong("id"),
                            rs.getString("room_code"),
                            rs.getString("name")
                    );
                },
                roomId,
                scope.propertyId()
        );

        StringBuilder sql = new StringBuilder("""
                SELECT lc.lease_contract_id AS id FROM lease_contracts lc
                WHERE lc.room_id = ?
                  AND lc.deleted_at IS NULL
                """);
        List<Object> params = new ArrayList<>();
        params.add(roomId);
        appendReadableScope(sql, params, scope);
        sql.append(" ORDER BY CASE WHEN lc.status = 'ACTIVE' THEN 0 ELSE 1 END, lc.start_date DESC, lc.lease_contract_id DESC");

        List<Long> contractIds = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("id"), params.toArray());
        List<LeaseContractQueryDetailsResponse> contracts = contractIds.stream()
                .map(contractId -> getContractDetails(tenantId, contractId))
                .toList();
        return new RoomRentalHistoryResponse(room.id(), room.roomCode(), room.roomName(), contracts);
    }

    public RoomRentalHistoryResponse getManagementRoomRentalHistory(Long roomId) {
        CurrentUser currentUser = getCurrentUser();
        RoomInfoRow room = jdbcTemplate.query("""
                        SELECT r.room_id AS id, r.room_code, r.name, r.property_id
                        FROM rooms r
                        WHERE r.room_id = ?
                          AND r.deleted_at IS NULL
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay phong.");
                    }
                    return new RoomInfoRow(
                            rs.getLong("id"),
                            rs.getString("room_code"),
                            rs.getString("name"),
                            rs.getLong("property_id")
                    );
                },
                roomId
        );
        assertManagerCanReadProperty(currentUser, room.propertyId());

        List<Long> contractIds = jdbcTemplate.query("""
                        SELECT lc.lease_contract_id AS id FROM lease_contracts lc
                        WHERE lc.room_id = ?
                          AND lc.deleted_at IS NULL
                        ORDER BY CASE WHEN lc.status = 'ACTIVE' THEN 0 ELSE 1 END,
                                 lc.start_date DESC,
                                 lc.lease_contract_id DESC
                        """,
                (rs, rowNum) -> rs.getLong("id"),
                roomId
        );
        List<LeaseContractQueryDetailsResponse> contracts = contractIds.stream()
                .map(contractId -> getManagementContractDetails(contractId, room.propertyId()))
                .toList();
        return new RoomRentalHistoryResponse(room.id(), room.roomCode(), room.roomName(), contracts);
    }

    public LeaseContractQueryDetailsResponse getManagementContractDetails(Long contractId) {
        CurrentUser currentUser = getCurrentUser();
        Long propertyId = jdbcTemplate.query("""
                        SELECT r.property_id
                        FROM lease_contracts lc
                        JOIN rooms r ON r.room_id = lc.room_id AND r.deleted_at IS NULL
                        WHERE lc.lease_contract_id = ?
                          AND lc.deleted_at IS NULL
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
                    }
                    return rs.getLong("property_id");
                },
                contractId
        );
        assertManagerCanReadProperty(currentUser, propertyId);
        return getManagementContractDetails(contractId, propertyId);
    }

    private LeaseContractQueryDetailsResponse getManagementContractDetails(Long contractId, Long propertyId) {
        return jdbcTemplate.query("""
                        SELECT
                            lc.lease_contract_id AS contract_id,
                            lc.contract_code,
                            lc.deposit_agreement_id,
                            da.signed_file_id AS deposit_signed_file_id,
                            lc.start_date,
                            lc.end_date,
                            lc.rent_start_date,
                            lc.monthly_rent,
                            lc.payment_cycle_months,
                            lc.deposit_amount,
                            lc.status,
                            lc.signed_at,
                            lc.previous_contract_id,
                            previous_contract.contract_code AS previous_contract_code,
                            lc.tenant_intention,
                            lc.expected_vacant_date,
                            lc.intention_recorded_at,
                            (
                                SELECT renewed.lease_contract_id
                                FROM lease_contracts renewed
                                WHERE renewed.previous_contract_id = lc.lease_contract_id
                                  AND renewed.deleted_at IS NULL
                                ORDER BY renewed.lease_contract_id DESC
                                LIMIT 1
                            ) AS renewed_contract_id,
                            (
                                SELECT renewed.contract_code
                                FROM lease_contracts renewed
                                WHERE renewed.previous_contract_id = lc.lease_contract_id
                                  AND renewed.deleted_at IS NULL
                                ORDER BY renewed.lease_contract_id DESC
                                LIMIT 1
                            ) AS renewed_contract_code,
                            lc.contract_file_id,
                            fm.original_name AS contract_file_name,
                            r.room_id AS room_id,
                            r.room_code,
                            r.name AS room_name,
                            p.property_id AS property_id,
                            p.name AS property_name,
                            p.address_line AS property_address,
                            pp.person_profile_id AS primary_tenant_profile_id,
                            pp.full_name AS primary_full_name,
                            pp.phone AS primary_phone,
                            pp.email AS primary_email,
                            pp.dob AS primary_dob,
                            pp.permanent_address AS primary_permanent_address,
                            COALESCE(
                                (
                                    SELECT idoc.doc_number
                                    FROM identity_documents idoc
                                    WHERE idoc.profile_id = pp.person_profile_id
                                      AND idoc.status = 'ACTIVE'
                                    ORDER BY idoc.updated_at DESC, idoc.identity_document_id DESC
                                    LIMIT 1
                                ),
                                df.id_number
                            ) AS primary_citizen_id,
                            COALESCE(
                                (
                                    SELECT idoc.issued_date
                                    FROM identity_documents idoc
                                    WHERE idoc.profile_id = pp.person_profile_id
                                      AND idoc.status = 'ACTIVE'
                                    ORDER BY idoc.updated_at DESC, idoc.identity_document_id DESC
                                    LIMIT 1
                                ),
                                df.id_issue_date
                            ) AS primary_identity_issued_date,
                            COALESCE(
                                (
                                    SELECT idoc.issued_place
                                    FROM identity_documents idoc
                                    WHERE idoc.profile_id = pp.person_profile_id
                                      AND idoc.status = 'ACTIVE'
                                    ORDER BY idoc.updated_at DESC, idoc.identity_document_id DESC
                                    LIMIT 1
                                ),
                                df.id_issue_place
                            ) AS primary_identity_issued_place
                        FROM lease_contracts lc
                        JOIN rooms r ON r.room_id = lc.room_id
                        JOIN properties p ON p.property_id = r.property_id
                        JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                        LEFT JOIN lease_contracts previous_contract ON previous_contract.lease_contract_id = lc.previous_contract_id
                        LEFT JOIN deposit_agreements da ON da.deposit_agreement_id = lc.deposit_agreement_id
                        LEFT JOIN deposit_forms df ON df.deposit_form_id = da.deposit_form_id
                        LEFT JOIN file_metadata fm ON fm.file_metadata_id = lc.contract_file_id
                        WHERE lc.deleted_at IS NULL
                          AND lc.lease_contract_id = ?
                          AND p.property_id = ?
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
                    }
                    return toDetailsShell(rs, findOccupants(contractId), findEvents(contractId));
                },
                contractId,
                propertyId
        );
    }

    private void appendReadableScope(StringBuilder sql, List<Object> params, Scope scope) {
        if (scope.role() == Role.TENANT) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM contract_occupants co_scope
                         LEFT JOIN person_profiles pp_scope ON pp_scope.person_profile_id = co_scope.tenant_profile_id
                         WHERE co_scope.contract_id = lc.lease_contract_id
                           AND co_scope.status = 'ACTIVE'
                           AND (co_scope.tenant_id = ? OR pp_scope.user_id = ?)
                     )
                    """);
            params.add(scope.tenantId());
            params.add(scope.userId());
        }
    }

    private void assertCanReadContract(Scope scope, Long contractId) {
        if (scope.role() != Role.TENANT) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts lc
                        WHERE lc.lease_contract_id = ?
                          AND lc.deleted_at IS NULL
                          AND (
                              lc.primary_tenant_profile_id IN (
                                  SELECT pp.person_profile_id AS id FROM person_profiles pp
                                  WHERE pp.user_id = ?
                                    AND pp.deleted_at IS NULL
                                  UNION
                                  SELECT tap.tenant_profile_id
                                  FROM tenant_account_provisionings tap
                                  JOIN person_profiles pp ON pp.person_profile_id = tap.tenant_profile_id
                                  WHERE tap.user_id = ?
                                    AND tap.status <> 'DISABLED'
                                    AND pp.user_id IS NULL
                                    AND pp.deleted_at IS NULL
                              )
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM contract_occupants disabled_primary
                                  WHERE disabled_primary.contract_id = lc.lease_contract_id
                                    AND disabled_primary.tenant_profile_id = lc.primary_tenant_profile_id
                                    AND disabled_primary.status = 'DISABLED'
                              )
                              OR EXISTS (
                                  SELECT 1
                                  FROM contract_occupants co
                                  WHERE co.contract_id = lc.lease_contract_id
                                    AND co.status = 'ACTIVE'
                                    AND co.tenant_profile_id IN (
                                        SELECT pp.person_profile_id AS id FROM person_profiles pp
                                        WHERE pp.user_id = ?
                                          AND pp.deleted_at IS NULL
                                        UNION
                                          SELECT tap.tenant_profile_id
                                          FROM tenant_account_provisionings tap
                                          JOIN person_profiles pp ON pp.person_profile_id = tap.tenant_profile_id
                                          WHERE tap.user_id = ?
                                          AND tap.status <> 'DISABLED'
                                          AND pp.user_id IS NULL
                                          AND pp.deleted_at IS NULL
                                    )
                              )
                          )
                        """,
                Integer.class,
                contractId,
                scope.userId(),
                scope.userId(),
                scope.userId(),
                scope.userId()
        );
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xem hop dong nay.");
        }
    }

    private Scope resolveScope(Long tenantId) {
        CurrentUser currentUser = getCurrentUser();
        Long propertyId = jdbcTemplate.query("""
                        SELECT property_id
                        FROM tenants
                        WHERE tenant_id = ?
                          AND deleted_at IS NULL
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("property_id") : null,
                tenantId
        );
        if (propertyId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay tenant scope.");
        }
        if (currentUser.role() == Role.TENANT) {
            Integer count = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM tenants
                            WHERE tenant_id = ?
                              AND user_id = ?
                              AND deleted_at IS NULL
                            """,
                    Integer.class,
                    tenantId,
                    currentUser.userId()
            );
            if (count == null || count == 0) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xem du lieu tenant nay.");
            }
        }
        return new Scope(tenantId, propertyId, currentUser.userId(), currentUser.role());
    }

    private CurrentUser getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap.");
        }
        Role role = userPrincipal.getRole();
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Khong xac dinh duoc quyen truy cap.");
        }
        return new CurrentUser(userPrincipal.getId(), role);
    }

    private void assertManagerCanReadProperty(CurrentUser currentUser, Long propertyId) {
        if (currentUser.role() == Role.OWNER) {
            return;
        }
        if (currentUser.role() != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xem lich su thue phong.");
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
                currentUser.userId(),
                propertyId
        );
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xem co so nay.");
        }
    }

    private LeaseContractQueryItemResponse toItem(ResultSet rs) throws SQLException {
        return new LeaseContractQueryItemResponse(
                rs.getLong("contract_id"),
                rs.getString("contract_code"),
                rs.getLong("room_id"),
                rs.getString("room_code"),
                rs.getLong("property_id"),
                rs.getString("property_name"),
                rs.getString("primary_tenant_name"),
                getIntOrNull(rs, "occupants_count"),
                toLocalDate(rs, "start_date"),
                toLocalDate(rs, "end_date"),
                toLocalDate(rs, "rent_start_date"),
                getLongOrNull(rs, "monthly_rent"),
                getIntOrNull(rs, "payment_cycle_months"),
                LeaseStatus.valueOf(rs.getString("status")),
                toLocalDateTime(rs, "signed_at"),
                getLongOrNull(rs, "contract_file_id"),
                rs.getString("contract_file_name")
        );
    }

    private LeaseContractQueryDetailsResponse toDetailsShell(
            ResultSet rs,
            List<LeaseContractQueryDetailsResponse.OccupantInfo> occupants,
            List<LeaseContractQueryDetailsResponse.EventInfo> events
    ) throws SQLException {
        Long fileId = getLongOrNull(rs, "contract_file_id");
        LeaseStatus status = LeaseStatus.valueOf(rs.getString("status"));
        Long renewedContractId = getLongOrNull(rs, "renewed_contract_id");
        AccountProvisioningSummary accountProvisioning =
                resolveAccountProvisioning(status, occupants);
        return new LeaseContractQueryDetailsResponse(
                rs.getLong("contract_id"),
                rs.getString("contract_code"),
                getLongOrNull(rs, "deposit_agreement_id"),
                getLongOrNull(rs, "deposit_signed_file_id"),
                new LeaseContractQueryDetailsResponse.RoomInfo(
                        rs.getLong("room_id"),
                        rs.getString("room_code"),
                        rs.getString("room_name")
                ),
                new LeaseContractQueryDetailsResponse.PropertyInfo(
                        rs.getLong("property_id"),
                        rs.getString("property_name"),
                        rs.getString("property_address")
                ),
                toLocalDate(rs, "start_date"),
                toLocalDate(rs, "end_date"),
                toLocalDate(rs, "rent_start_date"),
                getLongOrNull(rs, "monthly_rent"),
                getIntOrNull(rs, "payment_cycle_months"),
                getLongOrNull(rs, "deposit_amount"),
                status,
                toLocalDateTime(rs, "signed_at"),
                getLongOrNull(rs, "previous_contract_id"),
                rs.getString("previous_contract_code"),
                renewedContractId,
                rs.getString("renewed_contract_code"),
                rs.getString("tenant_intention"),
                toLocalDate(rs, "expected_vacant_date"),
                toLocalDateTime(rs, "intention_recorded_at"),
                renewedContractId == null && List.of(
                        LeaseStatus.ACTIVE,
                        LeaseStatus.EXPIRING_SOON,
                        LeaseStatus.EXPIRED
                ).contains(status),
                List.of(
                        LeaseStatus.ACTIVE,
                        LeaseStatus.EXPIRING_SOON,
                        LeaseStatus.EXPIRED,
                        LeaseStatus.TERMINATION_PENDING
                ).contains(status),
                accountProvisioning.canSend(),
                accountProvisioning.status(),
                fileId != null ? new LeaseContractQueryDetailsResponse.ContractFileInfo(fileId, rs.getString("contract_file_name")) : null,
                new LeaseContractQueryDetailsResponse.TenantProfileInfo(
                        rs.getLong("primary_tenant_profile_id"),
                        rs.getString("primary_full_name"),
                        rs.getString("primary_phone"),
                        rs.getString("primary_email"),
                        toLocalDate(rs, "primary_dob"),
                        rs.getString("primary_permanent_address"),
                        normalizeIdentityNumber(rs.getString("primary_citizen_id")),
                        toLocalDate(rs, "primary_identity_issued_date"),
                        rs.getString("primary_identity_issued_place")
                ),
                occupants,
                events
        );
    }

    private LeaseContractQueryDetailsResponse toDetailsShell(
            LeaseContractQueryDetailsResponse details,
            List<LeaseContractQueryDetailsResponse.OccupantInfo> occupants,
            List<LeaseContractQueryDetailsResponse.EventInfo> events
    ) {
        AccountProvisioningSummary accountProvisioning =
                resolveAccountProvisioning(details.status(), occupants);
        return new LeaseContractQueryDetailsResponse(
                details.contractId(),
                details.contractCode(),
                details.depositAgreementId(),
                details.depositSignedFileId(),
                details.room(),
                details.property(),
                details.startDate(),
                details.endDate(),
                details.rentStartDate(),
                details.monthlyRent(),
                details.paymentCycleMonths(),
                details.depositAmount(),
                details.status(),
                details.signedAt(),
                details.previousContractId(),
                details.previousContractCode(),
                details.renewedContractId(),
                details.renewedContractCode(),
                details.tenantIntention(),
                details.expectedVacantDate(),
                details.intentionRecordedAt(),
                details.canRenew(),
                details.canLiquidate(),
                accountProvisioning.canSend(),
                accountProvisioning.status(),
                details.contractFile(),
                details.primaryTenant(),
                occupants,
                events
        );
    }

    private List<LeaseContractQueryDetailsResponse.OccupantInfo> findOccupants(Long contractId) {
        return jdbcTemplate.query("""
                SELECT
                    pp.person_profile_id AS tenant_profile_id,
                    pp.full_name,
                    pp.phone,
                    COALESCE(pp.email, u.email) AS email,
                    pp.dob,
                    pp.permanent_address,
                    COALESCE(
                        (
                            SELECT idoc.doc_number
                            FROM identity_documents idoc
                            WHERE idoc.profile_id = pp.person_profile_id
                              AND idoc.status = 'ACTIVE'
                            ORDER BY idoc.updated_at DESC, idoc.identity_document_id DESC
                            LIMIT 1
                        ),
                        CASE WHEN co.occupant_role = 'PRIMARY' THEN df.id_number END
                    ) AS citizen_id,
                    COALESCE(
                        (
                            SELECT idoc.issued_date
                            FROM identity_documents idoc
                            WHERE idoc.profile_id = pp.person_profile_id
                              AND idoc.status = 'ACTIVE'
                            ORDER BY idoc.updated_at DESC, idoc.identity_document_id DESC
                            LIMIT 1
                        ),
                        CASE WHEN co.occupant_role = 'PRIMARY' THEN df.id_issue_date END
                    ) AS identity_issued_date,
                    COALESCE(
                        (
                            SELECT idoc.issued_place
                            FROM identity_documents idoc
                            WHERE idoc.profile_id = pp.person_profile_id
                              AND idoc.status = 'ACTIVE'
                            ORDER BY idoc.updated_at DESC, idoc.identity_document_id DESC
                            LIMIT 1
                        ),
                        CASE WHEN co.occupant_role = 'PRIMARY' THEN df.id_issue_place END
                    ) AS identity_issued_place,
                    co.occupant_role,
                    co.move_in_date,
                    co.move_out_date,
                    co.status,
                    COALESCE(
                        CASE
                            WHEN co.status = 'DISABLED'
                                THEN 'DISABLED'
                            WHEN u.user_id IS NOT NULL
                              AND (u.last_login_at IS NOT NULL OR u.must_change_password = FALSE)
                                THEN 'ACTIVE'
                            ELSE tap.status
                        END,
                        CASE
                            WHEN u.user_id IS NULL THEN 'NOT_PROVISIONED'
                            WHEN u.last_login_at IS NOT NULL OR u.must_change_password = FALSE THEN 'ACTIVE'
                            ELSE 'SENT'
                        END
                    ) AS account_status,
                    tap.sent_at AS account_sent_at,
                    u.last_login_at,
                    u.must_change_password
                FROM (
                    SELECT
                        active_occupant.contract_occupant_id AS id,
                        active_occupant.contract_id,
                        active_occupant.tenant_profile_id,
                        active_occupant.occupant_role,
                        active_occupant.move_in_date,
                        active_occupant.move_out_date,
                        active_occupant.status
                    FROM contract_occupants active_occupant
                    WHERE active_occupant.status IN ('ACTIVE','DISABLED')
                      AND active_occupant.tenant_profile_id IS NOT NULL

                    UNION ALL

                    SELECT
                        NULL AS id,
                        fallback_contract.lease_contract_id AS contract_id,
                        fallback_contract.primary_tenant_profile_id AS tenant_profile_id,
                        'PRIMARY' AS occupant_role,
                        fallback_contract.start_date AS move_in_date,
                        NULL AS move_out_date,
                        'ACTIVE' AS status
                    FROM lease_contracts fallback_contract
                    WHERE fallback_contract.deleted_at IS NULL
                      AND NOT EXISTS (
                          SELECT 1
                          FROM contract_occupants primary_occupant
                          WHERE primary_occupant.contract_id = fallback_contract.lease_contract_id
                            AND primary_occupant.tenant_profile_id =
                                fallback_contract.primary_tenant_profile_id
                      )
                ) co
                JOIN person_profiles pp
                    ON pp.person_profile_id = co.tenant_profile_id
                    AND pp.deleted_at IS NULL
                JOIN lease_contracts lc
                    ON lc.lease_contract_id = co.contract_id
                    AND lc.deleted_at IS NULL
                LEFT JOIN deposit_agreements da
                    ON da.deposit_agreement_id = lc.deposit_agreement_id
                LEFT JOIN deposit_forms df
                    ON df.deposit_form_id = da.deposit_form_id
                LEFT JOIN users u
                    ON u.user_id = pp.user_id
                    AND u.deleted_at IS NULL
                LEFT JOIN tenant_account_provisionings tap
                    ON tap.tenant_profile_id = pp.person_profile_id
                WHERE co.contract_id = ?
                ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.id
                """, (rs, rowNum) -> new LeaseContractQueryDetailsResponse.OccupantInfo(
                getLongOrNull(rs, "tenant_profile_id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                toLocalDate(rs, "dob"),
                rs.getString("permanent_address"),
                normalizeIdentityNumber(rs.getString("citizen_id")),
                toLocalDate(rs, "identity_issued_date"),
                rs.getString("identity_issued_place"),
                OccupantRole.valueOf(rs.getString("occupant_role")),
                toLocalDate(rs, "move_in_date"),
                toLocalDate(rs, "move_out_date"),
                OccupantStatus.valueOf(rs.getString("status")),
                TenantAccountProvisioningStatus.valueOf(rs.getString("account_status")),
                toLocalDateTime(rs, "account_sent_at"),
                toLocalDateTime(rs, "last_login_at"),
                getBooleanOrNull(rs, "must_change_password")
        ), contractId);
    }

    private AccountProvisioningSummary resolveAccountProvisioning(
            LeaseStatus contractStatus,
            List<LeaseContractQueryDetailsResponse.OccupantInfo> occupants
    ) {
        if (occupants.isEmpty()) {
            return new AccountProvisioningSummary(
                    TenantAccountProvisioningStatus.NOT_PROVISIONED.name(),
                    false
            );
        }
        boolean missingRecipientEmail = occupants.stream()
                .filter(item -> item.occupantRole() == OccupantRole.PRIMARY)
                .map(LeaseContractQueryDetailsResponse.OccupantInfo::email)
                .allMatch(email -> email == null || email.isBlank());
        if (missingRecipientEmail) {
            return new AccountProvisioningSummary("MISSING_EMAIL", false);
        }
        if (occupants.stream().anyMatch(item ->
                item.accountStatus() == TenantAccountProvisioningStatus.FAILED)) {
            return new AccountProvisioningSummary(
                    TenantAccountProvisioningStatus.FAILED.name(),
                    contractStatus == LeaseStatus.ACTIVE
            );
        }
        if (occupants.stream().anyMatch(item ->
                item.accountStatus() == TenantAccountProvisioningStatus.NOT_PROVISIONED)) {
            return new AccountProvisioningSummary(
                    TenantAccountProvisioningStatus.NOT_PROVISIONED.name(),
                    contractStatus == LeaseStatus.ACTIVE
            );
        }
        if (occupants.stream().anyMatch(item ->
                item.accountStatus() == TenantAccountProvisioningStatus.PENDING)) {
            return new AccountProvisioningSummary(
                    TenantAccountProvisioningStatus.PENDING.name(),
                    false
            );
        }
        if (!occupants.isEmpty() && occupants.stream().allMatch(item ->
                item.accountStatus() == TenantAccountProvisioningStatus.ACTIVE)) {
            return new AccountProvisioningSummary(
                    TenantAccountProvisioningStatus.ACTIVE.name(),
                    false
            );
        }
        return new AccountProvisioningSummary(
                TenantAccountProvisioningStatus.SENT.name(),
                false
        );
    }

    private List<LeaseContractQueryDetailsResponse.EventInfo> findEvents(Long contractId) {
        return jdbcTemplate.query("""
                        SELECT contract_event_id AS id, event_type, event_data, created_at
                        FROM contract_events
                        WHERE contract_id = ?
                        ORDER BY created_at ASC, contract_event_id ASC
                        """,
                (rs, rowNum) -> new LeaseContractQueryDetailsResponse.EventInfo(
                        rs.getLong("id"),
                        rs.getString("event_type"),
                        bytesToString(rs.getBytes("event_data")),
                        toLocalDateTime(rs, "created_at")
                ),
                contractId
        );
    }

    private String bytesToString(byte[] bytes) {
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    private String normalizeIdentityNumber(String value) {
        if (value == null || value.isBlank() || value.startsWith("PENDING-")) {
            return null;
        }
        return value;
    }

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getIntOrNull(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Boolean getBooleanOrNull(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDate toLocalDate(ResultSet rs, String column) throws SQLException {
        var date = rs.getDate(column);
        return date != null ? date.toLocalDate() : null;
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private record CurrentUser(Long userId, Role role) {
    }

    private record Scope(Long tenantId, Long propertyId, Long userId, Role role) {
    }

    private record RoomInfoRow(Long id, String roomCode, String roomName, Long propertyId) {
        private RoomInfoRow(Long id, String roomCode, String roomName) {
            this(id, roomCode, roomName, null);
        }
    }

    private record AccountProvisioningSummary(String status, boolean canSend) {
    }

    // ── Tenant room switcher ────────────────────────────────────────────────

    public record ActiveRoomItem(
            Long contractId,
            String contractCode,
            Long roomId,
            String roomCode,
            String roomName,
            String roomStatus,
            Long propertyId,
            String propertyName,
            String roleInContract,
            boolean isPrimary,
            String contractStatus,
            LocalDate startDate,
            LocalDate endDate,
            Long monthlyRent,
            int occupantCount
    ) {}

    /**
     * Returns the list of rooms where the authenticated user has an
     * currently relevant lease contract, for use in the mobile
     * home-screen room selector.
     */
    public List<ActiveRoomItem> getMyActiveRooms() {
        CurrentUser currentUser = getCurrentUser();
        return getRentalContexts(currentUser.userId());
    }

    public List<ActiveRoomItem> getRentalContexts(Long userId) {
        return jdbcTemplate.query("""
                WITH accessible_profiles AS (
                    SELECT pp.person_profile_id AS id FROM person_profiles pp
                    WHERE pp.user_id = ?
                      AND pp.deleted_at IS NULL
                    UNION
                    SELECT tap.tenant_profile_id
                    FROM tenant_account_provisionings tap
                    JOIN person_profiles pp ON pp.person_profile_id = tap.tenant_profile_id
                    WHERE tap.user_id = ?
                      AND tap.status <> 'DISABLED'
                      AND pp.user_id IS NULL
                      AND pp.deleted_at IS NULL
                ),
                contract_access AS (
                    SELECT lc.lease_contract_id AS contract_id, 0 AS role_rank
                    FROM lease_contracts lc
                    WHERE lc.primary_tenant_profile_id IN (SELECT id FROM accessible_profiles)
                      AND NOT EXISTS (
                          SELECT 1
                          FROM contract_occupants disabled_primary
                          WHERE disabled_primary.contract_id = lc.lease_contract_id
                            AND disabled_primary.tenant_profile_id = lc.primary_tenant_profile_id
                            AND disabled_primary.status = 'DISABLED'
                      )
                    UNION ALL
                    SELECT co.contract_id, 1 AS role_rank
                    FROM contract_occupants co
                    WHERE co.tenant_profile_id IN (SELECT id FROM accessible_profiles)
                      AND co.status = 'ACTIVE'
                      AND co.occupant_role = 'CO_OCCUPANT'
                )
                SELECT
                    lc.lease_contract_id   AS contract_id,
                    lc.contract_code,
                    r.room_id    AS room_id,
                    r.room_code,
                    r.name  AS room_name,
                    r.current_status AS room_status,
                    p.property_id    AS property_id,
                    p.name  AS property_name,
                    CASE WHEN MIN(access.role_rank) = 0 THEN 'PRIMARY' ELSE 'CO_OCCUPANT' END AS role_in_contract,
                    lc.status AS contract_status,
                    lc.start_date,
                    lc.end_date,
                    lc.monthly_rent,
                    (
                        SELECT COUNT(*)
                        FROM contract_occupants occupant_count
                        WHERE occupant_count.contract_id = lc.lease_contract_id
                          AND occupant_count.status = 'ACTIVE'
                    ) AS occupant_count
                FROM contract_access access
                JOIN lease_contracts lc ON lc.lease_contract_id = access.contract_id
                JOIN rooms r ON r.room_id = lc.room_id AND r.deleted_at IS NULL
                JOIN properties p ON p.property_id = r.property_id
                WHERE lc.deleted_at IS NULL
                  AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'EXPIRED')
                GROUP BY
                    lc.lease_contract_id, lc.contract_code, r.room_id, r.room_code, r.name,
                    r.current_status, p.property_id, p.name, lc.status, lc.start_date,
                    lc.end_date, lc.monthly_rent
                ORDER BY
                    CASE lc.status
                        WHEN 'ACTIVE' THEN 0
                        WHEN 'EXPIRING_SOON' THEN 1
                        ELSE 2
                    END,
                    lc.start_date DESC,
                    lc.lease_contract_id DESC
                """,
                (rs, rowNum) -> new ActiveRoomItem(
                        rs.getLong("contract_id"),
                        rs.getString("contract_code"),
                        rs.getLong("room_id"),
                        rs.getString("room_code"),
                        rs.getString("room_name"),
                        rs.getString("room_status"),
                        rs.getLong("property_id"),
                        rs.getString("property_name"),
                        rs.getString("role_in_contract"),
                        "PRIMARY".equals(rs.getString("role_in_contract")),
                        rs.getString("contract_status"),
                        toLocalDate(rs, "start_date"),
                        toLocalDate(rs, "end_date"),
                        rs.getLong("monthly_rent"),
                        rs.getInt("occupant_count")
                ),
                userId,
                userId
        );
    }

    public void assertCurrentUserCanReadContract(Long contractId) {
        CurrentUser currentUser = getCurrentUser();
        if (currentUser.role() != Role.TENANT) {
            return;
        }
        boolean allowed = getRentalContexts(currentUser.userId()).stream()
                .anyMatch(context -> context.contractId().equals(contractId));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xem hop dong nay.");
        }
    }

    public void assertCurrentUserCanReadRoom(Long roomId) {
        CurrentUser currentUser = getCurrentUser();
        if (currentUser.role() != Role.TENANT) {
            return;
        }
        boolean allowed = getRentalContexts(currentUser.userId()).stream()
                .anyMatch(context -> context.roomId().equals(roomId));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xem phong nay.");
        }
    }

    public boolean isCurrentUserTenant() {
        return getCurrentUser().role() == Role.TENANT;
    }
}
