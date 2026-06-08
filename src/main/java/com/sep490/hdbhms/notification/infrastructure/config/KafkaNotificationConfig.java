package com.sep490.hdbhms.notification.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaNotificationConfig {
    @Bean
    public NewTopic leaseContractConfirmed() {
        return TopicBuilder.name("lease.confirmed")
                .build();
    }
}
