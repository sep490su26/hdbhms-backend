package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantRole;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
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
                    lc.id AS contract_id,
                    lc.contract_code,
                    r.id AS room_id,
                    r.room_code,
                    p.id AS property_id,
                    p.name AS property_name,
                    pp.full_name AS primary_tenant_name,
                    (
                        SELECT COUNT(*)
                        FROM contract_occupants co_count
                        WHERE co_count.contract_id = lc.id
                          AND co_count.status = 'ACTIVE'
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
                JOIN rooms r ON r.id = lc.room_id
                JOIN properties p ON p.id = r.property_id
                JOIN person_profiles pp ON pp.id = lc.primary_tenant_profile_id
                LEFT JOIN file_metadata fm ON fm.id = lc.contract_file_id
                WHERE lc.deleted_at IS NULL
                  AND p.id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(scope.propertyId());
        appendReadableScope(sql, params, scope);

        if (status != null) {
            sql.append(" AND lc.status = ?\n");
            params.add(status.name());
        }
        if (roomId != null) {
            sql.append(" AND r.id = ?\n");
            params.add(roomId);
        }
        if (propertyId != null) {
            sql.append(" AND p.id = ?\n");
            params.add(propertyId);
        }
        if (tenantProfileId != null) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM contract_occupants co_filter
                         WHERE co_filter.contract_id = lc.id
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
                             LEFT JOIN person_profiles opp ON opp.id = co_keyword.tenant_profile_id
                             WHERE co_keyword.contract_id = lc.id
                               AND (opp.full_name LIKE ? OR opp.phone LIKE ?)
                         )
                     )
                    """);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        sql.append(" ORDER BY lc.start_date DESC, lc.id DESC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> toItem(rs), params.toArray());
    }

    public LeaseContractQueryDetailsResponse getContractDetails(Long tenantId, Long contractId) {
        Scope scope = resolveScope(tenantId);
        LeaseContractQueryDetailsResponse details = jdbcTemplate.query("""
                        SELECT
                            lc.id AS contract_id,
                            lc.contract_code,
                            lc.start_date,
                            lc.end_date,
                            lc.rent_start_date,
                            lc.monthly_rent,
                            lc.payment_cycle_months,
                            lc.deposit_amount,
                            lc.status,
                            lc.signed_at,
                            lc.contract_file_id,
                            fm.original_name AS contract_file_name,
                            r.id AS room_id,
                            r.room_code,
                            r.name AS room_name,
                            p.id AS property_id,
                            p.name AS property_name,
                            p.address_line AS property_address,
                            pp.id AS primary_tenant_profile_id,
                            pp.full_name AS primary_full_name,
                            pp.phone AS primary_phone
                        FROM lease_contracts lc
                        JOIN rooms r ON r.id = lc.room_id
                        JOIN properties p ON p.id = r.property_id
                        JOIN person_profiles pp ON pp.id = lc.primary_tenant_profile_id
                        LEFT JOIN file_metadata fm ON fm.id = lc.contract_file_id
                        WHERE lc.deleted_at IS NULL
                          AND lc.id = ?
                          AND p.id = ?
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
                        SELECT r.id, r.room_code, r.name
                        FROM rooms r
                        JOIN properties p ON p.id = r.property_id
                        WHERE r.id = ?
                          AND p.id = ?
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
                SELECT lc.id
                FROM lease_contracts lc
                WHERE lc.room_id = ?
                  AND lc.deleted_at IS NULL
                """);
        List<Object> params = new ArrayList<>();
        params.add(roomId);
        appendReadableScope(sql, params, scope);
        sql.append(" ORDER BY CASE WHEN lc.status = 'ACTIVE' THEN 0 ELSE 1 END, lc.start_date DESC, lc.id DESC");

        List<Long> contractIds = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("id"), params.toArray());
        List<LeaseContractQueryDetailsResponse> contracts = contractIds.stream()
                .map(contractId -> getContractDetails(tenantId, contractId))
                .toList();
        return new RoomRentalHistoryResponse(room.id(), room.roomCode(), room.roomName(), contracts);
    }

    public RoomRentalHistoryResponse getManagementRoomRentalHistory(Long roomId) {
        CurrentUser currentUser = getCurrentUser();
        RoomInfoRow room = jdbcTemplate.query("""
                        SELECT r.id, r.room_code, r.name, r.property_id
                        FROM rooms r
                        WHERE r.id = ?
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
                        SELECT lc.id
                        FROM lease_contracts lc
                        WHERE lc.room_id = ?
                          AND lc.deleted_at IS NULL
                        ORDER BY CASE WHEN lc.status = 'ACTIVE' THEN 0 ELSE 1 END,
                                 lc.start_date DESC,
                                 lc.id DESC
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
                        JOIN rooms r ON r.id = lc.room_id AND r.deleted_at IS NULL
                        WHERE lc.id = ?
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
                            lc.id AS contract_id,
                            lc.contract_code,
                            lc.start_date,
                            lc.end_date,
                            lc.rent_start_date,
                            lc.monthly_rent,
                            lc.payment_cycle_months,
                            lc.deposit_amount,
                            lc.status,
                            lc.signed_at,
                            lc.contract_file_id,
                            fm.original_name AS contract_file_name,
                            r.id AS room_id,
                            r.room_code,
                            r.name AS room_name,
                            p.id AS property_id,
                            p.name AS property_name,
                            p.address_line AS property_address,
                            pp.id AS primary_tenant_profile_id,
                            pp.full_name AS primary_full_name,
                            pp.phone AS primary_phone
                        FROM lease_contracts lc
                        JOIN rooms r ON r.id = lc.room_id
                        JOIN properties p ON p.id = r.property_id
                        JOIN person_profiles pp ON pp.id = lc.primary_tenant_profile_id
                        LEFT JOIN file_metadata fm ON fm.id = lc.contract_file_id
                        WHERE lc.deleted_at IS NULL
                          AND lc.id = ?
                          AND p.id = ?
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
                         LEFT JOIN person_profiles pp_scope ON pp_scope.id = co_scope.tenant_profile_id
                         WHERE co_scope.contract_id = lc.id
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
                        FROM contract_occupants co
                        LEFT JOIN person_profiles pp ON pp.id = co.tenant_profile_id
                        WHERE co.contract_id = ?
                          AND (co.tenant_id = ? OR pp.user_id = ?)
                        """,
                Integer.class,
                contractId,
                scope.tenantId(),
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
                        WHERE id = ?
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
                            WHERE id = ?
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
        return new LeaseContractQueryDetailsResponse(
                rs.getLong("contract_id"),
                rs.getString("contract_code"),
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
                LeaseStatus.valueOf(rs.getString("status")),
                toLocalDateTime(rs, "signed_at"),
                fileId != null ? new LeaseContractQueryDetailsResponse.ContractFileInfo(fileId, rs.getString("contract_file_name")) : null,
                new LeaseContractQueryDetailsResponse.TenantProfileInfo(
                        rs.getLong("primary_tenant_profile_id"),
                        rs.getString("primary_full_name"),
                        rs.getString("primary_phone")
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
        return new LeaseContractQueryDetailsResponse(
                details.contractId(),
                details.contractCode(),
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
                details.contractFile(),
                details.primaryTenant(),
                occupants,
                events
        );
    }

    private List<LeaseContractQueryDetailsResponse.OccupantInfo> findOccupants(Long contractId) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    COALESCE(pp.id, tenant_pp.id) AS tenant_profile_id,
                    COALESCE(pp.full_name, tenant_pp.full_name) AS full_name,
                    COALESCE(pp.phone, tenant_pp.phone) AS phone,
                    co.occupant_role,
                    co.move_in_date,
                    co.move_out_date,
                    co.status
                FROM contract_occupants co
                LEFT JOIN person_profiles pp ON pp.id = co.tenant_profile_id
                LEFT JOIN tenants t ON t.id = co.tenant_id
                LEFT JOIN person_profiles tenant_pp ON tenant_pp.user_id = t.user_id AND tenant_pp.deleted_at IS NULL
                WHERE co.contract_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(contractId);
        sql.append(" ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.id");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new LeaseContractQueryDetailsResponse.OccupantInfo(
                getLongOrNull(rs, "tenant_profile_id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                OccupantRole.valueOf(rs.getString("occupant_role")),
                toLocalDate(rs, "move_in_date"),
                toLocalDate(rs, "move_out_date"),
                OccupantStatus.valueOf(rs.getString("status"))
        ), params.toArray());
    }

    private List<LeaseContractQueryDetailsResponse.EventInfo> findEvents(Long contractId) {
        return jdbcTemplate.query("""
                        SELECT id, event_type, event_data, created_at
                        FROM contract_events
                        WHERE contract_id = ?
                        ORDER BY created_at ASC, id ASC
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

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getIntOrNull(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
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
}
