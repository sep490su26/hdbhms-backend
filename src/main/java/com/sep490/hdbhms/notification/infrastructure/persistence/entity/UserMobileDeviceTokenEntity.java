package com.sep490.hdbhms.notification.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.notification.domain.value_objects.Platform;
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
        name = "user_mobile_device_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_device_token", columnNames = "token")
        },
        indexes = {
                @Index(name = "idx_token_user", columnList = "user_id")
        }
)
public class UserMobileDeviceTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    UserEntity user;

    @Column(nullable = false, length = 500)
    String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    Platform platform = Platform.ANDROID;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}
