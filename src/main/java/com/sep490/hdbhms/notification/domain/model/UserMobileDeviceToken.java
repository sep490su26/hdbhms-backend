package com.sep490.hdbhms.notification.domain.model;

import com.sep490.hdbhms.notification.domain.value_objects.Platform;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserMobileDeviceToken {
    Long id;
    Long userId;
    String token;
    Platform platform;
    Boolean isActive;
    LocalDateTime createdAt;

    public static UserMobileDeviceToken register(Long userId, String token, Platform platform) {
        return UserMobileDeviceToken.builder()
                .userId(userId)
                .token(token)
                .platform(platform)
                .isActive(true)
                .build();
    }
}