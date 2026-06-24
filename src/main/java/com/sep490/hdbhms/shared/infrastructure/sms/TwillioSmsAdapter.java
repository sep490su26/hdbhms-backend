package com.sep490.hdbhms.shared.infrastructure.sms;

import com.sep490.hdbhms.shared.application.port.out.SmsPort;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TwillioSmsAdapter implements SmsPort {
    TwillioProperties twillioProperties;

    @Override
    public void send(String phoneNumber, String message) {
        log.info(toE164Vietnam(phoneNumber));
        Message.creator(
                new PhoneNumber(toE164Vietnam(phoneNumber)),
                new PhoneNumber(twillioProperties.getFromNumber()),
                message
        ).create();
    }

    private String toE164Vietnam(String phoneNumber) {
        if (phoneNumber.startsWith("0")) {
            return "+84" + phoneNumber.substring(1);
        }
        if (phoneNumber.startsWith("+84")) {
            return phoneNumber;
        }
        return phoneNumber;
    }
}
