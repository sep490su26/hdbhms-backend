package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.CollectionAccountProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.CollectionAccountType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
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
        name = "collection_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_collection_account", columnNames = {
                        "provider", "account_number", "account_type"
                })
        },
        indexes = {
                @Index(name = "idx_collection_account_scope", columnList = "property_id, account_type, status")
        }
)
public class CollectionAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = true)
    PropertyEntity property;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    CollectionAccountType accountType;

    @Column(name = "bank_name", length = 100)
    String bankName;

    @Column(name = "account_number", length = 100)
    String accountNumber;

    @Column(name = "account_holder", length = 255)
    String accountHolder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    CollectionAccountProvider provider = CollectionAccountProvider.BANK;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    AccountStatus status = AccountStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}