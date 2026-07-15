package com.sep490.hdbhms.accounting.infrastructure.persistence.entity;

import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseStatus;
import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseType;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
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
        name = "operating_expenses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_operating_expense_code", columnNames = "expense_code")
        },
        indexes = {
                @Index(name = "idx_operating_expense_property", columnList = "property_id, expense_date, status")
        }
)
public class OperatingExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operating_expense_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    PropertyEntity property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    RoomEntity room;

    /**
     * Storing ticketId as Long since MaintenanceTicket is in another deep domain
     */
    @Column(name = "ticket_id")
    Long ticketId;

    @Column(name = "expense_code", nullable = false, length = 80)
    String expenseCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", nullable = false, length = 50)
    ExpenseType expenseType;

    @Column(columnDefinition = "TEXT", nullable = false)
    String description;

    @Column(nullable = false)
    Long amount;

    @Column(name = "expense_date", nullable = false)
    LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_user_id")
    UserEntity paidByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_file_id")
    FileMetadataEntity receiptFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    ExpenseStatus status = ExpenseStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    UserEntity approvedBy;

    @Column(name = "approved_at")
    LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    UserEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}