package com.sep490.hdbhms.shared.config;

import com.sep490.hdbhms.shared.infrastructure.sms.TwillioProperties;
import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EnableConfigurationProperties(TwillioProperties.class)
public class TwillioConfig {
    TwillioProperties twillioProperties;

    @PostConstruct
    public void initTwillio() {
        Twilio.init(twillioProperties.getAccountSid(), twillioProperties.getAuthToken());
    }
}
