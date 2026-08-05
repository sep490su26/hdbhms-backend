package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateDraftLeaseContractForDepositUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositAgreementEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateDraftLeaseContractForDepositService implements CreateDraftLeaseContractForDepositUseCase {
    JpaLeaseContractRepository leaseContractRepository;
    LeaseContractWorkflowSupport workflowSupport;
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;

    @Override
    public LeaseContractManagementResponse execute(Long depositAgreementId) {
        DepositAgreementEntity deposit = workflowSupport.getReadyDeposit(depositAgreementId);
        LeaseContractEntity existing = workflowSupport.findLatestContractByDeposit(depositAgreementId);
        if (existing != null) {
            return getLeaseContractManagementUseCase.findOne(existing.getId());
        }
        LeaseContractEntity created = createDraftLeaseContract(deposit);
        return getLeaseContractManagementUseCase.findOne(created.getId());
    }

    private LeaseContractEntity createDraftLeaseContract(DepositAgreementEntity deposit) {
        RoomEntity room = deposit.getRoom();
        LocalDate startDate = deposit.getExpectedMoveInDate();
        if (startDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng cọc chưa có ngày vào ở dự kiến.");
        }
        LocalDate endDate = startDate.plusYears(1).minusDays(1);
        Integer paymentCycleMonths = workflowSupport.resolvePaymentCycleMonths(deposit);
        Long monthlyRent = room.getListedPrice();
        Long depositAmount = deposit.getAmount() != null ? deposit.getAmount() : 0L;
        workflowSupport.validateDraftInput(
                room,
                deposit.getDepositorPersonProfile() != null ? deposit.getDepositorPersonProfile().getId() : null,
                startDate,
                endDate,
                paymentCycleMonths,
                monthlyRent,
                depositAmount,
                workflowSupport.countRequestedOccupants(deposit)
        );

        String contractCode = "HD-" + startDate.getYear() + "-H" + room.getRoomCode() + "-" + deposit.getId();
        LeaseContractEntity contract = LeaseContractEntity.builder()
                .contractCode(contractCode)
                .room(room)
                .depositAgreement(deposit)
                .primaryTenantProfile(deposit.getDepositorPersonProfile())
                .startDate(startDate)
                .endDate(endDate)
                .rentStartDate(workflowSupport.resolveRentStartDate(startDate))
                .monthlyRent(monthlyRent)
                .paymentCycleMonths(paymentCycleMonths)
                .depositAmount(depositAmount)
                .status(LeaseStatus.PENDING_SIGNATURE)
                .build();
        LeaseContractEntity saved = leaseContractRepository.save(contract);
        workflowSupport.ensureContractOccupants(saved, deposit);
        workflowSupport.appendContractEvent(saved.getId(), "CREATED", "Tạo hợp đồng thuê từ hợp đồng cọc " + deposit.getId());
        return saved;
    }
}
