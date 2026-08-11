package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;

import com.sep490.hdbhms.occupancy.application.port.in.command.RenewLeaseContractCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RenewLeaseContractUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantRole;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractRenewalResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RenewLeaseContractService implements RenewLeaseContractUseCase {
    JpaLeaseContractRepository leaseContractRepository;
    JpaRoomRepository roomRepository;
    JdbcTemplate jdbcTemplate;
    RoomCommitmentChecker roomCommitmentChecker;
    LeaseContractWorkflowSupport workflowSupport;

    @Override
    public LeaseContractRenewalResponse execute(RenewLeaseContractCommand command) {
        assertOwnerOrManagerCanRenew();
        LeaseContractEntity oldContract = leaseContractRepository.findById(command.leaseContractId())
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        if (oldContract.getDeletedAt() != null) {
            throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.EXPIRED)
                .contains(oldContract.getStatus())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (leaseContractRepository.existsByPreviousContract_IdAndDeletedAtIsNull(oldContract.getId())) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }

        if (command.newStartDate() == null
                || command.newEndDate() == null
                || !command.newEndDate().isAfter(command.newStartDate())) {
            throw new AppException(ApiErrorCode.LEASE_RENEWAL_DATES_INVALID);
        }

        workflowSupport.validateContractTerms(
                command.newStartDate(),
                command.paymentCycleMonths(),
                command.monthlyRent(),
                command.depositAmount()
        );
        RoomEntity room = oldContract.getRoom();
        if (room == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (workflowSupport.hasOtherActiveContract(room.getId(), oldContract.getId(), null)) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }

        RoomStatus previousRoomStatus = room.getCurrentStatus();
        if (previousRoomStatus == RoomStatus.SOON_VACANT) {
            RoomCommitmentChecker.Blocker blocker =
                    roomCommitmentChecker.checkRenewBlockers(room.getId(), oldContract.getId());
            if (blocker != RoomCommitmentChecker.Blocker.NONE) {
                throwRenewBlocked(blocker);
            }
            oldContract.setTenantIntention("RENEW");
            oldContract.setExpectedVacantDate(null);
            oldContract.setIntentionRecordedAt(java.time.LocalDateTime.now());
            leaseContractRepository.saveAndFlush(oldContract);
            room.setCurrentStatus(RoomStatus.OCCUPIED);
            roomRepository.saveAndFlush(room);
            workflowSupport.appendRoomStatusHistory(
                    room.getId(),
                    previousRoomStatus,
                    RoomStatus.OCCUPIED,
                    "Khách cũ đổi ý tái ký hợp đồng " + oldContract.getContractCode()
            );
            workflowSupport.appendContractEvent(
                    oldContract.getId(),
                    "RENEWAL_AFTER_MOVE_OUT_INTENT",
                    "Owner xác nhận tái ký sau khi khách đã báo chuyển đi"
            );
        }

        String newContractCode = resolveRenewalContractCode(oldContract, command.newContractCode());
        LeaseContractEntity newContract = LeaseContractEntity.builder()
                .contractCode(newContractCode)
                .room(room)
                .primaryTenantProfile(oldContract.getPrimaryTenantProfile())
                .startDate(command.newStartDate())
                .endDate(command.newEndDate())
                .rentStartDate(workflowSupport.resolveRentStartDate(command.newStartDate()))
                .monthlyRent(command.monthlyRent())
                .paymentCycleMonths(command.paymentCycleMonths())
                .depositAmount(command.depositAmount())
                .status(LeaseStatus.PENDING_SIGNATURE)
                .previousContract(oldContract)
                .build();
        newContract = leaseContractRepository.save(newContract);
        workflowSupport.copyContractOccupants(oldContract, newContract);

        RoomStatus currentRoomStatus = room.getCurrentStatus();
        if (currentRoomStatus != RoomStatus.OCCUPIED) {
            room.setCurrentStatus(RoomStatus.OCCUPIED);
            roomRepository.save(room);
            workflowSupport.appendRoomStatusHistory(
                    room.getId(),
                    currentRoomStatus,
                    RoomStatus.OCCUPIED,
                    "Tạo hợp đồng tái ký " + newContractCode
            );
        }

        String eventNote = command.note() == null || command.note().isBlank() ? "Tạo hợp đồng tái ký" : command.note().trim();
        workflowSupport.appendContractEvent(
                newContract.getId(),
                "CREATED",
                "Tái ký từ hợp đồng " + oldContract.getContractCode() + "; note=" + eventNote
        );

        List<LeaseContractRenewalResponse.OccupantInfo> occupants = findRenewalOccupants(newContract.getId());
        return new LeaseContractRenewalResponse(
                oldContract.getId(),
                oldContract.getContractCode(),
                oldContract.getStatus(),
                newContract.getId(),
                newContract.getContractCode(),
                newContract.getStatus(),
                oldContract.getId(),
                room.getId(),
                room.getRoomCode(),
                occupants.size(),
                occupants
        );
    }

    private List<LeaseContractRenewalResponse.OccupantInfo> findRenewalOccupants(Long contractId) {
        return jdbcTemplate.query("""
                        SELECT
                            co.tenant_profile_id,
                            pp.full_name,
                            pp.phone,
                            co.occupant_role
                        FROM contract_occupants co
                        LEFT JOIN person_profiles pp ON pp.person_profile_id = co.tenant_profile_id
                        WHERE co.contract_id = ?
                          AND co.status = 'ACTIVE'
                        ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.contract_occupant_id
                        """,
                (rs, rowNum) -> new LeaseContractRenewalResponse.OccupantInfo(
                        getLongOrNull(rs, "tenant_profile_id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        OccupantRole.valueOf(rs.getString("occupant_role"))
                ),
                contractId
        );
    }

    private String resolveRenewalContractCode(LeaseContractEntity oldContract, String requestedContractCode) {
        String contractCode = requestedContractCode == null ? "" : requestedContractCode.trim();
        if (contractCode.isBlank()) {
            contractCode = generateRenewalContractCode(oldContract);
        }
        if (contractCode.length() > 80) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (leaseContractRepository.existsByContractCodeAndDeletedAtIsNull(contractCode)) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }
        return contractCode;
    }

    private String generateRenewalContractCode(LeaseContractEntity oldContract) {
        LeaseContractEntity rootContract = oldContract;
        int renewalNumber = 1;
        while (rootContract.getPreviousContract() != null) {
            rootContract = rootContract.getPreviousContract();
            renewalNumber++;
        }

        return rootContract.getContractCode() + "-R" + renewalNumber;
    }

    private void assertOwnerOrManagerCanRenew() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean canRenew = authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_OWNER".equals(authority.getAuthority())
                        || "ROLE_MANAGER".equals(authority.getAuthority()));
        if (!canRenew) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }
    }

    private void throwRenewBlocked(RoomCommitmentChecker.Blocker blocker) {
        if (blocker == RoomCommitmentChecker.Blocker.ROOM_ALREADY_RESERVED_BY_NEW_TENANT) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }
        throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
    }

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
