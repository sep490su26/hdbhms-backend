package com.sep490.hdbhms.accounting.infrastructure.persistence.entity;

import com.sep490.hdbhms.accounting.domain.value_objects.ExpensePaymentMethod;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "expense_payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_expense_payment_expense", columnNames = "operating_expense_id")
        },
        indexes = {
                @Index(name = "idx_expense_payment_date", columnList = "payment_date")
        }
)
public class ExpensePaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_payment_id")
    Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operating_expense_id", nullable = false)
    OperatingExpenseEntity operatingExpense;

    @Column(name = "payment_date", nullable = false)
    LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    ExpensePaymentMethod paymentMethod;

    @Column(name = "payment_reference", length = 100)
    String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_file_id")
    FileMetadataEntity receiptFile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_user_id", nullable = false)
    UserEntity paidBy;

    @Column(name = "paid_at", nullable = false)
    LocalDateTime paidAt;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
