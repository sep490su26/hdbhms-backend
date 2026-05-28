package com.sep490.hdbhms.identityandaccess.infrastructure.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.reset-password")
public class ResetPasswordConfig {
    String webConfirmationUrl;
    String mobileConfirmationUrl;
}
