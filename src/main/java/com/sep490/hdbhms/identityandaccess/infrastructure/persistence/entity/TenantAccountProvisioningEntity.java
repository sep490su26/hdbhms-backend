package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.TenantAccountProvisioningStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "tenant_account_provisionings")
public class TenantAccountProvisioningEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tenant_account_provisioning_id")
    Long id;

    @Column(name = "tenant_profile_id", nullable = false, unique = true)
    Long tenantProfileId;

    @Column(name = "user_id")
    Long userId;

    @Column(name = "first_contract_id")
    Long firstContractId;

    @Column(name = "latest_contract_id")
    Long latestContractId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    TenantAccountProvisioningStatus status = TenantAccountProvisioningStatus.NOT_PROVISIONED;

    @Column(name = "recipient_email", length = 255)
    String recipientEmail;

    @Column(name = "sent_at")
    LocalDateTime sentAt;

    @Column(name = "failed_at")
    LocalDateTime failedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    String failureReason;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    Integer attemptCount = 0;

    @Column(name = "last_attempt_at")
    LocalDateTime lastAttemptAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}