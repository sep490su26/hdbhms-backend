package com.sep490.hdbhms.shared.infrastructure.sms;

import com.sep490.hdbhms.shared.application.port.out.SmsPort;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TwillioSmsAdapter implements SmsPort {
    TwillioProperties twillioProperties;

    @Override
    public void send(String phoneNumber, String message) {
        Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twillioProperties.getFromNumber()),
                message
        ).create();
    }
}
