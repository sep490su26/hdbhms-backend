package com.sep490.hdbhms.shared.infrastructure.sms.android_sms;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "android-sms")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AndroidSmsGatewayProperties {
    String serverAddress;
    String username;
    String password;
}
