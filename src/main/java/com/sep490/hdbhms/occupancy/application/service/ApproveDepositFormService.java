package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveDepositFormCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ConfirmDepositPaymentCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ApproveDepositFormUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.Lead;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApproveDepositFormService implements ApproveDepositFormUseCase {
    RoomRepository roomRepository;
    DepositFormRepository depositFormRepository;
    SendDepositPaymentPort sendDepositPaymentPort;
    CreateLeadUserPort createLeadUserPort;
    ConfirmPaymentIntentPort confirmPaymentIntentPort;

    @Override
    public void approveAndInitiatePayment(ApproveDepositFormCommand command) {
        DepositForm depositForm = depositFormRepository.findById(command.depositFormId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        depositForm.approveDepositForm();
        depositFormRepository.save(depositForm);
        Lead lead = createLeadUserPort.execute(depositForm.getId());
        sendDepositPaymentPort.execute(depositForm, lead.getAssignedUserId());
    }

    @Override
    public void confirmPayment(ConfirmDepositPaymentCommand command) {
        confirmPaymentIntentPort.execute(command.paymentIntentId(), command.paymentStatus());
    }
}
