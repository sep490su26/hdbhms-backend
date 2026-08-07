package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.ActivateLeaseContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ActivateLeaseContractService implements ActivateLeaseContractUseCase {
    JpaLeaseContractRepository leaseContractRepository;
    JpaRoomRepository roomRepository;
    JdbcTemplate jdbcTemplate;
    LeaseContractWorkflowSupport workflowSupport;
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;

    @Override
    public LeaseContractManagementResponse execute(Long leaseContractId) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease contract not found."));
        if (contract.getStatus() == LeaseStatus.ACTIVE) {
            return getLeaseContractManagementUseCase.findOne(leaseContractId);
        }
        workflowSupport.ensureNotRoomTransferManagedContract(leaseContractId);
        if (contract.getStatus() != LeaseStatus.DRAFT && contract.getStatus() != LeaseStatus.PENDING_SIGNATURE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending lease contracts can be activated.");
        }
        if (contract.getSignedFile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload the signed lease contract before activation.");
        }
        if (contract.getPrimaryTenantProfile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lease contract has no primary tenant.");
        }
        if (contract.getStartDate() == null || contract.getEndDate() == null || contract.getEndDate().isBefore(contract.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lease contract dates are invalid.");
        }
        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lease contract has no room.");
        }

        if (contract.getPreviousContract() == null && !hasCompletedMoveInHandover(leaseContractId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete move-in handover before activating the lease contract.");
        }

        boolean renewalActivation = contract.getPreviousContract() != null
                && (room.getCurrentStatus() == RoomStatus.OCCUPIED
                || room.getCurrentStatus() == RoomStatus.EXPIRED);
        if (!renewalActivation
                && room.getCurrentStatus() != RoomStatus.RESERVED
                && room.getCurrentStatus() != RoomStatus.VACANT
                && room.getCurrentStatus() != RoomStatus.ON_HOLD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room must be vacant or reserved before activation.");
        }
        Long previousContractId = contract.getPreviousContract() != null
                ? contract.getPreviousContract().getId()
                : null;
        if (workflowSupport.hasOtherActiveContract(room.getId(), contract.getId(), previousContractId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room already has an active contract.");
        }

        workflowSupport.ensureContractOccupants(contract);
        LeaseContractEntity previousContract = contract.getPreviousContract();
        if (previousContract != null && workflowSupport.isHolderReplacementLiquidation(previousContract.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Complete the liquidation flow before activating the replacement contract.");
        }
        if (previousContract != null) {
            workflowSupport.copyContractOccupants(previousContract, contract);
            boolean legacyPrematureRenewal = previousContract.getStatus() == LeaseStatus.RENEWED
                    && List.of(LeaseStatus.DRAFT, LeaseStatus.PENDING_SIGNATURE).contains(contract.getStatus());
            if (!legacyPrematureRenewal
                    && !List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.EXPIRED)
                    .contains(previousContract.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Previous contract cannot be renewed from its current state.");
            }
            previousContract.setStatus(LeaseStatus.RENEWED);
            leaseContractRepository.saveAndFlush(previousContract);
        }
        contract.setStatus(LeaseStatus.ACTIVE);
        contract.setSignedAt(LocalDateTime.now());
        if (contract.getRentStartDate() == null) {
            contract.setRentStartDate(workflowSupport.resolveRentStartDate(contract.getStartDate()));
        }
        leaseContractRepository.save(contract);

        RoomStatus fromStatus = room.getCurrentStatus();
        room.setCurrentStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);
        workflowSupport.appendRoomStatusHistory(room.getId(), fromStatus, RoomStatus.OCCUPIED, "Lease contract activated: " + contract.getContractCode());
        workflowSupport.appendContractEvent(contract.getId(), "SIGNED", "Lease contract activated");
        if (previousContract != null) {
            workflowSupport.appendContractEvent(previousContract.getId(), "RENEWED", "Lease contract renewed; newContractId=" + contract.getId());
        }
        return getLeaseContractManagementUseCase.findOne(contract.getId());
    }

    private boolean hasCompletedMoveInHandover(Long leaseContractId) {
        Integer handoverCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM contract_handover_records
                        WHERE contract_id = ?
                          AND handover_type = 'MOVE_IN'
                          AND electricity_reading_id IS NOT NULL
                          AND water_reading_id IS NOT NULL
                          AND signed_document_id IS NOT NULL
                        """,
                Integer.class,
                leaseContractId
        );
        return handoverCount != null && handoverCount > 0;
    }
}
