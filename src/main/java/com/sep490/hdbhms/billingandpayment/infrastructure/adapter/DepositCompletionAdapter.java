package com.sep490.hdbhms.billingandpayment.infrastructure.adapter;

import com.sep490.hdbhms.billingandpayment.application.port.out.DepositCompletionPort;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.occupancy.application.service.DepositContractDocumentService;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositCompletionAdapter implements DepositCompletionPort {
    RoomRepository roomRepository;
    RoomHoldRepository roomHoldRepository;
    DepositAgreementRepository depositAgreementRepository;
    EarlyCancelRoomHoldTaskPort earlyCancelRoomHoldTaskPort;
    CreateLeadOrAssignTenantPort createLeadOrAssignTenantPort;
    DepositContractDocumentService depositContractDocumentService;


    @Override
    public void execute(Invoice invoice) {
        DepositAgreement depositAgreement = depositAgreementRepository.findById(
                        invoice.getDepositAgreementId()
                )
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        if (depositAgreement.getStatus() != DepositAgreementStatus.PENDING_PAYMENT) {
            return;
        }
        RoomHold roomHold = roomHoldRepository.findById(depositAgreement.getRoomHoldId())
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        if (roomHold.getStatus() != RoomHoldStatus.CONFIRMED) {
            roomHold.confirmPaidHold();
            roomHoldRepository.save(roomHold);
        }
        earlyCancelRoomHoldTaskPort.execute(roomHold.getId());
        int updatedRows = roomRepository.updateRoomStatusIfCurrent(
                depositAgreement.getRoomId(),
                RoomStatus.ON_HOLD,
                RoomStatus.RESERVED
        );
        if (updatedRows == 0) {
            roomRepository.updateRoomStatusIfCurrent(
                    depositAgreement.getRoomId(),
                    RoomStatus.VACANT,
                    RoomStatus.RESERVED
            );
        }
        createLeadOrAssignTenantPort.execute(depositAgreement);
        depositAgreement.markPaid();
        depositAgreement = depositAgreementRepository.save(depositAgreement);
        depositContractDocumentService.generateOfficialContractAfterCommit(depositAgreement.getId());
    }
}
