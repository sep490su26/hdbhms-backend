package com.sep490.hdbhms.notification.infrastructure.external;

import com.sep490.hdbhms.shared.application.port.out.MailSenderPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailNotificationService {
    MailSenderPort mailSenderPort;

    public void send(String recipient, String subject, String body) {
        mailSenderPort.sendMail(recipient, subject, body, false, false);
    }
}