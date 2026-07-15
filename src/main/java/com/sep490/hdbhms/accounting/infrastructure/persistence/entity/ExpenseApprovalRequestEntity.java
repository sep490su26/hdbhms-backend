package com.sep490.hdbhms.accounting.infrastructure.persistence.entity;

import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "expense_approval_requests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_expense_approval_expense", columnNames = "operating_expense_id"),
                @UniqueConstraint(name = "uq_expense_approval_change_request", columnNames = "change_request_id")
        },
        indexes = {
                @Index(name = "idx_expense_approval_expected_date", columnList = "expected_payment_date")
        }
)
public class ExpenseApprovalRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_approval_request_id")
    Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operating_expense_id", nullable = false)
    OperatingExpenseEntity operatingExpense;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "change_request_id", nullable = false)
    ChangeRequestEntity changeRequest;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    String reason;

    @Column(name = "vendor_name", length = 255)
    String vendorName;

    @Column(name = "expected_payment_date")
    LocalDate expectedPaymentDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
