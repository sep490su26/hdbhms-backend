package com.sep490.hdbhms.property.application.service;

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
        ROOM_ALREADY_RESERVED_BY_NEW_TENANT,
        APPROVED_TRANSFER_EXISTS,
        FUTURE_CONTRACT_EXISTS
    }

    public Blocker checkRenewBlockers(
            Long roomId,
            Long currentContractId,
            LocalDate contractEndDate
    ) {
        // A future tenant can only displace a renewal once the current lease is
        // inside its last 3 calendar months.
        if (hasLessThanThreeMonthRemaining(contractEndDate)
                && isReserved(roomId, currentContractId)) {
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

    /**
     * Returns true for the expiring tenant's room when another tenant has
     * already reserved it. This is the shared gate for move-out workflows.
     */
    public boolean isSoonVacantBookingCase(
            Long roomId,
            Long currentContractId,
            LocalDate contractEndDate
    ) {
        return isSoonVacant(roomId)
                && hasLessThanThreeMonthRemaining(contractEndDate)
                && isReserved(roomId, currentContractId);
    }

    /**
     * Returns true for a booking-backed incoming contract waiting for a
     * move-in handover while the previous lease is still expiring.
     */
    public boolean requiresVacantRoomForIncomingBooking(Long roomId, Long incomingContractId) {
        return !isVacant(roomId)
                && hasExpiringContract(roomId, incomingContractId)
                && hasBookingBackedContract(incomingContractId, roomId);
    }

    public Optional<LocalDate> findExpectedVacantDateForBooking(Long roomId) {
        return jdbcTemplate.query("""
                        SELECT COALESCE(expected_vacant_date, end_date) AS expected_vacant_date
                        FROM lease_contracts
                        WHERE room_id = ?
                          AND deleted_at IS NULL
                          AND status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
                        ORDER BY
                          CASE status WHEN 'EXPIRING_SOON' THEN 0 WHEN 'ACTIVE' THEN 1 ELSE 2 END,
                          end_date ,
                          lease_contract_id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? Optional.of(rs.getDate("expected_vacant_date").toLocalDate()) : Optional.empty(),
                roomId
        );
    }

    private boolean isReserved(Long roomId, Long currentContractId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM rooms
                        WHERE room_id = ?
                          AND deleted_at IS NULL
                          AND (
                              current_status = 'RESERVED'
                              OR EXISTS (
                                  SELECT 1
                                  FROM room_holds hold
                                  WHERE hold.room_id = rooms.room_id
                                    AND (
                                        (
                                            hold.status IN ('ACTIVE', 'PAYMENT_PROCESSING')
                                            AND hold.expires_at > NOW(6)
                                        )
                                        OR hold.status = 'CONFIRMED'
                                    )
                              )
                              OR EXISTS (
                                  SELECT 1
                                  FROM deposit_forms deposit
                                  WHERE deposit.room_id = rooms.room_id
                                    AND deposit.deposit_status IN ('PAID', 'CONFIRMED', 'EXTENDED')
                                    AND (
                                        deposit.depositor_person_profile_id IS NULL
                                        OR NOT EXISTS (
                                            SELECT 1
                                            FROM lease_contracts current_contract
                                            WHERE current_contract.lease_contract_id = ?
                                              AND current_contract.primary_tenant_profile_id = deposit.depositor_person_profile_id
                                        )
                                    )
                              )
                          )
                        """,
                Integer.class,
                roomId,
                currentContractId
        );
        return count != null && count > 0;
    }

    private boolean hasLessThanThreeMonthRemaining(LocalDate contractEndDate) {
        return contractEndDate != null
                && contractEndDate.isBefore(LocalDate.now().plusMonths(3));
    }

    private boolean isSoonVacant(Long roomId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM rooms
                        WHERE room_id = ?
                          AND deleted_at IS NULL
                          AND current_status = 'SOON_VACANT'
                        """,
                Integer.class,
                roomId
        );
        return count != null && count > 0;
    }

    private boolean isVacant(Long roomId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM rooms
                        WHERE room_id = ?
                          AND deleted_at IS NULL
                          AND current_status = 'VACANT'
                        """,
                Integer.class,
                roomId
        );
        return count != null && count > 0;
    }

    private boolean hasExpiringContract(Long roomId, Long excludedContractId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts
                        WHERE room_id = ?
                          AND lease_contract_id <> ?
                          AND deleted_at IS NULL
                          AND status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
                          AND end_date < DATE_ADD(CURRENT_DATE, INTERVAL 1 MONTH)
                        """,
                Integer.class,
                roomId,
                excludedContractId
        );
        return count != null && count > 0;
    }

    private boolean hasBookingBackedContract(Long contractId, Long roomId) {
        if (contractId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts contract
                        JOIN deposit_forms deposit
                          ON deposit.deposit_form_id = contract.deposit_form_id
                        WHERE contract.lease_contract_id = ?
                          AND contract.room_id = ?
                          AND contract.deleted_at IS NULL
                          AND deposit.deposit_status IN ('PAID', 'CONFIRMED', 'EXTENDED')
                        """,
                Integer.class,
                contractId,
                roomId
        );
        return count != null && count > 0;
    }

    private boolean hasApprovedTransfer(Long roomId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM room_transfer_requests
                        WHERE target_room_id = ?
                          AND status IN (
                              'MANAGER_APPROVED',
                              'WAITING_HOLDER_RESPONSE',
                              'WAITING_TARGET_HOLDER_APPROVAL',
                              'WAITING_NEW_CONTRACT',
                              'WAITING_CONTRACT_CONFIRMATION',
                              'WAITING_SIGNING',
                              'WAITING_CONTRACT_SIGNING',
                              'WAITING_PAYMENT',
                              'WAITING_TRANSFER_DATE',
                              'READY_FOR_HANDOVER',
                              'WAITING_EXECUTION'
                          )
                          AND COALESCE(reserved_slots, 0) > 0
                          AND (reservation_expires_at IS NULL OR reservation_expires_at >= ?)
                        """,
                Integer.class,
                roomId,
                LocalDateTime.now()
        );
        Integer currentTransferReservationCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM rooms
                        WHERE room_id = ?
                          AND current_status = 'RESERVED_FOR_TRANSFER'
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                roomId
        );
        return (count != null && count > 0)
                || (currentTransferReservationCount != null && currentTransferReservationCount > 0);
    }

    private boolean hasFutureContract(Long roomId, Long currentContractId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts
                        WHERE room_id = ?
                          AND lease_contract_id <> ?
                          AND deleted_at IS NULL
                          AND status IN ('DRAFT', 'PENDING_SIGNATURE')
                        """,
                Integer.class,
                roomId,
                currentContractId
        );
        return count != null && count > 0;
    }

}
