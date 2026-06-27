package com.sep490.hdbhms.notification.application.port.out;

import com.sep490.hdbhms.notification.domain.model.UserMobileDeviceToken;

import java.util.List;

public interface UserMobileDeviceTokenRepository {
    UserMobileDeviceToken save(UserMobileDeviceToken mobileDeviceToken);

    List<String> findActiveTokenByUserId(Long userId);

    java.util.Optional<UserMobileDeviceToken> findByUserIdAndToken(Long userId, String token);

    void deleteByToken(String token);
}
