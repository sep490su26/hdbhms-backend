package com.sep490.hdbhms.accounting.infrastructure.persistence.entity;

import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseAttachmentType;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
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
        name = "expense_attachments",
        indexes = {
                @Index(name = "idx_expense_attachment_expense", columnList = "operating_expense_id")
        }
)
public class ExpenseAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_attachment_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operating_expense_id", nullable = false)
    OperatingExpenseEntity operatingExpense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    FileMetadataEntity file;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 50)
    ExpenseAttachmentType attachmentType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
