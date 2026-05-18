package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper;

import com.sep490.hdbhms.billingandpayment.domain.model.InvoiceLine;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaCollectionAccountRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoiceLinePersistenceMapper {
    JpaInvoiceRepository invoiceRepository;
    JpaMeterReadingRepository meterReadingRepository;
    JpaCollectionAccountRepository collectionAccountRepository;

    public InvoiceLine toDomain(InvoiceLineEntity entity) {
        if (entity == null) return null;
        Long invoiceId = entity.getInvoice() != null ? entity.getInvoice().getId() : null;
        Long meterReadingId = entity.getMeterReading() != null ? entity.getMeterReading().getId() : null;
        Long collectionAccountId = entity.getCollectionAccount() != null ? entity.getCollectionAccount().getId() : null;

        return InvoiceLine.builder()
                .id(entity.getId())
                .invoiceId(invoiceId)
                .lineType(entity.getLineType())
                .description(entity.getDescription())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .amount(entity.getAmount())
                .meterReadingId(meterReadingId)
                .sourceType(entity.getSourceType())
                .sourceId(entity.getSourceId())
                .collectionAccountId(collectionAccountId)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public InvoiceLineEntity toEntity(InvoiceLine domain) {
        if (domain == null) return null;
        var invoice = domain.getInvoiceId() != null
                ? invoiceRepository.findById(domain.getInvoiceId()).orElse(null) : null;
        var meterReading = domain.getMeterReadingId() != null
                ? meterReadingRepository.findById(domain.getMeterReadingId()).orElse(null) : null;
        var collectionAccount = domain.getCollectionAccountId() != null
                ? collectionAccountRepository.findById(domain.getCollectionAccountId()).orElse(null) : null;

        return InvoiceLineEntity.builder()
                .id(domain.getId())
                .invoice(invoice)
                .lineType(domain.getLineType())
                .description(domain.getDescription())
                .quantity(domain.getQuantity())
                .unitPrice(domain.getUnitPrice())
                .meterReading(meterReading)
                .sourceType(domain.getSourceType())
                .sourceId(domain.getSourceId())
                .collectionAccount(collectionAccount)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
