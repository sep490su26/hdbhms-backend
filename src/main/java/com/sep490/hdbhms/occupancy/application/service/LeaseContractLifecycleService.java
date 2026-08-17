package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.out.ReleaseRoomPort;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.property.application.port.out.RoomRepository;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractLifecycleService {
    static final List<LeaseStatus> EXPIRY_CANDIDATE_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.TERMINATION_PENDING
    );

    LeaseContractRepository leaseContractRepository;
    RoomRepository roomRepository;
    JdbcTemplate jdbcTemplate;
    LeaseExpiryReminderService leaseExpiryReminderService;
    ReleaseRoomPort releaseRoomPort;

    @Transactional
    public void processAll(LocalDate today) {
        leaseContractRepository.findLifecycleCandidates(EXPIRY_CANDIDATE_STATUSES)
                .forEach(contract -> processContract(contract, today));
    }

    @Transactional
    public void processContract(Long contractId, LocalDate today) {
        leaseContractRepository.findLifecycleCandidateById(contractId, EXPIRY_CANDIDATE_STATUSES)
                .ifPresent(contract -> processContract(contract, today));
    }

    private void processContract(LeaseContract contract, LocalDate today) {
        if (contract.getStatus() == LeaseStatus.TERMINATION_PENDING) {
            resolveTerminationPendingRoom(contract, today);
            return;
        }
        if (contract.getEndDate() == null) {
            return;
        }
        boolean hasActivatedRenewal =
                leaseContractRepository.existsByPreviousContractIdAndStatus(
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
            leaseExpiryReminderService.processContract(contract, today, true);
            log.info(
                "Skipping lease expiry because an active renewal contract already exists. contractId={}, status={}",
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
                    "Thời hạn hợp đồng sắp hết hạn do còn hoặc dưới 3 tháng"
            );
        }
        resolveToReleaseRoom(contract, today);
        leaseExpiryReminderService.processContract(contract, today, hasActivatedRenewal);
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

    private void transitionToExpired(LeaseContract contract) {
        transitionContractStatus(contract, LeaseStatus.EXPIRED, "Ngày kết thúc hợp đồng đã qua");
        if (contract.getRoomId() == null) {
            return;
        }
        int changed = roomRepository.updateRoomStatusIfCurrent(contract.getRoomId(), RoomStatus.OCCUPIED, RoomStatus.EXPIRED);
        if (changed == 0) {
            return;
        }
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
                contract.getRoomId(),
                "Hợp đồng " + contract.getContractCode() + " đã hết hạn"
        );
    }

    private void transitionContractStatus(
            LeaseContract contract,
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
                "Lease lifecycle status changed. contractId={}, oldStatus={}, newStatus={}, reason={}",
                contract.getId(),
                oldStatus,
                newStatus,
                reason
        );
    }

    private void resolveToReleaseRoom(LeaseContract contract, LocalDate today) {
        boolean is1MonthLeft = !today.isBefore(contract.getEndDate().minusMonths(1).minus(1, ChronoUnit.DAYS));
        //TODO: Nếu còn 1 tháng cuối thì thực hiện chuyển phòng về sắp trống để tranh giữ phòng giữa guest và tenant
        if (is1MonthLeft) {
            releaseRoomPort.execute(contract.getRoomId());
        }
    }

    private void resolveTerminationPendingRoom(LeaseContract contract, LocalDate today) {
        LocalDate expectedVacantDate = contract.getExpectedVacantDate();
        if (contract.getRoomId() == null
                || expectedVacantDate == null
                || today.isBefore(expectedVacantDate)) {
            return;
        }
        releaseRoomPort.executeImmediately(contract.getRoomId());
    }
}
