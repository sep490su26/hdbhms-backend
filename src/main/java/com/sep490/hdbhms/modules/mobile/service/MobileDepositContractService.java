package com.sep490.hdbhms.modules.mobile.service;

import com.sep490.hdbhms.common.exception.ApiException;
import com.sep490.hdbhms.modules.auth.service.OnboardingService;
import com.sep490.hdbhms.modules.mobile.dto.MobileContractListItem;
import com.sep490.hdbhms.modules.mobile.dto.MobileDepositContractResponse;
import com.sep490.hdbhms.modules.user.entity.User;
import com.sep490.hdbhms.modules.user.repository.UserRepository;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class MobileDepositContractService {

    private final UserRepository userRepository;
    private final OnboardingService onboardingService;
    private final JdbcTemplate jdbcTemplate;

    public MobileDepositContractService(
            UserRepository userRepository,
            OnboardingService onboardingService,
            JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.onboardingService = onboardingService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MobileContractListItem> listMyDeposits(Long userId, Long tenantId) {
        requireActiveTenant(userId, tenantId);

        return jdbcTemplate.query("""
                SELECT da.id,
                       da.deposit_code AS contract_code,
                       r.room_code,
                       da.created_at AS signed_at,
                       da.status
                FROM deposit_agreements da
                JOIN rooms r ON r.id = da.room_id
                WHERE da.tenant_id = ?
                  AND r.deleted_at IS NULL
                ORDER BY
                  CASE da.status
                    WHEN 'CONFIRMED' THEN 0
                    WHEN 'PAID' THEN 1
                    WHEN 'PENDING_PAYMENT' THEN 2
                    WHEN 'CONVERTED_TO_LEASE' THEN 3
                    ELSE 4
                  END,
                  da.created_at DESC,
                  da.id DESC
                """, (rs, rowNum) -> new MobileContractListItem(
                rs.getLong("id"),
                rs.getString("contract_code"),
                rs.getString("room_code"),
                rs.getDate("signed_at") != null ? rs.getDate("signed_at").toLocalDate() : null,
                rs.getString("status")
        ), tenantId);
    }

    public MobileDepositContractResponse getDepositById(Long userId, Long tenantId, Long depositId) {
        requireActiveTenant(userId, tenantId);

        MobileDepositContractResponse deposit = queryNullable("""
                SELECT da.id,
                       da.deposit_code,
                       da.status,
                       da.amount,
                       da.expected_move_in_date,
                       da.expected_lease_sign_date,
                       da.deposit_expires_at,
                       da.created_at,
                       da.note,
                       da.contract_file_id,
                       r.id AS room_id,
                       r.room_code,
                       r.name AS room_name,
                       r.area_m2,
                       (
                           SELECT ri.file_id
                           FROM room_images ri
                           JOIN file_metadata fm ON fm.id = ri.file_id
                           WHERE ri.room_id = r.id
                             AND fm.deleted_at IS NULL
                             AND fm.is_sensitive = FALSE
                           ORDER BY ri.sort_order ASC, ri.id ASC
                           LIMIT 1
                       ) AS room_image_file_id
                FROM deposit_agreements da
                JOIN rooms r ON r.id = da.room_id
                WHERE da.tenant_id = ?
                  AND da.id = ?
                  AND r.deleted_at IS NULL
                """, rs -> {
            Long roomImageFileId = nullableLong(rs, "room_image_file_id");
            Long contractFileId = nullableLong(rs, "contract_file_id");

            return new MobileDepositContractResponse(
                    rs.getLong("id"),
                    rs.getString("deposit_code"),
                    rs.getString("status"),
                    new MobileDepositContractResponse.RoomDto(
                            rs.getLong("room_id"),
                            rs.getString("room_code"),
                            rs.getString("room_name"),
                            rs.getBigDecimal("area_m2"),
                            fileUrl(roomImageFileId)
                    ),
                    rs.getBigDecimal("amount"),
                    rs.getDate("expected_move_in_date") != null ? rs.getDate("expected_move_in_date").toLocalDate() : null,
                    rs.getDate("expected_lease_sign_date") != null ? rs.getDate("expected_lease_sign_date").toLocalDate() : null,
                    rs.getDate("deposit_expires_at") != null ? rs.getDate("deposit_expires_at").toLocalDate() : null,
                    rs.getDate("created_at") != null ? rs.getDate("created_at").toLocalDate() : null,
                    rs.getString("note"),
                    fileUrl(contractFileId)
            );
        }, tenantId, depositId);

        if (deposit == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "DEPOSIT_NOT_FOUND",
                    "Không tìm thấy hợp đồng cọc"
            );
        }

        return deposit;
    }

    private void requireActiveTenant(Long userId, Long tenantId) {
        User user = userRepository.findById(userId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ"));

        if (!onboardingService.hasActiveTenant(user, tenantId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem hợp đồng cọc này");
        }
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

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private <T> T queryNullable(String sql, RowMapperFunction<T> mapper, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapper.map(rs), args);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    @FunctionalInterface
    private interface RowMapperFunction<T> {
        T map(java.sql.ResultSet resultSet) throws java.sql.SQLException;
    }
}
