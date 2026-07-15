package com.sep490.hdbhms.notification.infrastructure.external;

import com.sep490.hdbhms.shared.application.port.out.SmsPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SmsNotificationService {
    SmsPort smsPort;

    public void send(String recipient, String body) {
        smsPort.send(recipient, body);
    }
}