package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.application.port.in.usecase.CompleteInvoiceUseCase;
import com.sep490.hdbhms.billingandpayment.application.port.out.DepositCompletionPort;
import com.sep490.hdbhms.billingandpayment.application.port.out.DepositBatchCompletionPort;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentAllocation;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompleteInvoiceService implements CompleteInvoiceUseCase {
    DepositCompletionPort depositCompletionPort;
    DepositBatchCompletionPort depositBatchCompletionPort;

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
            case OTHER:
            default:
                break;
        }
    }
}
