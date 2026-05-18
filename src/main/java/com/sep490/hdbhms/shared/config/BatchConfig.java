package com.sep490.hdbhms.shared.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
public class BatchConfig {
//    @Bean
//    public PlatformTransactionManager resourcelessTransactionManager() {
//        return new ResourcelessTransactionManager();
//    }
}
