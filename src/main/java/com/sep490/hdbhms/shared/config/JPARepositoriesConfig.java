package com.sep490.hdbhms.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.sep490.hdbhms")
public class JPARepositoriesConfig {
}
