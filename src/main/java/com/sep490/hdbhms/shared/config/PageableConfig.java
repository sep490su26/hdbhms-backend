package com.sep490.hdbhms.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PageableConfig {
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer customizer() {
        return pageableResolver -> pageableResolver.setOneIndexedParameters(true);
    }
}
