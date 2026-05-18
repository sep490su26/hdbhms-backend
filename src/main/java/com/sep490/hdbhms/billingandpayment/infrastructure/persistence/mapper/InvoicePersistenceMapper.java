package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper;

import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaCollectionAccountRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoicePersistenceMapper {
    JpaPropertyRepository propertyRepository;
    JpaRoomRepository roomRepository;
    JpaLeaseContractRepository contractRepository;
    JpaCollectionAccountRepository collectionAccountRepository;
    JpaUserRepository userRepository;

    public Invoice toDomain(InvoiceEntity entity) {
        if (entity == null) return null;
        Long propertyId = entity.getProperty() != null ? entity.getProperty().getId() : null;
        Long roomId = entity.getRoom() != null ? entity.getRoom().getId() : null;
        Long contractId = entity.getContract() != null ? entity.getContract().getId() : null;
        Long collectionAccountId = entity.getCollectionAccount() != null ? entity.getCollectionAccount().getId() : null;
        Long createdBy = entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null;

        return Invoice.builder()
                .id(entity.getId())
                .invoiceCode(entity.getInvoiceCode())
                .propertyId(propertyId)
                .roomId(roomId)
                .contractId(contractId)
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
                .issuedAt(entity.getIssuedAt() != null ? LocalDateTime.ofInstant(entity.getIssuedAt(), ZoneId.systemDefault()) : null)
                .voidedAt(entity.getVoidedAt() != null ? LocalDateTime.ofInstant(entity.getVoidedAt(), ZoneId.systemDefault()) : null)
                .voidReason(entity.getVoidReason())
                .createdAt(entity.getCreatedAt() != null ? LocalDateTime.ofInstant(entity.getCreatedAt(), ZoneId.systemDefault()) : null)
                .updatedAt(entity.getUpdatedAt() != null ? LocalDateTime.ofInstant(entity.getUpdatedAt(), ZoneId.systemDefault()) : null)
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
        var contract = domain.getContractId() != null
                ? contractRepository.findById(domain.getContractId()).orElse(null) : null;
        var collectionAccount = domain.getCollectionAccountId() != null
                ? collectionAccountRepository.findById(domain.getCollectionAccountId()).orElse(null) : null;
        var createdBy = domain.getCreatedBy() != null
                ? userRepository.findById(domain.getCreatedBy()).orElse(null) : null;

        return InvoiceEntity.builder()
                .id(domain.getId())
                .invoiceCode(domain.getInvoiceCode())
                .property(property)
                .room(room)
                .contract(contract)
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
                .issuedAt(domain.getIssuedAt() != null ? domain.getIssuedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .voidedAt(domain.getVoidedAt() != null ? domain.getVoidedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .voidReason(domain.getVoidReason())
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .updatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .version(domain.getVersion() != null ? domain.getVersion() : 0)
                .build();
    }
}
