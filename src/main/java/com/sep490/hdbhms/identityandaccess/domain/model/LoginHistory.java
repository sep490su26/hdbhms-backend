package com.sep490.hdbhms.identityandaccess.domain.model;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.LoginMethod;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.LoginStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginHistory {
    final String id;
    Long userId;

    @Enumerated(EnumType.STRING)
    LoginStatus status;

    String ipAddress;
    String userAgent;

    @Enumerated(EnumType.STRING)
    LoginMethod method;

    String sessionId;
    String deviceId;

    final LocalDateTime loggedInAt;

    public static LoginHistory newAccountModificationHistory(
            Long accountId,
            LoginStatus status,
            String ipAddress,
            String userAgent,
            LoginMethod method,
            String sessionId,
            String deviceId
    ) {
        return LoginHistory.builder()
                .userId(accountId)
                .status(status)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .method(method)
                .sessionId(sessionId)
                .deviceId(deviceId)
                .build();
    }
}
