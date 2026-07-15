package com.sep490.hdbhms.shared.config;

import com.sep490.hdbhms.shared.infrastructure.sms.twillio.TwillioProperties;
import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TwillioConfig {
    TwillioProperties twillioProperties;

    @PostConstruct
    public void initTwillio() {
        Twilio.init(twillioProperties.getAccountSid(), twillioProperties.getAuthToken());
    }
}
