package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveDepositFormCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ConfirmDepositPaymentCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ApproveDepositFormUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApproveDepositFormService implements ApproveDepositFormUseCase {
    RoomRepository roomRepository;
    RoomHoldRepository roomHoldRepository;
    DepositFormRepository depositFormRepository;
    SendDepositPaymentPort sendDepositPaymentPort;
    CreateRoomHoldTaskPort createRoomHoldTaskPort;
    ConfirmPaymentIntentPort confirmPaymentIntentPort;
    EarlyCancelRoomHoldTaskPort earlyCancelRoomHoldTaskPort;
    CreateLeadOrAssignTenantPort createLeadOrAssignTenantPort;

    @Override
    public void approveAndInitiatePayment(ApproveDepositFormCommand command) {
        DepositForm depositForm = depositFormRepository.findById(command.depositFormId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        depositForm.approveDepositForm();
        depositFormRepository.save(depositForm);
        RoomHold roomHold = RoomHold.createRoomHoldForGuest(
                depositForm.getRoomId(),
                LocalDateTime.now().plusMinutes(15)
        );
        roomHold = roomHoldRepository.save(roomHold);
        createRoomHoldTaskPort.execute(roomHold);

        sendDepositPaymentPort.execute(depositForm);
    }

    @Override
    public void confirmPayment(ConfirmDepositPaymentCommand command) {
        DepositAgreement depositAgreement = confirmPaymentIntentPort
                .execute(command.paymentIntentId(), command.paymentStatus());



        RoomHold roomHold = roomHoldRepository.findById(depositAgreement.getRoomHoldId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        roomHold.confirm();
        roomHoldRepository.save(roomHold);
        earlyCancelRoomHoldTaskPort.execute(roomHold.getId());

        createLeadOrAssignTenantPort.execute(depositAgreement);
    }
}
