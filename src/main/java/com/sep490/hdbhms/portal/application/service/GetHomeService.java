package com.sep490.hdbhms.portal.application.service;

import com.sep490.hdbhms.portal.application.port.in.query.GetHomeQuery;
import com.sep490.hdbhms.portal.application.port.in.usecase.GetHomeUseCase;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.*;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetHomeService implements GetHomeUseCase {

    JdbcTemplate jdbcTemplate;
    LeaseContractQueryService leaseContractQueryService;

    @Override
    public HomeResponse execute(GetHomeQuery query) {
        Long userId = query.userId();

        // 1. Fetch User, Tenant, and Profile info
        HomeResponse response = jdbcTemplate.query("""
                SELECT 
                    u.user_id AS user_id, u.email, u.phone AS user_phone, u.role,
                    t.tenant_id AS tenant_id, prop.property_id AS property_id,
                    prop.name AS tenant_name, prop.address_line AS property_address,
                    p.person_profile_id AS profile_id, p.full_name, p.phone AS profile_phone, p.portrait_file_id
                FROM users u
                LEFT JOIN tenants t ON t.user_id = u.user_id AND t.deleted_at IS NULL
                LEFT JOIN properties prop ON prop.property_id = t.property_id AND prop.deleted_at IS NULL
                LEFT JOIN person_profiles p ON p.user_id = u.user_id AND p.deleted_at IS NULL
                WHERE u.user_id = ? AND u.deleted_at IS NULL
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
                                .address(rs.getString("property_address"))
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

        // 2. Resolve every rental context available to this user and select one.
        List<LeaseContractQueryService.ActiveRoomItem> rentalContexts =
                leaseContractQueryService.getRentalContexts(userId);
        LeaseContractQueryService.ActiveRoomItem selectedContext = null;
        if (query.contractId() != null) {
            selectedContext = rentalContexts.stream()
                    .filter(context -> context.contractId().equals(query.contractId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Ban khong co quyen xem phong hoac hop dong nay."
                    ));
        } else if (!rentalContexts.isEmpty()) {
            selectedContext = rentalContexts.getFirst();
        }

        List<RoomHomeResponse> rooms = rentalContexts.stream()
                .map(context -> RoomHomeResponse.builder()
                        .id(context.roomId())
                        .roomCode(context.roomCode())
                        .name(context.roomName())
                        .currentStatus(context.roomStatus())
                        .build())
                .toList();
        response.setRooms(rooms.isEmpty() ? null : rooms);

        if (selectedContext != null) {
            response.setContract(ContractHomeResponse.builder()
                    .id(selectedContext.contractId())
                    .contractCode(selectedContext.contractCode())
                    .status(selectedContext.contractStatus())
                    .startDate(selectedContext.startDate() == null
                            ? null
                            : selectedContext.startDate().atStartOfDay())
                    .endDate(selectedContext.endDate() == null
                            ? null
                            : selectedContext.endDate().atStartOfDay())
                    .build());
            response.setRoom(RoomHomeResponse.builder()
                    .id(selectedContext.roomId())
                    .roomCode(selectedContext.roomCode())
                    .name(selectedContext.roomName())
                    .currentStatus(selectedContext.roomStatus())
                    .build());
            response.setTenant(resolveTenant(userId, selectedContext));
        }

        // 3. Invoice Summary
        if (response.getContract() != null) {
            Long contractId = response.getContract().getId();
            jdbcTemplate.query("""
                    SELECT 
                        COUNT(invoice_id) AS unpaid_count,
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
                    JOIN meters m ON m.meter_id = mr.meter_id
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
                    JOIN meters m ON m.meter_id = mr.meter_id
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

    private TenantHomeResponse resolveTenant(
            Long userId,
            LeaseContractQueryService.ActiveRoomItem context
    ) {
        Long tenantId = jdbcTemplate.query("""
                        SELECT t.tenant_id AS id
                        FROM tenants t
                        WHERE t.user_id = ?
                          AND t.property_id = ?
                          AND t.deleted_at IS NULL
                        ORDER BY t.tenant_id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userId,
                context.propertyId()
        );
        return TenantHomeResponse.builder()
                .id(tenantId)
                .name(context.propertyName())
                .address(getPropertyAddress(context.propertyId()))
                .imageUrls(getPropertyImageUrls(context.propertyId()))
                .build();
    }

    private String getPropertyAddress(Long propertyId) {
        if (propertyId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                        SELECT address_line
                        FROM properties
                        WHERE property_id = ?
                          AND deleted_at IS NULL
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getString("address_line") : null,
                propertyId
        );
    }

    private List<String> getPropertyImageUrls(Long propertyId) {
        if (propertyId == null) {
            return List.of();
        }
        return jdbcTemplate.query("""
                        SELECT ri.file_id
                        FROM room_images ri
                        JOIN rooms r ON r.room_id = ri.room_id
                        WHERE r.property_id = ?
                          AND r.deleted_at IS NULL
                        ORDER BY ri.sort_order ASC, ri.created_at ASC, ri.file_id ASC
                        LIMIT 6
                        """,
                (rs, rowNum) -> "/api/v1/files/download/" + rs.getLong("file_id"),
                propertyId
        );
    }
}
