package com.sep490.hdbhms.notification.application.port.in.usecase;

import com.sep490.hdbhms.notification.domain.valueObjects.Platform;

public interface ManageDeviceTokenUseCase {
    void registerDeviceToken(Long userId, String token, Platform platform);
}
