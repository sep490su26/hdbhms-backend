package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositFormEntity;
import com.sep490.hdbhms.booking.infrastructure.persistence.jpa.JpaDepositFormRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateDraftLeaseContractForDepositUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
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
    JpaDepositFormRepository depositFormRepository;
    JpaLeaseContractRepository leaseContractRepository;
    JpaRoomRepository roomRepository;
    LeaseContractWorkflowSupport workflowSupport;
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;

    @Override
    public LeaseContractManagementResponse execute(Long depositFormId) {
        DepositFormEntity deposit = depositFormRepository.findById(depositFormId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy form đặt cọc."));
        LeaseContractEntity existing = leaseContractRepository
                .findFirstByDepositForm_IdAndDeletedAtIsNull(depositFormId)
                .orElse(null);
        if (existing != null) {
            return getLeaseContractManagementUseCase.findOne(existing.getId());
        }
        if (deposit.getDepositStatus() != DepositAgreementStatus.PAID
                && deposit.getDepositStatus() != DepositAgreementStatus.CONFIRMED
                && deposit.getDepositStatus() != DepositAgreementStatus.CONVERTED_TO_LEASE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Form đặt cọc chưa được thanh toán.");
        }

        RoomEntity room = deposit.getRoom();
        PersonProfileEntity tenant = deposit.getDepositorPersonProfile();
        LocalDate startDate = deposit.getExpectedMoveInDate();
        if (room == null || tenant == null || startDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Form đặt cọc chưa đủ dữ liệu để tạo hợp đồng thuê.");
        }
        int termMonths = deposit.getContractTermMonths() == null ? 12 : deposit.getContractTermMonths();
        if (termMonths < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời hạn hợp đồng tối thiểu là 6 tháng.");
        }
        LocalDate endDate = startDate.plusMonths(termMonths).minusDays(1);
        int paymentCycleMonths = deposit.getPaymentCycleMonths() == null ? 1 : deposit.getPaymentCycleMonths();
        long monthlyRent = room.getListedPrice() == null ? 0L : room.getListedPrice();
        long depositAmount = deposit.getAmount() == null ? 0L : deposit.getAmount();
        workflowSupport.validateContractTerms(startDate, paymentCycleMonths, monthlyRent, depositAmount);

        String contractCode = "HD-" + startDate.getYear() + "-H" + room.getRoomCode() + "-" + depositFormId;
        LeaseContractEntity contract = LeaseContractEntity.builder()
                .contractCode(contractCode)
                .room(room)
                .depositForm(deposit)
                .primaryTenantProfile(tenant)
                .startDate(startDate)
                .endDate(endDate)
                .rentStartDate(workflowSupport.resolveRentStartDate(startDate))
                .monthlyRent(monthlyRent)
                .paymentCycleMonths(paymentCycleMonths)
                .depositAmount(depositAmount)
                .status(LeaseStatus.PENDING_SIGNATURE)
                .build();
        LeaseContractEntity saved = leaseContractRepository.save(contract);
        workflowSupport.ensureContractOccupants(saved);
        workflowSupport.appendContractEvent(saved.getId(), "CREATED", "Tạo hợp đồng thuê hợp nhất từ form đặt cọc " + depositFormId);
        return getLeaseContractManagementUseCase.findOne(saved.getId());
    }
}
