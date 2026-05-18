package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentIntentEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaCollectionAccountRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoicePaymentGroupRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentIntentPersistenceMapper {
    JpaInvoiceRepository invoiceRepository;
    JpaDepositAgreementRepository depositAgreementRepository;
    JpaInvoicePaymentGroupRepository invoicePaymentGroupRepository;
    JpaCollectionAccountRepository collectionAccountRepository;

    public PaymentIntent toDomain(PaymentIntentEntity entity) {
        if (entity == null) return null;
        Long invoiceId = entity.getInvoice() != null ? entity.getInvoice().getId() : null;
        Long depositAgreementId = entity.getDepositAgreement() != null ? entity.getDepositAgreement().getId() : null;
        Long invoicePaymentGroupId = entity.getInvoicePaymentGroup() != null ? entity.getInvoicePaymentGroup().getId() : null;
        Long collectionAccountId = entity.getCollectionAccount() != null ? entity.getCollectionAccount().getId() : null;
        
        return PaymentIntent.builder()
                .id(entity.getId())
                .invoiceId(invoiceId)
                .depositAgreementId(depositAgreementId)
                .invoicePaymentGroupId(invoicePaymentGroupId)
                .amount(entity.getAmount())
                .provider(entity.getProvider())
                .collectionAccountId(collectionAccountId)
                .paymentContent(entity.getPaymentContent())
                .qrPayload(entity.getQrPayload())
                .status(entity.getStatus())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public PaymentIntentEntity toEntity(PaymentIntent domain) {
        if (domain == null) return null;
        var invoice = domain.getInvoiceId() != null 
                ? invoiceRepository.findById(domain.getInvoiceId()).orElse(null) : null;
        var depositAgreement = domain.getDepositAgreementId() != null 
                ? depositAgreementRepository.findById(domain.getDepositAgreementId()).orElse(null) : null;
        var invoicePaymentGroup = domain.getInvoicePaymentGroupId() != null 
                ? invoicePaymentGroupRepository.findById(domain.getInvoicePaymentGroupId()).orElse(null) : null;
        var collectionAccount = domain.getCollectionAccountId() != null 
                ? collectionAccountRepository.findById(domain.getCollectionAccountId()).orElse(null) : null;

        return PaymentIntentEntity.builder()
                .id(domain.getId())
                .invoice(invoice)
                .depositAgreement(depositAgreement)
                .invoicePaymentGroup(invoicePaymentGroup)
                .amount(domain.getAmount())
                .provider(domain.getProvider())
                .collectionAccount(collectionAccount)
                .paymentContent(domain.getPaymentContent())
                .qrPayload(domain.getQrPayload())
                .status(domain.getStatus())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
