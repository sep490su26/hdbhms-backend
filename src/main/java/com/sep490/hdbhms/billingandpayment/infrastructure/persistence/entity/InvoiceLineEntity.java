package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "invoice_lines",
        indexes = {
                @Index(name = "idx_invoice_lines_invoice", columnList = "invoice_id"),
                @Index(name = "idx_invoice_lines_source", columnList = "source_type, source_id")
        }
)
public class InvoiceLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_line_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    InvoiceEntity invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 50)
    InvoiceLineType lineType;

    @Column(nullable = false, length = 1000)
    String description;

    @Column(nullable = false)
    @Builder.Default
    Integer quantity = 1;

    @Column(name = "unit_price", nullable = false)
    @Builder.Default
    Long unitPrice = 0L;

    @Column(name = "amount", insertable = false, updatable = false, nullable = false)
    Long amount;  // generated column

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_reading_id", nullable = true)
    MeterReadingEntity meterReading;

    @Column(name = "source_type", length = 100)
    String sourceType;

    @Column(name = "source_id")
    Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_account_id", nullable = true)
    CollectionAccountEntity collectionAccount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}