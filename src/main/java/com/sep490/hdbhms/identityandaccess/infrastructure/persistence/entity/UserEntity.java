package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_phone_active", columnNames = {
                        "phone", "active_unique_token"
                }),
                @UniqueConstraint(name = "uq_users_email_active", columnNames = {
                        "email", "active_unique_token"
                })
        },
        indexes = {
                @Index(name = "idx_users_status", columnList = "status, created_at")
        }
)
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    Long id;

    @Column(nullable = false, length = 30)
    String phone;

    @Column(nullable = false, length = 255)
    String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    String passwordHash;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    Role role = Role.LEAD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('PENDING_CONTRACT','ACTIVE','DISABLED') DEFAULT 'PENDING_CONTRACT'")
    @Builder.Default
    AccountStatus status = AccountStatus.PENDING_CONTRACT;

    @Builder.Default
    @Column(name = "must_change_password", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    Boolean mustChangePassword = false;

    @Column(name = "last_login_at")
    LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    LocalDateTime updatedAt;

    LocalDateTime deletedAt;

    @Column(name = "active_unique_token", insertable = false, updatable = false,
            columnDefinition = "TINYINT")
    Short activeUniqueToken;

    @OneToMany(mappedBy = "user")
    List<UserModificationHistoryEntity> modificationHistory;

    @PrePersist
    public void prePersist() {
        if (mustChangePassword == null) mustChangePassword = false;
        if (this.email != null) this.email = email.toLowerCase();
    }

    @PreUpdate
    public void normalize() {
        if (this.email != null) this.email = email.toLowerCase();
    }
}