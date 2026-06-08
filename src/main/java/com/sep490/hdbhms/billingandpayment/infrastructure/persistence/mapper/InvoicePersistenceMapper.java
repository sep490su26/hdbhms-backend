package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper;

import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaCollectionAccountRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositBatchRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoicePersistenceMapper {
    JpaRoomRepository roomRepository;
    JpaUserRepository userRepository;
    JpaPropertyRepository propertyRepository;
    JpaLeaseContractRepository leaseContractRepository;
    JpaDepositAgreementRepository depositAgreementRepository;
    JpaDepositBatchRepository depositBatchRepository;
    JpaCollectionAccountRepository collectionAccountRepository;

    public Invoice toDomain(InvoiceEntity entity) {
        if (entity == null) return null;
        Long propertyId = entity.getProperty() != null ? entity.getProperty().getId() : null;
        Long roomId = entity.getRoom() != null ? entity.getRoom().getId() : null;
        Long leaseContractId = entity.getLeastContract() != null ? entity.getLeastContract().getId() : null;
        Long depositAgreementId = entity.getDepositAgreement() != null ? entity.getDepositAgreement().getId() : null;
        Long depositBatchId = entity.getDepositBatch() != null ? entity.getDepositBatch().getId() : null;
        Long collectionAccountId = entity.getCollectionAccount() != null ? entity.getCollectionAccount().getId() : null;
        Long createdBy = entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null;

        return Invoice.builder()
                .id(entity.getId())
                .invoiceCode(entity.getInvoiceCode())
                .propertyId(propertyId)
                .roomId(roomId)
                .leaseContractId(leaseContractId)
                .depositAgreementId(depositAgreementId)
                .depositBatchId(depositBatchId)
                .invoiceType(entity.getInvoiceType())
                .revisionNo(entity.getRevisionNo())
                .billingPeriod(entity.getBillingPeriod())
                .issueDate(entity.getIssueDate())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .subtotalAmount(entity.getSubtotalAmount())
                .discountAmount(entity.getDiscountAmount())
                .totalAmount(entity.getTotalAmount())
                .paidAmount(entity.getPaidAmount())
                .remainingAmount(entity.getRemainingAmount())
                .collectionAccountId(collectionAccountId)
                .createdBy(createdBy)
                .issuedAt(entity.getIssuedAt() != null ? entity.getIssuedAt() : null)
                .voidedAt(entity.getVoidedAt() != null ? entity.getVoidedAt() : null)
                .voidReason(entity.getVoidReason())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : null)
                .version(entity.getVersion())
                .activeInvoiceKey(entity.getActiveInvoiceKey())
                .build();
    }

    public InvoiceEntity toEntity(Invoice domain) {
        if (domain == null) return null;
        var property = domain.getPropertyId() != null
                ? propertyRepository.findById(domain.getPropertyId()).orElse(null) : null;
        var room = domain.getRoomId() != null
                ? roomRepository.findById(domain.getRoomId()).orElse(null) : null;
        var leaseContract = domain.getLeaseContractId() != null
                ? leaseContractRepository.findById(domain.getLeaseContractId()).orElse(null) : null;
        var depositAgreement = domain.getDepositAgreementId() != null
                ? depositAgreementRepository.findById(domain.getDepositAgreementId()).orElse(null) : null;
        var depositBatch = domain.getDepositBatchId() != null
                ? depositBatchRepository.findById(domain.getDepositBatchId()).orElse(null) : null;
        var collectionAccount = domain.getCollectionAccountId() != null
                ? collectionAccountRepository.findById(domain.getCollectionAccountId()).orElse(null) : null;
        var createdBy = domain.getCreatedBy() != null
                ? userRepository.findById(domain.getCreatedBy()).orElse(null) : null;

        return InvoiceEntity.builder()
                .id(domain.getId())
                .invoiceCode(domain.getInvoiceCode())
                .property(property)
                .room(room)
                .leastContract(leaseContract)
                .depositAgreement(depositAgreement)
                .depositBatch(depositBatch)
                .invoiceType(domain.getInvoiceType())
                .revisionNo(domain.getRevisionNo())
                .billingPeriod(domain.getBillingPeriod())
                .issueDate(domain.getIssueDate())
                .dueDate(domain.getDueDate())
                .status(domain.getStatus())
                .subtotalAmount(domain.getSubtotalAmount())
                .discountAmount(domain.getDiscountAmount())
                .totalAmount(domain.getTotalAmount())
                .paidAmount(domain.getPaidAmount())
                .remainingAmount(domain.getRemainingAmount())
                .collectionAccount(collectionAccount)
                .createdBy(createdBy)
                .issuedAt(domain.getIssuedAt() != null ? domain.getIssuedAt() : null)
                .voidedAt(domain.getVoidedAt() != null ? domain.getVoidedAt() : null)
                .voidReason(domain.getVoidReason())
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : null)
                .updatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : null)
                .version(domain.getVersion() != null ? domain.getVersion() : 0)
                .build();
    }
}
