package com.sep490.hdbhms.portal.application.service;

import com.sep490.hdbhms.portal.application.port.in.query.GetHomeQuery;
import com.sep490.hdbhms.portal.application.port.in.usecase.GetHomeUseCase;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.*;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetHomeService implements GetHomeUseCase {

    JdbcTemplate jdbcTemplate;

    @Override
    public HomeResponse execute(GetHomeQuery query) {
        Long userId = query.userId();

        // 1. Fetch User, Tenant, and Profile info
        HomeResponse response = jdbcTemplate.query("""
                SELECT 
                    u.id AS user_id, u.email, u.phone AS user_phone, u.role,
                    t.id AS tenant_id, prop.name AS tenant_name,
                    p.id AS profile_id, p.full_name, p.phone AS profile_phone, p.portrait_file_id
                FROM users u
                LEFT JOIN tenants t ON t.user_id = u.id AND t.deleted_at IS NULL
                LEFT JOIN properties prop ON prop.id = t.property_id AND prop.deleted_at IS NULL
                LEFT JOIN person_profiles p ON p.user_id = u.id AND p.deleted_at IS NULL
                WHERE u.id = ? AND u.deleted_at IS NULL
                """,
                rs -> {
                    if (!rs.next()) {
                        throw new AppException(ApiErrorCode.UNAUTHENTICATED);
                    }
                    
                    String avatarUrl = null;
                    Long portraitFileId = rs.getLong("portrait_file_id");
                    if (!rs.wasNull()) {
                        avatarUrl = "/files/download/" + portraitFileId;
                    }
                    
                    UserHomeResponse user = UserHomeResponse.builder()
                            .id(rs.getLong("user_id"))
                            .fullName(rs.getString("full_name"))
                            .phone(rs.getString("profile_phone") != null ? rs.getString("profile_phone") : rs.getString("user_phone"))
                            .email(rs.getString("email"))
                            .role(rs.getString("role"))
                            .avatarUrl(avatarUrl)
                            .build();
                            
                    TenantHomeResponse tenant = null;
                    Long tenantId = rs.getLong("tenant_id");
                    if (!rs.wasNull()) {
                        tenant = TenantHomeResponse.builder()
                                .id(tenantId)
                                .name(rs.getString("tenant_name"))
                                .build();
                    }
                    
                    HomeResponse hr = new HomeResponse();
                    hr.setUser(user);
                    hr.setTenant(tenant);
                    return hr;
                }, userId);

        if (response == null || response.getTenant() == null) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }

        Long tenantId = response.getTenant().getId();

        // 2. Fetch All Active Contract Rooms for Tenant
        List<RoomHomeResponse> rooms = new ArrayList<>();
        ContractHomeResponse firstContract = null;
        jdbcTemplate.query("""
                SELECT DISTINCT
                    lc.id AS contract_id, lc.contract_code, lc.status AS contract_status, lc.start_date, lc.end_date,
                    r.id AS room_id, r.room_code, r.name AS room_name, r.current_status AS room_status
                FROM lease_contracts lc
                JOIN rooms r ON r.id = lc.room_id
                LEFT JOIN contract_occupants co ON co.contract_id = lc.id
                LEFT JOIN person_profiles pp_scope ON pp_scope.id = co.tenant_profile_id
                WHERE (
                      lc.primary_tenant_profile_id = (SELECT id FROM person_profiles WHERE user_id = ? AND deleted_at IS NULL LIMIT 1)
                      OR co.tenant_id IN (SELECT id FROM tenants WHERE user_id = ? AND deleted_at IS NULL)
                      OR pp_scope.user_id = ?
                  )
                  AND lc.status IN ('ACTIVE', 'EXPIRING_SOON')
                  AND lc.deleted_at IS NULL
                ORDER BY lc.id DESC
                """,
                rs -> {
                    while (rs.next()) {
                        ContractHomeResponse contract = ContractHomeResponse.builder()
                                .id(rs.getLong("contract_id"))
                                .contractCode(rs.getString("contract_code"))
                                .status(rs.getString("contract_status"))
                                .startDate(rs.getTimestamp("start_date") != null ? rs.getTimestamp("start_date").toLocalDateTime() : null)
                                .endDate(rs.getTimestamp("end_date") != null ? rs.getTimestamp("end_date").toLocalDateTime() : null)
                                .build();

                        RoomHomeResponse room = RoomHomeResponse.builder()
                                .id(rs.getLong("room_id"))
                                .roomCode(rs.getString("room_code"))
                                .name(rs.getString("room_name"))
                                .currentStatus(rs.getString("room_status"))
                                .build();

                        rooms.add(room);
                        if (rooms.size() == 1) {
                            // Use the first (most recent) active contract for invoice/utility context
                            response.setContract(contract);
                            response.setRoom(room);
                        }
                    }
                    return null;
                }, userId, userId, userId);
        response.setRooms(rooms.isEmpty() ? null : rooms);

        // 3. Invoice Summary
        if (response.getContract() != null) {
            Long contractId = response.getContract().getId();
            jdbcTemplate.query("""
                    SELECT 
                        COUNT(id) AS unpaid_count,
                        SUM(remaining_amount) AS total_unpaid_amount,
                        MIN(due_date) AS nearest_due_date
                    FROM invoices
                    WHERE lease_contract_id = ? 
                      AND status IN ('ISSUED', 'PARTIALLY_PAID', 'OVERDUE')
                    """,
                    rs -> {
                        if (rs.next()) {
                            response.setInvoiceSummary(InvoiceSummaryHomeResponse.builder()
                                    .unpaidCount(rs.getInt("unpaid_count"))
                                    .totalUnpaidAmount(rs.getDouble("total_unpaid_amount"))
                                    .nearestDueDate(rs.getTimestamp("nearest_due_date") != null ? rs.getTimestamp("nearest_due_date").toLocalDateTime() : null)
                                    .build());
                        } else {
                            response.setInvoiceSummary(new InvoiceSummaryHomeResponse(0, 0.0, null));
                        }
                        return null;
                    }, contractId);
        } else {
            response.setInvoiceSummary(new InvoiceSummaryHomeResponse(0, 0.0, null));
        }

        // 4. Utility Summary
        response.setUtilitySummary(new UtilitySummaryHomeResponse());
        
        if (response.getRoom() != null) {
            Long roomId = response.getRoom().getId();
            // Fetch electricity reading
            jdbcTemplate.query("""
                    SELECT mr.usage_amount, mr.status
                    FROM meter_readings mr
                    JOIN meters m ON m.id = mr.meter_id
                    WHERE mr.room_id = ? AND m.meter_type = 'ELECTRICITY' AND mr.status != 'VOIDED'
                    ORDER BY mr.reading_date DESC
                    LIMIT 1
                    """,
                    rs -> {
                        if (rs.next()) {
                            response.getUtilitySummary().setElectricity(UtilityUsageResponse.builder()
                                    .name("Điện")
                                    .value(rs.getDouble("usage_amount"))
                                    .unit("kWh")
                                    .percentChange(0.0)
                                    .status(rs.getString("status"))
                                    .build());
                        }
                        return null;
                    }, roomId);

            // Fetch water reading
            jdbcTemplate.query("""
                    SELECT mr.usage_amount, mr.status
                    FROM meter_readings mr
                    JOIN meters m ON m.id = mr.meter_id
                    WHERE mr.room_id = ? AND m.meter_type = 'WATER' AND mr.status != 'VOIDED'
                    ORDER BY mr.reading_date DESC
                    LIMIT 1
                    """,
                    rs -> {
                        if (rs.next()) {
                            response.getUtilitySummary().setWater(UtilityUsageResponse.builder()
                                    .name("Nước")
                                    .value(rs.getDouble("usage_amount"))
                                    .unit("m3")
                                    .percentChange(0.0)
                                    .status(rs.getString("status"))
                                    .build());
                        }
                        return null;
                    }, roomId);
        }

        return response;
    }
}
