package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.sep490.hdbhms.shared.utils.AuthUtils;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractWorkflowSupport {
    static final List<LeaseStatus> BLOCKING_ACTIVE_CONTRACT_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.TERMINATION_PENDING
    );

    JdbcTemplate jdbcTemplate;
    JpaLeaseContractRepository leaseContractRepository;

    void validateContractTerms(
            LocalDate startDate,
            Integer paymentCycleMonths,
            Long monthlyRent,
            Long depositAmount
    ) {
        if (startDate == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (!Objects.equals(paymentCycleMonths, 1) && !Objects.equals(paymentCycleMonths, 3)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (monthlyRent == null || monthlyRent <= 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (depositAmount == null || depositAmount < 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    LocalDate resolveRentStartDate(LocalDate startDate) {
        return startDate.getDayOfMonth() <= 10 ? startDate : startDate.plusMonths(1).withDayOfMonth(1);
    }

    void ensureContractOccupants(LeaseContractEntity contract) {
        if (contract == null || contract.getPrimaryTenantProfile() == null || contract.getRoom() == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        Long propertyId = contract.getRoom().getProperty() == null ? null : contract.getRoom().getProperty().getId();
        Long tenantId = resolveTenantIdForProfile(contract.getPrimaryTenantProfile().getId(), propertyId);
        insertContractOccupantIfAbsent(
                contract.getId(),
                tenantId,
                contract.getPrimaryTenantProfile().getId(),
                "PRIMARY",
                contract.getStartDate()
        );
    }

    void copyContractOccupants(LeaseContractEntity oldContract, LeaseContractEntity newContract) {
        jdbcTemplate.update("""
                        INSERT INTO contract_occupants (
                            contract_id, tenant_id, tenant_profile_id, occupant_role,
                            move_in_date, move_out_date, status, created_at
                        )
                        SELECT ?, tenant_id, tenant_profile_id, occupant_role,
                               ?, NULL, 'ACTIVE', NOW(6)
                        FROM contract_occupants source_occupant
                        WHERE source_occupant.contract_id = ?
                          AND source_occupant.status = 'ACTIVE'
                          AND NOT EXISTS (
                              SELECT 1 FROM contract_occupants existing_occupant
                              WHERE existing_occupant.contract_id = ?
                                AND existing_occupant.tenant_profile_id <=> source_occupant.tenant_profile_id
                          )
                        """,
                newContract.getId(),
                newContract.getStartDate(),
                oldContract.getId(),
                newContract.getId()
        );
        ensureContractOccupants(newContract);
    }

    boolean hasOtherActiveContract(Long roomId, Long contractId, Long previousContractId) {
        String statusPlaceholders = String.join(",", BLOCKING_ACTIVE_CONTRACT_STATUSES.stream().map(status -> "?").toList());
        List<Object> args = new java.util.ArrayList<>();
        args.add(roomId);
        args.add(contractId);
        args.add(previousContractId);
        args.add(previousContractId);
        args.addAll(BLOCKING_ACTIVE_CONTRACT_STATUSES.stream().map(Enum::name).toList());
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lease_contracts WHERE room_id = ? AND lease_contract_id <> ? "
                        + "AND (? IS NULL OR lease_contract_id <> ?) AND deleted_at IS NULL "
                        + "AND status IN (" + statusPlaceholders + ")",
                Integer.class,
                args.toArray()
        );
        return count != null && count > 0;
    }

    void ensureNotRoomTransferManagedContract(Long leaseContractId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM room_transfer_requests
                        WHERE (new_contract_id = ? OR replacement_old_contract_id = ?)
                          AND status NOT IN ('CANCELLED', 'REJECTED', 'EXPIRED', 'COMPLETED')
                        """, Integer.class, leaseContractId, leaseContractId);
        if (count != null && count > 0) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }
    }

    boolean isHolderReplacementLiquidation(Long contractId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM contract_liquidations
                        WHERE contract_id = ? AND reason LIKE '%PRIMARY_LEAVES_CO_OCCUPANT_STAYS%'
                          AND status NOT IN ('COMPLETED', 'CANCELLED')
                        """, Integer.class, contractId);
        return count != null && count > 0;
    }

    void appendContractEvent(Long contractId, String eventType, String description) {
        jdbcTemplate.update("""
                        INSERT INTO contract_events (
                            contract_id,
                            event_type,
                            event_data,
                            created_by,
                            created_at
                        )
                        VALUES (?, ?, ?, ?, NOW(6))
                        """,
                contractId,
                eventType,
                description != null ? description.getBytes(StandardCharsets.UTF_8) : null,
                AuthUtils.getCurrentAuthenticationId()
        );
    }

    void appendRoomStatusHistory(Long roomId, RoomStatus fromStatus, RoomStatus toStatus, String reason) {
        jdbcTemplate.update("""
                        INSERT INTO room_status_history (room_id, from_status, to_status, reason, changed_at)
                        VALUES (?, ?, ?, ?, NOW(6))
                        """, roomId, fromStatus == null ? null : fromStatus.name(),
                toStatus == null ? null : toStatus.name(), reason);
    }

    private void insertContractOccupantIfAbsent(
            Long contractId,
            Long tenantId,
            Long tenantProfileId,
            String occupantRole,
            LocalDate moveInDate
    ) {
        Integer exists = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM contract_occupants
                        WHERE contract_id = ? AND tenant_profile_id = ?
                        """, Integer.class, contractId, tenantProfileId);
        if (exists != null && exists > 0) {
            return;
        }
        jdbcTemplate.update("""
                        INSERT INTO contract_occupants (
                            contract_id, tenant_id, tenant_profile_id, occupant_role,
                            move_in_date, status, created_at
                        ) VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW(6))
                        """, contractId, tenantId, tenantProfileId, occupantRole, moveInDate);
    }

    private Long resolveTenantIdForProfile(Long profileId, Long propertyId) {
        if (profileId == null || propertyId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                        SELECT t.tenant_id AS id
                        FROM person_profiles pp
                        JOIN tenants t ON t.user_id = pp.user_id
                        WHERE pp.person_profile_id = ? AND pp.deleted_at IS NULL
                          AND t.property_id = ? AND t.deleted_at IS NULL
                        ORDER BY t.tenant_id DESC LIMIT 1
                        """, rs -> rs.next() ? rs.getLong("id") : null, profileId, propertyId);
    }
}
