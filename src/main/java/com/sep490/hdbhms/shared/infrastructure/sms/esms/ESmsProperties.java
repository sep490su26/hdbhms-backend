package com.sep490.hdbhms.shared.infrastructure.sms.esms;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "esms")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ESmsProperties {
    String apiKey;
    String secretKey;
    String brandName;
}
