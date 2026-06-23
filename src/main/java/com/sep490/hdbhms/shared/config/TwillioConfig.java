package com.sep490.hdbhms.shared.config;

import com.sep490.hdbhms.shared.infrastructure.sms.TwillioProperties;
import com.twilio.Twilio;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TwillioProperties.class)
public class TwillioConfig {
    @Bean
    public void initTwillio(TwillioProperties twillioProperties) {
        Twilio.init(twillioProperties.getAccountSid(), twillioProperties.getAuthToken());
    }
}
