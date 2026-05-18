package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper;

import com.sep490.hdbhms.billingandpayment.domain.model.InvoicePaymentGroup;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoicePaymentGroupEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaCollectionAccountRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentIntentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoicePaymentGroupPersistenceMapper {
    JpaInvoiceRepository invoiceRepository;
    JpaCollectionAccountRepository collectionAccountRepository;
    JpaPaymentIntentRepository paymentIntentRepository;

    public InvoicePaymentGroup toDomain(InvoicePaymentGroupEntity entity) {
        if (entity == null) return null;
        Long invoiceId = entity.getInvoice() != null ? entity.getInvoice().getId() : null;
        Long collectionAccountId = entity.getCollectionAccount() != null ? entity.getCollectionAccount().getId() : null;
        Long paymentIntentId = entity.getPaymentIntent() != null ? entity.getPaymentIntent().getId() : null;

        return InvoicePaymentGroup.builder()
                .id(entity.getId())
                .invoiceId(invoiceId)
                .collectionAccountId(collectionAccountId)
                .groupType(entity.getGroupType())
                .amount(entity.getAmount())
                .paymentIntentId(paymentIntentId)
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public InvoicePaymentGroupEntity toEntity(InvoicePaymentGroup domain) {
        if (domain == null) return null;
        var invoice = domain.getInvoiceId() != null
                ? invoiceRepository.findById(domain.getInvoiceId()).orElse(null) : null;
        var collectionAccount = domain.getCollectionAccountId() != null
                ? collectionAccountRepository.findById(domain.getCollectionAccountId()).orElse(null) : null;
        var paymentIntent = domain.getPaymentIntentId() != null
                ? paymentIntentRepository.findById(domain.getPaymentIntentId()).orElse(null) : null;

        return InvoicePaymentGroupEntity.builder()
                .id(domain.getId())
                .invoice(invoice)
                .collectionAccount(collectionAccount)
                .groupType(domain.getGroupType())
                .amount(domain.getAmount())
                .paymentIntent(paymentIntent)
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
