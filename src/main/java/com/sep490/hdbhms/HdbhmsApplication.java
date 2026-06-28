package com.sep490.hdbhms;

import com.sep490.hdbhms.billingandpayment.infrastructure.config.PayOSProperties;
import com.sep490.hdbhms.billingandpayment.infrastructure.config.VNPayProperties;
import com.sep490.hdbhms.file.infrastructure.config.FileProperties;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.ResetPasswordConfig;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.AuthProperties;
import com.sep490.hdbhms.shared.constant.DefaultConfig;
import com.sep490.hdbhms.shared.infrastructure.sms.esms.ESmsProperties;
import com.sep490.hdbhms.shared.infrastructure.sms.twillio.TwillioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.sep490.*"}, exclude = {RedisRepositoriesAutoConfiguration.class})
@EnableConfigurationProperties({
        AuthProperties.class,
        DefaultConfig.class,
        FileProperties.class,
        ResetPasswordConfig.class,
        VNPayProperties.class,
        PayOSProperties.class,
        TwillioProperties.class,
        ESmsProperties.class,
})
public class HdbhmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HdbhmsApplication.class, args);
    }

}
