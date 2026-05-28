package com.sep490.hdbhms.shared.config;

import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.CreateDefaultOwnerAccountUseCase;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;

@Slf4j
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppInitConfig {
    CreateDefaultOwnerAccountUseCase createDefaultOwnerAccountUseCase;
    @Bean
    ApplicationRunner init() {
        log.info("Initializing application");
        return args -> {
            createDefaultOwnerAccountUseCase.execute();
            log.info("Application initialized");
        };
    }

    @Autowired
    private ApplicationContext ctx;

    @PostConstruct
    public void checkControllers() {
        System.out.println("Controllers: " + ctx.getBeanNamesForAnnotation(Controller.class).length);
    }
}
