package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity;

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
        name = "emergency_contacts",
        indexes = {
                @Index(name = "idx_emergency_profile", columnList = "tenant_profile_id")
        }
)
public class EmergencyContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_profile_id", nullable = false)
    PersonProfileEntity tenantProfile;

    @Column(name = "full_name", nullable = false, length = 255)
    String fullName;

    @Column(nullable = false, length = 100)
    String relationship;

    @Column(nullable = false, length = 30)
    String phone;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}
