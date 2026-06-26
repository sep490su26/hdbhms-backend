package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_status_logs")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserStatusLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_status_log_id")
    Long id;

    @ManyToOne
    @JoinColumn(name = "user_entity_id", nullable = false)
    UserEntity userEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", nullable = false)
    AccountStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    AccountStatus newStatus;

    String reason;

    @Column(name = "changed_by")
    String changedBy;

    @CreatedDate
    @Column(name = "changed_at", updatable = false, nullable = false)
    LocalDateTime changedAt;
}