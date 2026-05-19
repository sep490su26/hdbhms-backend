package com.sep490.hdbhms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(
        scanBasePackages = {"com.sep490.hdbhms"},
        exclude = {RedisRepositoriesAutoConfiguration.class}
)
public class HdbhmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HdbhmsApplication.class, args);
    }

}
