package com.sep490.hdbhms.shared.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FanOutJobConfig {
//    JobRepository jobRepository;
//    PlatformTransactionManager transactionManager;
//
//    @Bean
//    public Job fanOutJob() {
//        return new JobBuilder("fanOutJob", jobRepository)
//                .start(fanOutStep())
//                .build();
//    }

//    @Bean
//    public Step fanOutStep() {
//        return new StepBuilder("fanOutStep", jobRepository)
//                .<Long, >
//    }
}
