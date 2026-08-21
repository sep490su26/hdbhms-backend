package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.sep490.hdbhms.occupancy.application.port.in.command.UpdateLeaseContractTermsCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseContractTermsUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.ContractEventType;
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
        Long effectiveDepositAmount = command.depositAmount() == null
                ? contract.getDepositAmount()
                : command.depositAmount();
        boolean startDateChanged = !Objects.equals(contract.getStartDate(), command.startDate());
        boolean endDateChanged = !Objects.equals(contract.getEndDate(), command.endDate());
        boolean paymentCycleChanged = !Objects.equals(contract.getPaymentCycleMonths(), command.paymentCycleMonths());
        boolean rentChanged = !Objects.equals(contract.getMonthlyRent(), command.monthlyRent());
        boolean preSigning = List.of(LeaseStatus.DRAFT, LeaseStatus.PENDING_SIGNATURE).contains(contract.getStatus())
                && contract.getSignedFile() == null
                && contract.getSignedAt() == null;

        if ((startDateChanged || endDateChanged)
                && !preSigning
                && !command.allowPostSigningDateChange()) {
            throw new AppException(ApiErrorCode.LEASE_CONTRACT_DATES_UPDATE_NOT_ALLOWED);
        }
        if (paymentCycleChanged) {
            ensurePaymentCycleCanChange(contract, command.allowPostSigningFinancialChange());
        }
        if ((paymentCycleChanged || rentChanged)
                && !preSigning
                && !command.allowPostSigningFinancialChange()) {
            throw new AppException(ApiErrorCode.LEASE_SIGNED_TERMS_UPDATE_NOT_ALLOWED);
        }
        if ((paymentCycleChanged || rentChanged)
                && !preSigning
                && !isPostSigningFinancialStatus(contract, command.allowPostSigningDateChange())) {
            throw new AppException(ApiErrorCode.LEASE_SIGNED_TERMS_UPDATE_NOT_ALLOWED);
        }
        if (paymentCycleChanged || startDateChanged || endDateChanged) {
            LocalDate paymentCycleTermStart = preSigning || command.allowPostSigningDateChange()
                    ? command.startDate()
                    : resolveRemainingTermStart(contract);
            workflowSupport.validatePaymentCycleMatchesTerm(
                    paymentCycleTermStart,
                    command.endDate(),
                    command.paymentCycleMonths()
            );
        }

        workflowSupport.validateContractTerms(
                command.startDate(),
                command.paymentCycleMonths(),
                command.monthlyRent(),
                effectiveDepositAmount
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
        contract.setStartDate(command.startDate());
        contract.setEndDate(command.endDate());
        contract.setRentStartDate(workflowSupport.resolveRentStartDate(command.startDate()));
        contract.setPaymentCycleMonths(command.paymentCycleMonths());
        contract.setMonthlyRent(command.monthlyRent());
        contract.setDepositAmount(effectiveDepositAmount);
        applyLifecycleStatusAfterTermsUpdate(contract, LocalDate.now());
        leaseContractRepository.save(contract);

        if (rentChanged) {
            workflowSupport.appendContractEvent(
                    contract.getId(),
                    "PRICE_CHANGED",
                    "Cập nhật giá thuê hằng tháng thành " + command.monthlyRent()
            );
        }
        if (paymentCycleChanged) {
            workflowSupport.appendContractEvent(
                    contract.getId(),
                    ContractEventType.PAYMENT_CYCLE_CHANGED.name(),
                    "Cập nhật chu kỳ đóng tiền thành " + command.paymentCycleMonths() + " tháng/lần"
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

    private void ensurePaymentCycleCanChange(LeaseContractEntity contract, boolean postSigningFinancialChange) {
        if (!postSigningFinancialChange
                && !List.of(LeaseStatus.DRAFT, LeaseStatus.PENDING_SIGNATURE).contains(contract.getStatus())) {
            throw new AppException(ApiErrorCode.LEASE_PAYMENT_CYCLE_UPDATE_NOT_ALLOWED);
        }
        if (!postSigningFinancialChange && (contract.getSignedFile() != null || contract.getSignedAt() != null)) {
            throw new AppException(ApiErrorCode.LEASE_PAYMENT_CYCLE_UPDATE_NOT_ALLOWED);
        }

        if (postSigningFinancialChange) {
            return;
        }

        Integer invoiceCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM invoices
                        WHERE lease_contract_id = ?
                          AND status <> 'VOIDED'
                        """,
                Integer.class,
                contract.getId()
        );
        if (invoiceCount != null && invoiceCount > 0) {
            throw new AppException(ApiErrorCode.LEASE_PAYMENT_CYCLE_UPDATE_NOT_ALLOWED);
        }
    }

    private boolean isPostSigningFinancialStatus(
            LeaseContractEntity contract,
            boolean renewalFlow
    ) {
        return List.of(LeaseStatus.SIGNED, LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON)
                .contains(contract.getStatus())
                || (contract.getStatus() == LeaseStatus.PENDING_SIGNATURE
                && (contract.getSignedFile() != null || contract.getSignedAt() != null))
                || (renewalFlow && contract.getStatus() == LeaseStatus.EXPIRED);
    }

    private LocalDate resolveRemainingTermStart(LeaseContractEntity contract) {
        LocalDate today = LocalDate.now();
        return contract.getStartDate() != null && contract.getStartDate().isAfter(today)
                ? contract.getStartDate()
                : today;
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
