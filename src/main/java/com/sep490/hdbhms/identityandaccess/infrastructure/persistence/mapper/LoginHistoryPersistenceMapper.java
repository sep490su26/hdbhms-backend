package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.domain.model.LoginHistory;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.LoginHistoryEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginHistoryPersistenceMapper {
    public LoginHistory toDomain(LoginHistoryEntity entity) {
        return LoginHistory.builder()
//                .id(entity.getId())
                .userId(entity.getUserId())
                .loggedInAt(entity.getLoggedInAt())
                .userAgent(entity.getUserAgent())
                .method(entity.getMethod())
                .sessionId(entity.getSessionId())
                .status(entity.getStatus())
                .deviceId(entity.getDeviceId())
                .ipAddress(entity.getIpAddress())
                .build();
    }

    public LoginHistoryEntity toEntity(LoginHistory domain) {
        return LoginHistoryEntity.builder()
//                .id(domain.getId())
                .userId(domain.getUserId())
                .deviceId(domain.getDeviceId())
                .loggedInAt(domain.getLoggedInAt())
                .userAgent(domain.getUserAgent())
                .method(domain.getMethod())
                .sessionId(domain.getSessionId())
                .status(domain.getStatus())
                .ipAddress(domain.getIpAddress())
                .build();
    }
}
