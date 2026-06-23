package com.sep490.hdbhms.notification.application.service;

import com.sep490.hdbhms.notification.application.port.in.usecase.ManageDeviceTokenUseCase;
import com.sep490.hdbhms.notification.application.port.out.UserMobileDeviceTokenRepository;
import com.sep490.hdbhms.notification.domain.model.UserMobileDeviceToken;
import com.sep490.hdbhms.notification.domain.value_objects.Platform;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceTokenService implements ManageDeviceTokenUseCase {

    UserMobileDeviceTokenRepository userMobileDeviceTokenRepository;

    @Override
    public void registerDeviceToken(Long userId, String token, Platform platform) {
        Optional<UserMobileDeviceToken> existingToken = userMobileDeviceTokenRepository.findByUserIdAndToken(userId, token);
        if (existingToken.isEmpty()) {
            UserMobileDeviceToken newToken = UserMobileDeviceToken.builder()
                    .userId(userId)
                    .token(token)
                    .platform(platform)
                    .isActive(true)
                    .build();
            userMobileDeviceTokenRepository.save(newToken);
        }
    }
}
