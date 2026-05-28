package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentAllocation;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentAllocationEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentTransactionRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentAllocationPersistenceMapper {
    JpaPaymentTransactionRepository jpaPaymentTransactionRepository;
    JpaInvoiceRepository jpaInvoiceRepository;
    JpaUserRepository jpaUserRepository;

    public PaymentAllocation toDomain(PaymentAllocationEntity entity) {
        if (entity == null) return null;
        return PaymentAllocation.builder()
                .id(entity.getId())
                .paymentTransactionId(entity.getPaymentTransaction() != null
                        ? entity.getPaymentTransaction().getId() : null)
                .invoiceId(entity.getInvoice() != null
                        ? entity.getInvoice().getId() : null)
                .amount(entity.getAmount())
                .allocatedBy(entity.getAllocatedBy() != null
                        ? entity.getAllocatedBy().getId() : null)
                .allocatedAt(entity.getAllocatedAt())
                .build();
    }

    public PaymentAllocationEntity toEntity(PaymentAllocation domain) {
        if (domain == null) return null;
        return PaymentAllocationEntity.builder()
                .id(domain.getId())
                .paymentTransaction(domain.getPaymentTransactionId() != null
                        ? jpaPaymentTransactionRepository.getReferenceById(domain.getPaymentTransactionId())
                        : null)
                .invoice(domain.getInvoiceId() != null
                        ? jpaInvoiceRepository.getReferenceById(domain.getInvoiceId())
                        : null)
                .amount(domain.getAmount())
                .allocatedBy(domain.getAllocatedBy() != null
                        ? jpaUserRepository.getReferenceById(domain.getAllocatedBy())
                        : null)
                .allocatedAt(domain.getAllocatedAt())
                .build();
    }
}
