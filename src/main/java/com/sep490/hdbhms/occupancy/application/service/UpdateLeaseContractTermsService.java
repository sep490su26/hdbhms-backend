package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.sep490.hdbhms.occupancy.application.port.in.command.UpdateLeaseContractTermsCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseContractTermsUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateLeaseContractTermsService implements UpdateLeaseContractTermsUseCase {
    JpaLeaseContractRepository leaseContractRepository;
    JpaRoomRepository roomRepository;
    LeaseContractWorkflowSupport workflowSupport;
    RoomCommitmentChecker roomCommitmentChecker;
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;
    JdbcTemplate jdbcTemplate;

    @Override
    public LeaseContractManagementResponse execute(UpdateLeaseContractTermsCommand command) {
        LeaseContractEntity contract = leaseContractRepository.findById(command.leaseContractId())
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        if (contract.getDeletedAt() != null) {
            throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
        }
        if (List.of(
                LeaseStatus.LIQUIDATED,
                LeaseStatus.AUTO_TERMINATED,
                LeaseStatus.CANCELLED
        ).contains(contract.getStatus())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        LeaseContractEntity debtContract = contract.getPreviousContract() == null
                ? contract
                : contract.getPreviousContract();
        LeaseContractDebtPolicy.requireNoOutstandingDebt(jdbcTemplate, debtContract.getId());

        workflowSupport.validateContractTerms(
                command.startDate(),
                command.paymentCycleMonths(),
                command.monthlyRent(),
                command.depositAmount()
        );
        if (command.endDate() == null || !command.endDate().isAfter(command.startDate())) {
            throw new AppException(ApiErrorCode.LEASE_RENEWAL_DATES_INVALID);
        }
        RoomEntity room = contract.getRoom();
        LocalDate currentEndDate = contract.getEndDate();
        boolean extendsEndDate = currentEndDate == null || command.endDate().isAfter(currentEndDate);
        if (extendsEndDate
                && room != null
                && List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.EXPIRED)
                .contains(contract.getStatus())) {
            if (workflowSupport.hasOtherActiveContract(room.getId(), contract.getId(), null)) {
                throw new AppException(ApiErrorCode.LEASE_RENEWAL_ROOM_RESERVED_BY_OTHER_TENANT);
            }

            RoomCommitmentChecker.Blocker blocker = roomCommitmentChecker.checkRenewBlockers(
                    room.getId(),
                    contract.getId(),
                    currentEndDate
            );
            if (blocker != RoomCommitmentChecker.Blocker.NONE) {
                throwRenewBlocked(blocker);
            }

            if (room.getCurrentStatus() == RoomStatus.SOON_VACANT) {
                RoomStatus fromStatus = room.getCurrentStatus();
                contract.setTenantIntention("RENEW");
                contract.setExpectedVacantDate(null);
                contract.setIntentionRecordedAt(java.time.LocalDateTime.now());
                room.setCurrentStatus(RoomStatus.OCCUPIED);
                roomRepository.saveAndFlush(room);
                workflowSupport.appendRoomStatusHistory(
                        room.getId(),
                        fromStatus,
                        RoomStatus.OCCUPIED,
                        "Gia hạn hợp đồng thuê " + contract.getContractCode()
                );
                workflowSupport.appendContractEvent(
                        contract.getId(),
                        "RENEWAL_AFTER_MOVE_OUT_INTENT",
                        "Gia hạn hợp đồng sau khi khách đã báo chuyển đi"
                );
            }
        }
        boolean rentChanged = !Objects.equals(contract.getMonthlyRent(), command.monthlyRent());

        contract.setStartDate(command.startDate());
        contract.setEndDate(command.endDate());
        contract.setRentStartDate(workflowSupport.resolveRentStartDate(command.startDate()));
        contract.setPaymentCycleMonths(command.paymentCycleMonths());
        contract.setMonthlyRent(command.monthlyRent());
        contract.setDepositAmount(command.depositAmount());
        applyLifecycleStatusAfterTermsUpdate(contract, LocalDate.now());
        leaseContractRepository.save(contract);

        if (rentChanged) {
            workflowSupport.appendContractEvent(
                    contract.getId(),
                    "PRICE_CHANGED",
                    "Cập nhật giá thuê hằng tháng thành " + command.monthlyRent()
            );
        }
        return getLeaseContractManagementUseCase.findOne(contract.getId());
    }

    private void throwRenewBlocked(RoomCommitmentChecker.Blocker blocker) {
        if (blocker == RoomCommitmentChecker.Blocker.ROOM_ALREADY_RESERVED_BY_NEW_TENANT) {
            throw new AppException(ApiErrorCode.LEASE_ROOM_PREBOOKED_BY_OTHER_TENANT);
        }
        throw new AppException(ApiErrorCode.LEASE_RENEWAL_ROOM_RESERVED_BY_OTHER_TENANT);
    }

    private void applyLifecycleStatusAfterTermsUpdate(LeaseContractEntity contract, LocalDate today) {
        if (!List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.EXPIRED).contains(contract.getStatus())
                || contract.getEndDate() == null) {
            return;
        }

        LeaseStatus oldStatus = contract.getStatus();
        LeaseStatus newStatus;
        if (today.isAfter(contract.getEndDate())) {
            newStatus = LeaseStatus.EXPIRED;
        } else if (!today.isBefore(contract.getEndDate().minusMonths(3))) {
            newStatus = LeaseStatus.EXPIRING_SOON;
        } else {
            newStatus = LeaseStatus.ACTIVE;
        }

        if (newStatus == oldStatus) {
            return;
        }

        contract.setStatus(newStatus);
        if ((newStatus == LeaseStatus.ACTIVE || newStatus == LeaseStatus.EXPIRING_SOON)
                && oldStatus == LeaseStatus.EXPIRED) {
            RoomEntity room = contract.getRoom();
            if (room != null && room.getCurrentStatus() == RoomStatus.EXPIRED) {
                RoomStatus fromStatus = room.getCurrentStatus();
                room.setCurrentStatus(RoomStatus.OCCUPIED);
                roomRepository.save(room);
                workflowSupport.appendRoomStatusHistory(
                        room.getId(),
                        fromStatus,
                        RoomStatus.OCCUPIED,
                        "Gia hạn hợp đồng thuê " + contract.getContractCode()
                );
            }
        }
        if (newStatus == LeaseStatus.EXPIRING_SOON) {
            workflowSupport.appendContractEvent(
                    contract.getId(),
                    "NOTICE_SENT",
                    "Cập nhật thời hạn hợp đồng còn dưới hoặc bằng 3 tháng"
            );
            return;
        }
        if (newStatus == LeaseStatus.EXPIRED) {
            workflowSupport.appendContractEvent(
                    contract.getId(),
                    "EXPIRED",
                    "Cập nhật thời hạn hợp đồng đã qua ngày kết thúc"
            );
            RoomEntity room = contract.getRoom();
            if (room != null && room.getCurrentStatus() == RoomStatus.OCCUPIED) {
                RoomStatus fromStatus = room.getCurrentStatus();
                room.setCurrentStatus(RoomStatus.EXPIRED);
                roomRepository.save(room);
                workflowSupport.appendRoomStatusHistory(
                        room.getId(),
                        fromStatus,
                        RoomStatus.EXPIRED,
                        "Hợp đồng " + contract.getContractCode() + " đã hết hạn"
                );
            }
        }
    }
}
