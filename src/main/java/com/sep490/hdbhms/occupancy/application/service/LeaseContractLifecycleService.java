package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.domain.valueObjects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractLifecycleService {
    static final List<LeaseStatus> EXPIRY_CANDIDATE_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON
    );

    JpaLeaseContractRepository leaseContractRepository;
    JpaRoomRepository roomRepository;
    JdbcTemplate jdbcTemplate;

    @Transactional
    public void processAll(LocalDate today) {
        leaseContractRepository.findAllByStatusInAndDeletedAtIsNull(EXPIRY_CANDIDATE_STATUSES)
                .forEach(contract -> processContract(contract, today));
    }

    @Transactional
    public void processContract(Long contractId, LocalDate today) {
        leaseContractRepository.findById(contractId)
                .filter(contract -> contract.getDeletedAt() == null)
                .filter(contract -> EXPIRY_CANDIDATE_STATUSES.contains(contract.getStatus()))
                .ifPresent(contract -> processContract(contract, today));
    }

    private void processContract(LeaseContractEntity contract, LocalDate today) {
        if (contract.getEndDate() == null) {
            return;
        }
        boolean hasActivatedRenewal =
                leaseContractRepository.existsByPreviousContract_IdAndStatusAndDeletedAtIsNull(
                        contract.getId(),
                        LeaseStatus.ACTIVE
                );
        LeaseStatus targetStatus = resolveTargetStatus(
                contract.getStatus(),
                contract.getEndDate(),
                today,
                hasActivatedRenewal
        );
        if (today.isAfter(contract.getEndDate()) && hasActivatedRenewal) {
                log.info(
                        "Skip contract expiry because active renewed contract exists. contractId={}, status={}",
                        contract.getId(),
                        contract.getStatus()
                );
            return;
        }
        if (targetStatus == LeaseStatus.EXPIRED) {
            transitionToExpired(contract);
            return;
        }
        if (targetStatus == LeaseStatus.EXPIRING_SOON) {
            transitionContractStatus(
                    contract,
                    LeaseStatus.EXPIRING_SOON,
                    "Contract has three months or less remaining"
            );
        }
    }

    static LeaseStatus resolveTargetStatus(
            LeaseStatus currentStatus,
            LocalDate endDate,
            LocalDate today,
            boolean hasRenewedContract
    ) {
        if (!EXPIRY_CANDIDATE_STATUSES.contains(currentStatus) || endDate == null) {
            return currentStatus;
        }
        if (today.isAfter(endDate)) {
            return hasRenewedContract ? currentStatus : LeaseStatus.EXPIRED;
        }
        if (currentStatus == LeaseStatus.ACTIVE && !today.isBefore(endDate.minusMonths(3))) {
            return LeaseStatus.EXPIRING_SOON;
        }
        return currentStatus;
    }

    private void transitionToExpired(LeaseContractEntity contract) {
        transitionContractStatus(contract, LeaseStatus.EXPIRED, "Contract end date has passed");
        RoomEntity room = contract.getRoom();
        if (room == null || room.getCurrentStatus() != RoomStatus.OCCUPIED) {
            return;
        }
        room.setCurrentStatus(RoomStatus.EXPIRED);
        roomRepository.save(room);
        jdbcTemplate.update("""
                        INSERT INTO room_status_history (
                            room_id,
                            from_status,
                            to_status,
                            reason,
                            changed_by,
                            changed_at
                        )
                        VALUES (?, 'OCCUPIED', 'EXPIRED', ?, NULL, NOW(6))
                        """,
                room.getId(),
                "Hop dong " + contract.getContractCode() + " da het han"
        );
    }

    private void transitionContractStatus(
            LeaseContractEntity contract,
            LeaseStatus newStatus,
            String reason
    ) {
        LeaseStatus oldStatus = contract.getStatus();
        if (oldStatus == newStatus) {
            return;
        }
        contract.setStatus(newStatus);
        leaseContractRepository.save(contract);
        jdbcTemplate.update("""
                        INSERT INTO contract_events (
                            contract_id,
                            event_type,
                            event_data,
                            created_by,
                            created_at
                        )
                        VALUES (?, ?, ?, NULL, NOW(6))
                        """,
                contract.getId(),
                newStatus == LeaseStatus.EXPIRED ? "EXPIRED" : "NOTICE_SENT",
                reason.getBytes(StandardCharsets.UTF_8)
        );
        log.info(
                "Contract lifecycle status changed. contractId={}, oldStatus={}, newStatus={}, reason={}",
                contract.getId(),
                oldStatus,
                newStatus,
                reason
        );
    }
}
