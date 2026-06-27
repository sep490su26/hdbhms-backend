package com.sep490.hdbhms.maintenance.infrastructure.persistence.mapper;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceCost;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceCostEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaintenanceCostPersistenceMapper {
    JpaMaintenanceTicketRepository jpaMaintenanceTicketRepository;
    JpaInvoiceRepository jpaInvoiceRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaUserRepository jpaUserRepository;

    public MaintenanceCost toDomain(MaintenanceCostEntity entity) {
        if (entity == null) return null;
        return MaintenanceCost.builder()
                .id(entity.getId())
                .ticketId(entity.getTicket() != null ? entity.getTicket().getId() : null)
                .costType(entity.getCostType())
                .description(entity.getDescription())
                .amount(entity.getAmount())
                .paidBy(entity.getPaidBy())
                .costResponsibility(entity.getCostResponsibility())
                .chargeInvoiceId(entity.getChargeInvoice() != null ? entity.getChargeInvoice().getId() : null)
                .receiptFileId(entity.getReceiptFile() != null ? entity.getReceiptFile().getId() : null)
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public MaintenanceCostEntity toEntity(MaintenanceCost domain) {
        if (domain == null) return null;
        return MaintenanceCostEntity.builder()
                .id(domain.getId())
                .ticket(domain.getTicketId() != null
                        ? jpaMaintenanceTicketRepository.getReferenceById(domain.getTicketId())
                        : null)
                .costType(domain.getCostType())
                .description(domain.getDescription())
                .amount(domain.getAmount())
                .paidBy(domain.getPaidBy())
                .costResponsibility(domain.getCostResponsibility())
                .chargeInvoice(domain.getChargeInvoiceId() != null
                        ? jpaInvoiceRepository.getReferenceById(domain.getChargeInvoiceId())
                        : null)
                .receiptFile(domain.getReceiptFileId() != null
                        ? jpaFileMetadataRepository.getReferenceById(domain.getReceiptFileId())
                        : null)
                .createdBy(domain.getCreatedById() != null
                        ? jpaUserRepository.getReferenceById(domain.getCreatedById())
                        : null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
