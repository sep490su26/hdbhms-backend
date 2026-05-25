package com.sep490.hdbhms;

import com.sep490.hdbhms.billingandpayment.infrastructure.config.PayOSProperties;
import com.sep490.hdbhms.billingandpayment.infrastructure.config.VNPayProperties;
import com.sep490.hdbhms.file.infrastructure.config.FileProperties;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.ResetPasswordConfig;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.AuthProperties;
import com.sep490.hdbhms.shared.constant.DefaultConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.sep490.*"}, exclude = {RedisRepositoriesAutoConfiguration.class})
@EnableConfigurationProperties({
        AuthProperties.class,
        DefaultConfig.class,
        FileProperties.class,
        ResetPasswordConfig.class,
        VNPayProperties.class,
        PayOSProperties.class,
})
public class HdbhmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HdbhmsApplication.class, args);
    }

}
