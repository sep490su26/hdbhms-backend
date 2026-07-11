package com.sep490.hdbhms.notification.application.port.in.usecase;

import com.sep490.hdbhms.notification.domain.value_objects.Platform;

public interface ManageDeviceTokenUseCase {
    void registerDeviceToken(Long userId, String token, Platform platform);
}
