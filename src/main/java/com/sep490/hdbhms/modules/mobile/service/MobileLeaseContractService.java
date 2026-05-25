//package com.sep490.hdbhms.modules.mobile.service;
//
//import com.sep490.hdbhms.common.exception.ApiException;
//import com.sep490.hdbhms.modules.auth.service.OnboardingService;
//import com.sep490.hdbhms.modules.mobile.dto.MobileContractListItem;
//import com.sep490.hdbhms.modules.mobile.dto.MobileLeaseContractResponse;
//import com.sep490.hdbhms.modules.user.entity.User;
//import com.sep490.hdbhms.modules.user.repository.UserRepository;
//import java.math.BigDecimal;
//import java.sql.ResultSet;
//import java.text.DecimalFormat;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//import org.springframework.dao.EmptyResultDataAccessException;
//import org.springframework.http.HttpStatus;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
//
//@Service
//public class MobileLeaseContractService {
//
//    private static final DateTimeFormatter VI_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//
//    private final UserRepository userRepository;
//    private final OnboardingService onboardingService;
//    private final JdbcTemplate jdbcTemplate;
//
//    public MobileLeaseContractService(
//            UserRepository userRepository,
//            OnboardingService onboardingService,
//            JdbcTemplate jdbcTemplate
//    ) {
//        this.userRepository = userRepository;
//        this.onboardingService = onboardingService;
//        this.jdbcTemplate = jdbcTemplate;
//    }
//
//    public List<MobileContractListItem> listMyContracts(Long userId, Long tenantId) {
//        requireActiveTenant(userId, tenantId);
//
//        return jdbcTemplate.query("""
//                SELECT lc.id,
//                       lc.contract_code,
//                       r.room_code,
//                       lc.signed_at,
//                       lc.status
//                FROM contract_occupants co
//                JOIN lease_contracts lc ON lc.id = co.contract_id
//                JOIN rooms r ON r.id = lc.room_id
//                WHERE co.tenant_id = ?
//                  AND co.status = 'ACTIVE'
//                  AND lc.deleted_at IS NULL
//                  AND r.deleted_at IS NULL
//                ORDER BY
//                  CASE lc.status
//                    WHEN 'ACTIVE' THEN 0
//                    WHEN 'EXPIRING_SOON' THEN 1
//                    WHEN 'EXPIRED' THEN 2
//                    ELSE 3
//                  END,
//                  lc.start_date DESC,
//                  lc.id DESC
//                """, (rs, rowNum) -> new MobileContractListItem(
//                rs.getLong("id"),
//                rs.getString("contract_code"),
//                rs.getString("room_code"),
//                rs.getDate("signed_at") != null ? rs.getDate("signed_at").toLocalDate() : null,
//                rs.getString("status")
//        ), tenantId);
//    }
//
//    public MobileLeaseContractResponse getContractById(Long userId, Long tenantId, Long contractId) {
//        requireActiveTenant(userId, tenantId);
//
//        MobileLeaseContractResponse contract = queryNullable("""
//                SELECT lc.id,
//                       lc.contract_code,
//                       lc.status,
//                       lc.start_date,
//                       lc.end_date,
//                       lc.rent_start_date,
//                       lc.monthly_rent,
//                       lc.payment_cycle_months,
//                       lc.deposit_amount,
//                       lc.contract_file_id,
//                       r.id AS room_id,
//                       r.room_code,
//                       r.name AS room_name,
//                       r.area_m2,
//                       (
//                           SELECT ri.file_id
//                           FROM room_images ri
//                           JOIN file_metadata fm ON fm.id = ri.file_id
//                           WHERE ri.room_id = r.id
//                             AND fm.deleted_at IS NULL
//                             AND fm.is_sensitive = FALSE
//                           ORDER BY ri.sort_order ASC, ri.id ASC
//                           LIMIT 1
//                       ) AS room_image_file_id,
//                       (
//                           SELECT ut.unit_price
//                           FROM utility_tariffs ut
//                           WHERE ut.utility_type = 'SERVICE_FEE'
//                             AND (ut.property_id = r.property_id OR ut.property_id IS NULL)
//                             AND ut.effective_from <= CURRENT_DATE
//                             AND (ut.effective_to IS NULL OR ut.effective_to >= CURRENT_DATE)
//                           ORDER BY
//                             CASE WHEN ut.property_id = r.property_id THEN 0 ELSE 1 END,
//                             ut.effective_from DESC,
//                             ut.id DESC
//                           LIMIT 1
//                       ) AS service_fee
//                FROM contract_occupants co
//                JOIN lease_contracts lc ON lc.id = co.contract_id
//                JOIN rooms r ON r.id = lc.room_id
//                WHERE co.tenant_id = ?
//                  AND co.status = 'ACTIVE'
//                  AND lc.id = ?
//                  AND lc.deleted_at IS NULL
//                  AND r.deleted_at IS NULL
//                """, this::mapContract, tenantId, contractId);
//
//        if (contract == null) {
//            throw new ApiException(
//                    HttpStatus.NOT_FOUND,
//                    "CONTRACT_NOT_FOUND",
//                    "Không tìm thấy hợp đồng"
//            );
//        }
//
//        return withTerms(contract, buildTerms(contract));
//    }
//
//    public MobileLeaseContractResponse getMyActiveContract(Long userId, Long tenantId) {
//        requireActiveTenant(userId, tenantId);
//
//        MobileLeaseContractResponse contract = queryNullable("""
//                SELECT lc.id,
//                       lc.contract_code,
//                       lc.status,
//                       lc.start_date,
//                       lc.end_date,
//                       lc.rent_start_date,
//                       lc.monthly_rent,
//                       lc.payment_cycle_months,
//                       lc.deposit_amount,
//                       lc.contract_file_id,
//                       r.id AS room_id,
//                       r.room_code,
//                       r.name AS room_name,
//                       r.area_m2,
//                       (
//                           SELECT ri.file_id
//                           FROM room_images ri
//                           JOIN file_metadata fm ON fm.id = ri.file_id
//                           WHERE ri.room_id = r.id
//                             AND fm.deleted_at IS NULL
//                             AND fm.is_sensitive = FALSE
//                           ORDER BY ri.sort_order ASC, ri.id ASC
//                           LIMIT 1
//                       ) AS room_image_file_id,
//                       (
//                           SELECT ut.unit_price
//                           FROM utility_tariffs ut
//                           WHERE ut.utility_type = 'SERVICE_FEE'
//                             AND (ut.property_id = r.property_id OR ut.property_id IS NULL)
//                             AND ut.effective_from <= CURRENT_DATE
//                             AND (ut.effective_to IS NULL OR ut.effective_to >= CURRENT_DATE)
//                           ORDER BY
//                             CASE WHEN ut.property_id = r.property_id THEN 0 ELSE 1 END,
//                             ut.effective_from DESC,
//                             ut.id DESC
//                           LIMIT 1
//                       ) AS service_fee
//                FROM contract_occupants co
//                JOIN lease_contracts lc ON lc.id = co.contract_id
//                JOIN rooms r ON r.id = lc.room_id
//                WHERE co.tenant_id = ?
//                  AND co.status = 'ACTIVE'
//                  AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'EXPIRED')
//                  AND lc.deleted_at IS NULL
//                  AND r.deleted_at IS NULL
//                ORDER BY
//                  CASE lc.status
//                    WHEN 'ACTIVE' THEN 0
//                    WHEN 'EXPIRING_SOON' THEN 1
//                    WHEN 'EXPIRED' THEN 2
//                    ELSE 3
//                  END,
//                  lc.start_date DESC,
//                  lc.id DESC
//                LIMIT 1
//                """, this::mapContract, tenantId);
//
//        if (contract == null) {
//            throw new ApiException(
//                    HttpStatus.NOT_FOUND,
//                    "CONTRACT_NOT_FOUND",
//                    "Bạn chưa có hợp đồng thuê phòng đang hiệu lực"
//            );
//        }
//
//        return withTerms(contract, buildTerms(contract));
//    }
//
//    private MobileLeaseContractResponse mapContract(ResultSet rs) throws java.sql.SQLException {
//        Long roomImageFileId = nullableLong(rs, "room_image_file_id");
//        Long contractFileId = nullableLong(rs, "contract_file_id");
//
//        return new MobileLeaseContractResponse(
//                rs.getLong("id"),
//                rs.getString("contract_code"),
//                rs.getString("status"),
//                new MobileLeaseContractResponse.RoomDto(
//                        rs.getLong("room_id"),
//                        rs.getString("room_code"),
//                        rs.getString("room_name"),
//                        rs.getBigDecimal("area_m2"),
//                        fileUrl(roomImageFileId)
//                ),
//                rs.getDate("start_date").toLocalDate(),
//                rs.getDate("end_date").toLocalDate(),
//                rs.getDate("rent_start_date").toLocalDate(),
//                rs.getBigDecimal("monthly_rent"),
//                rs.getInt("payment_cycle_months"),
//                rs.getBigDecimal("deposit_amount"),
//                nullableBigDecimal(rs, "service_fee"),
//                List.of(),
//                fileUrl(contractFileId)
//        );
//    }
//
//    private MobileLeaseContractResponse withTerms(
//            MobileLeaseContractResponse contract,
//            List<String> terms
//    ) {
//        return new MobileLeaseContractResponse(
//                contract.id(),
//                contract.contractCode(),
//                contract.status(),
//                contract.room(),
//                contract.startDate(),
//                contract.endDate(),
//                contract.rentStartDate(),
//                contract.monthlyRent(),
//                contract.paymentCycleMonths(),
//                contract.depositAmount(),
//                contract.serviceFee(),
//                terms,
//                contract.contractFileUrl()
//        );
//    }
//
//    private List<String> buildTerms(MobileLeaseContractResponse contract) {
//        List<String> terms = new ArrayList<>();
//        terms.add("Tiền thuê hàng tháng là " + formatMoney(contract.monthlyRent())
//                + ", thanh toán theo chu kỳ " + contract.paymentCycleMonths() + " tháng.");
//        terms.add("Thời hạn thuê từ " + VI_DATE_FORMATTER.format(contract.startDate())
//                + " đến " + VI_DATE_FORMATTER.format(contract.endDate()) + ".");
//        terms.add("Ngày bắt đầu tính tiền thuê là "
//                + VI_DATE_FORMATTER.format(contract.rentStartDate()) + ".");
//        terms.add("Tiền đặt cọc là " + formatMoney(contract.depositAmount()) + ".");
//        if (contract.serviceFee() != null && contract.serviceFee().compareTo(BigDecimal.ZERO) > 0) {
//            terms.add("Phí dịch vụ dự kiến là " + formatMoney(contract.serviceFee()) + " mỗi tháng.");
//        }
//        return terms;
//    }
//
//    private String formatMoney(BigDecimal amount) {
//        if (amount == null) {
//            return "0đ";
//        }
//        return new DecimalFormat("#,###").format(amount).replace(',', '.') + "đ";
//    }
//
//    private String fileUrl(Long fileId) {
//        if (fileId == null) {
//            return null;
//        }
//        return ServletUriComponentsBuilder.fromCurrentContextPath()
//                .path("/api/v1/files/{fileId}")
//                .buildAndExpand(fileId)
//                .toUriString();
//    }
//
//    private Long nullableLong(ResultSet rs, String column) throws java.sql.SQLException {
//        long value = rs.getLong(column);
//        return rs.wasNull() ? null : value;
//    }
//
//    private BigDecimal nullableBigDecimal(ResultSet rs, String column) throws java.sql.SQLException {
//        BigDecimal value = rs.getBigDecimal(column);
//        return rs.wasNull() ? null : value;
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
//    private void requireActiveTenant(Long userId, Long tenantId) {
//        User user = userRepository.findById(userId)
//                .filter(item -> item.getDeletedAt() == null)
//                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ"));
//
//        if (!onboardingService.hasActiveTenant(user, tenantId)) {
//            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem hợp đồng này");
//        }
//    }
//
//    @FunctionalInterface
//    private interface RowMapperFunction<T> {
//        T map(ResultSet resultSet) throws java.sql.SQLException;
//    }
//}
