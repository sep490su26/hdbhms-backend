package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.CompleteInvoiceUseCase;
import com.sep490.hdbhms.billingandpayment.application.port.out.DepositCompletionPort;
import com.sep490.hdbhms.billingandpayment.application.port.out.DepositBatchCompletionPort;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.model.InvoiceLine;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentAllocation;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.occupancy.application.port.out.RoomTransferRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TransferSettlementRepository;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.model.TransferSettlement;
import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompleteInvoiceService implements CompleteInvoiceUseCase {
    DepositCompletionPort depositCompletionPort;
    DepositBatchCompletionPort depositBatchCompletionPort;
    TransferSettlementRepository transferSettlementRepository;
    RoomTransferRepository roomTransferRepository;
    InvoiceLineRepository invoiceLineRepository;
    RoomTransferUseCase roomTransferUseCase;

    @Override
    public void execute(Invoice invoice, PaymentAllocation paymentAllocation) {
        if (invoice == null || invoice.getStatus() != InvoiceStatus.PAID) {
            return;
        }

        switch (invoice.getInvoiceType()) {
            case DEPOSIT:
                if (invoice.getDepositBatchId() != null) {
                    depositBatchCompletionPort.execute(invoice);
                } else {
                    depositCompletionPort.execute(invoice);
                }
                break;
            case TRANSFER_DIFFERENCE:
                handleTransferDifferenceInvoicePaid(invoice);
                break;
            case OTHER:
            default:
                break;
        }
    }

    private void handleTransferDifferenceInvoicePaid(Invoice invoice) {
        if (invoice.getId() == null) {
            return;
        }
        TransferSettlement settlement = transferSettlementRepository
                .findByTransferDifferenceInvoiceId(invoice.getId())
                .orElse(null);
        Long transferRequestId = settlement != null
                ? settlement.getTransferRequestId()
                : invoiceLineRepository.findByInvoiceId(invoice.getId())
                .map(InvoiceLine::getSourceId)
                .orElse(null);
        if (transferRequestId == null) {
            return;
        }
        RoomTransferRequest transferRequest = roomTransferRepository
                .findById(transferRequestId)
                .orElse(null);
        if (transferRequest == null || !canAdvanceTransferAfterPayment(transferRequest)) {
            return;
        }
        Long confirmerUserId = settlement != null && settlement.getConfirmedById() != null
                ? settlement.getConfirmedById()
                : transferRequest.getRequesterId();
        if (confirmerUserId != null) {
            try {
                roomTransferUseCase.advanceTransferAfterDifferencePayment(transferRequestId, confirmerUserId);
                return;
            } catch (RuntimeException exception) {
                log.warn("Could not auto-confirm room transfer contract after payment. transferRequestId={}",
                        transferRequestId, exception);
            }
        }
    }

    private boolean canAdvanceTransferAfterPayment(RoomTransferRequest transferRequest) {
        return transferRequest.getStatus() == TransferRequestStatus.WAITING_PAYMENT
                || transferRequest.getStatus() == TransferRequestStatus.WAITING_TENANT_CONFIRMATION
                || transferRequest.getStatus() == TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION;
    }
}
