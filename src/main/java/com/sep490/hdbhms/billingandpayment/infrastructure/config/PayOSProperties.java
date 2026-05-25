package com.sep490.hdbhms.billingandpayment.infrastructure.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.payment.payos")
public class PayOSProperties {
    String clientId;
    String apiKey;
    String checksumKey;
    String returnUrl;
    String cancelUrl;

    @Bean
    public PayOS payOS() {
        return new PayOS(
                this.clientId,
                this.apiKey,
                this.checksumKey
        );
    }
}

