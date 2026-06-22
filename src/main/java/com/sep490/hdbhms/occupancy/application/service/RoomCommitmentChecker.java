package com.sep490.hdbhms.occupancy.application.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomCommitmentChecker {
    JdbcTemplate jdbcTemplate;

    public enum Blocker {
        NONE,
        ROOM_HOLD_IN_PROGRESS,
        ROOM_ALREADY_RESERVED_BY_NEW_TENANT,
        APPROVED_TRANSFER_EXISTS,
        FUTURE_CONTRACT_EXISTS
    }

    public Blocker checkRenewBlockers(Long roomId, Long currentContractId) {
        if (hasActiveHold(roomId)) {
            return Blocker.ROOM_HOLD_IN_PROGRESS;
        }
        if (hasReservedRoom(roomId)) {
            return Blocker.ROOM_ALREADY_RESERVED_BY_NEW_TENANT;
        }
        if (hasBlockingDeposit(roomId, currentContractId)) {
            return Blocker.ROOM_ALREADY_RESERVED_BY_NEW_TENANT;
        }
        if (hasApprovedTransfer(roomId)) {
            return Blocker.APPROVED_TRANSFER_EXISTS;
        }
        if (hasFutureContract(roomId, currentContractId)) {
            return Blocker.FUTURE_CONTRACT_EXISTS;
        }
        return Blocker.NONE;
    }

    public boolean canOldTenantRenew(Long roomId, Long currentContractId) {
        return checkRenewBlockers(roomId, currentContractId) == Blocker.NONE;
    }

    public Optional<LocalDate> findExpectedVacantDateForBooking(Long roomId) {
        return jdbcTemplate.query("""
                        SELECT expected_vacant_date
                        FROM lease_contracts
                        WHERE room_id = ?
                          AND deleted_at IS NULL
                          AND status IN ('ACTIVE', 'EXPIRING_SOON')
                          AND tenant_intention IN ('MOVE_OUT', 'TRANSFER')
                          AND expected_vacant_date IS NOT NULL
                        ORDER BY
                          CASE status WHEN 'EXPIRING_SOON' THEN 0 WHEN 'ACTIVE' THEN 1 ELSE 2 END,
                          end_date ASC,
                          id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? Optional.of(rs.getDate("expected_vacant_date").toLocalDate()) : Optional.empty(),
                roomId
        );
    }

    private boolean hasActiveHold(Long roomId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM room_holds
                        WHERE room_id = ?
                          AND status IN ('ACTIVE', 'PAYMENT_PROCESSING')
                          AND expires_at > ?
                        """,
                Integer.class,
                roomId,
                LocalDateTime.now()
        );
        return count != null && count > 0;
    }

    private boolean hasReservedRoom(Long roomId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM rooms
                        WHERE id = ?
                          AND current_status = 'RESERVED'
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                roomId
        );
        return count != null && count > 0;
    }

    private boolean hasBlockingDeposit(Long roomId, Long currentContractId) {
        Long currentDepositId = currentDepositId(currentContractId);
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM deposit_agreements da
                        WHERE da.room_id = ?
                          AND da.status IN ('PENDING_PAYMENT', 'PAID', 'CONFIRMED')
                          AND (? IS NULL OR da.id <> ?)
                        """,
                Integer.class,
                roomId,
                currentDepositId,
                currentDepositId
        );
        return count != null && count > 0;
    }

    private boolean hasApprovedTransfer(Long roomId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM room_transfer_requests
                        WHERE target_room_id = ?
                          AND status = 'APPROVED'
                        """,
                Integer.class,
                roomId
        );
        return count != null && count > 0;
    }

    private boolean hasFutureContract(Long roomId, Long currentContractId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts
                        WHERE room_id = ?
                          AND id <> ?
                          AND deleted_at IS NULL
                          AND status IN ('DRAFT', 'PENDING_SIGNATURE')
                        """,
                Integer.class,
                roomId,
                currentContractId
        );
        return count != null && count > 0;
    }

    private Long currentDepositId(Long currentContractId) {
        return jdbcTemplate.query("""
                        SELECT deposit_agreement_id
                        FROM lease_contracts
                        WHERE id = ?
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getObject("deposit_agreement_id", Long.class) : null,
                currentContractId
        );
    }
}
