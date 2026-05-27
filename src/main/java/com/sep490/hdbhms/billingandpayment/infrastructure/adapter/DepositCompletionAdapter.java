package com.sep490.hdbhms.billingandpayment.infrastructure.adapter;

import com.sep490.hdbhms.billingandpayment.application.port.out.DepositCompletionPort;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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


    @Override
    public void execute(Invoice invoice) {
        DepositAgreement depositAgreement = depositAgreementRepository.findById(
                        invoice.getDepositAgreementId()
                )
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (depositAgreement.getStatus() != DepositAgreementStatus.PENDING_PAYMENT) {
            return;
        }
        RoomHold roomHold = roomHoldRepository.findById(depositAgreement.getRoomHoldId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        roomHold.confirm();
        roomHoldRepository.save(roomHold);
        earlyCancelRoomHoldTaskPort.execute(roomHold.getId());
        createLeadOrAssignTenantPort.execute(depositAgreement);
        Room room = roomRepository.findById(depositAgreement.getRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        room.reserveRoom();
        roomRepository.save(room);
    }
}
