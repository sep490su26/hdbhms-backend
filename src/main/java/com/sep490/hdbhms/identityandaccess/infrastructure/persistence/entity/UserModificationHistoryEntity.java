package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.ModificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "user_modification_histories")
public class UserModificationHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ModificationType type;

    @Column(name = "old_value")
    String oldValue;

    @Column(name = "new_value")
    String newValue;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false, nullable = false)
    LocalDateTime changedAt;
}
