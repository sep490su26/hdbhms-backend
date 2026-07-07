package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.LoginMethod;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.LoginStatus;
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
@Table(name = "login_history")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_history_id")
    Long id;                                  // now BIGINT AUTO_INCREMENT

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    LoginStatus status;

    @Column(name = "ip_address", length = 45)
    String ipAddress;

    @Column(name = "user_agent", length = 500)
    String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    LoginMethod method;

    @Column(name = "session_id")
    String sessionId;

    @Column(name = "device_id")
    String deviceId;

    @CreationTimestamp
    @Column(name = "logged_in_at", updatable = false, nullable = false)
    LocalDateTime loggedInAt;
}
