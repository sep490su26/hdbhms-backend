package com.sep490.hdbhms.shared.infrastructure.sms;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "twillio")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TwillioProperties {
    String accountSid;
    String authToken;
    String fromNumber;
}
