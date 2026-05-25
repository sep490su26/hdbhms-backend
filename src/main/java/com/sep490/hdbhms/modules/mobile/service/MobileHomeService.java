//package com.sep490.hdbhms.modules.mobile.service;
//
//import com.sep490.hdbhms.common.exception.ApiException;
//import com.sep490.hdbhms.modules.auth.dto.OnboardingStateResponse;
//import com.sep490.hdbhms.modules.auth.service.OnboardingService;
//import com.sep490.hdbhms.modules.mobile.dto.MobileHomeResponse;
//import com.sep490.hdbhms.modules.user.entity.User;
//import com.sep490.hdbhms.modules.user.repository.UserRepository;
//import java.math.BigDecimal;
//import java.sql.ResultSet;
//import org.springframework.dao.EmptyResultDataAccessException;
//import org.springframework.http.HttpStatus;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//public class MobileHomeService {
//
//    private final UserRepository userRepository;
//    private final OnboardingService onboardingService;
//    private final JdbcTemplate jdbcTemplate;
//
//    public MobileHomeService(
//            UserRepository userRepository,
//            OnboardingService onboardingService,
//            JdbcTemplate jdbcTemplate
//    ) {
//        this.userRepository = userRepository;
//        this.onboardingService = onboardingService;
//        this.jdbcTemplate = jdbcTemplate;
//    }
//
//    public MobileHomeResponse getHome(Long userId, Long tenantId) {
//        User user = userRepository.findById(userId)
//                .filter(item -> item.getDeletedAt() == null)
//                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ"));
//
//        if (!onboardingService.hasActiveTenant(user, tenantId)) {
//            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem tenant này");
//        }
//
//        MobileHomeResponse.TenantSummary tenant = getTenant(tenantId);
//        MobileHomeResponse.ContractSummary contract = getCurrentContract(tenantId);
//        MobileHomeResponse.RoomSummary room = getCurrentRoom(tenantId);
//        MobileHomeResponse.InvoiceSummary invoiceSummary = getInvoiceSummary(tenantId, contract == null ? null : contract.id());
//        MobileHomeResponse.NotificationSummary notificationSummary = getNotificationSummary(userId);
//        OnboardingStateResponse onboarding = onboardingService.resolve(user);
//
//        return new MobileHomeResponse(
//                new MobileHomeResponse.UserSummary(
//                        user.getId(),
//                        onboardingService.resolveFullName(user),
//                        user.getPhone(),
//                        user.getEmail(),
//                        user.getRole().name()
//                ),
//                tenant,
//                room,
//                contract,
//                invoiceSummary,
//                notificationSummary,
//                onboarding
//        );
//    }
//
//    private MobileHomeResponse.TenantSummary getTenant(Long tenantId) {
//        return jdbcTemplate.queryForObject("""
//                SELECT t.id, p.name
//                FROM tenants t
//                JOIN properties p ON p.id = t.property_id
//                WHERE t.id = ?
//                  AND t.deleted_at IS NULL
//                """, (rs, rowNum) -> new MobileHomeResponse.TenantSummary(
//                rs.getLong("id"),
//                rs.getString("name")
//        ), tenantId);
//    }
//
//    private MobileHomeResponse.RoomSummary getCurrentRoom(Long tenantId) {
//        return queryNullable("""
//                SELECT r.id, r.room_code, r.name, r.current_status
//                FROM contract_occupants co
//                JOIN lease_contracts lc ON lc.id = co.contract_id
//                JOIN rooms r ON r.id = lc.room_id
//                WHERE co.tenant_id = ?
//                  AND co.status = 'ACTIVE'
//                  AND lc.deleted_at IS NULL
//                ORDER BY lc.start_date DESC, lc.id DESC
//                LIMIT 1
//                """, rs -> new MobileHomeResponse.RoomSummary(
//                rs.getLong("id"),
//                rs.getString("room_code"),
//                rs.getString("name"),
//                rs.getString("current_status")
//        ), tenantId);
//    }
//
//    private MobileHomeResponse.ContractSummary getCurrentContract(Long tenantId) {
//        return queryNullable("""
//                SELECT lc.id, lc.contract_code, lc.status, lc.start_date, lc.end_date
//                FROM contract_occupants co
//                JOIN lease_contracts lc ON lc.id = co.contract_id
//                WHERE co.tenant_id = ?
//                  AND co.status = 'ACTIVE'
//                  AND lc.deleted_at IS NULL
//                ORDER BY
//                  CASE lc.status
//                    WHEN 'ACTIVE' THEN 0
//                    WHEN 'EXPIRING_SOON' THEN 1
//                    ELSE 2
//                  END,
//                  lc.start_date DESC,
//                  lc.id DESC
//                LIMIT 1
//                """, rs -> new MobileHomeResponse.ContractSummary(
//                rs.getLong("id"),
//                rs.getString("contract_code"),
//                rs.getString("status"),
//                rs.getDate("start_date").toLocalDate(),
//                rs.getDate("end_date").toLocalDate()
//        ), tenantId);
//    }
//
//    private MobileHomeResponse.InvoiceSummary getInvoiceSummary(Long tenantId, Long contractId) {
//        if (contractId == null) {
//            return new MobileHomeResponse.InvoiceSummary(0, BigDecimal.ZERO, null);
//        }
//
//        return jdbcTemplate.queryForObject("""
//                SELECT COUNT(*) AS unpaid_count,
//                       COALESCE(SUM(remaining_amount), 0) AS total_unpaid_amount,
//                       MIN(due_date) AS nearest_due_date
//                FROM invoices
//                WHERE contract_id = ?
//                  AND status IN ('ISSUED', 'PARTIALLY_PAID', 'OVERDUE')
//                  AND remaining_amount > 0
//                """, (rs, rowNum) -> new MobileHomeResponse.InvoiceSummary(
//                rs.getLong("unpaid_count"),
//                rs.getBigDecimal("total_unpaid_amount"),
//                rs.getDate("nearest_due_date") == null ? null : rs.getDate("nearest_due_date").toLocalDate()
//        ), contractId);
//    }
//
//    private MobileHomeResponse.NotificationSummary getNotificationSummary(Long userId) {
//        Long unreadCount = jdbcTemplate.queryForObject("""
//                SELECT COUNT(*)
//                FROM notification_outbox
//                WHERE recipient_user_id = ?
//                  AND status = 'PENDING'
//                """, Long.class, userId);
//        return new MobileHomeResponse.NotificationSummary(unreadCount == null ? 0 : unreadCount);
//    }
//
//    private <T> T queryNullable(String sql, RowMapperFunction<T> mapper, Object... args) {
//        try {
//            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapper.map(rs), args);
//        } catch (EmptyResultDataAccessException ex) {
//            return null;
//        }
//    }
//
//    @FunctionalInterface
//    private interface RowMapperFunction<T> {
//        T map(ResultSet resultSet) throws java.sql.SQLException;
//    }
//}
