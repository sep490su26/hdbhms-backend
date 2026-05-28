package com.sep490.hdbhms.billingandpayment.infrastructure.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.payment.vnpay")
public class VNPayProperties {
    String tmnCode;
    String hashSecret;
    String url;
    String returnUrl;
}
