package com.sep490.hdbhms.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI hdbhmsOpenApi() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("HDBHMS Mobile Backend API")
                        .version("dev")
                        .description("API documentation for Flutter mobile and web clients."))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Flutter web / Postman local"),
                        new Server()
                                .url("http://10.0.2.2:8080")
                                .description("Android emulator"),
                        new Server()
                                .url("http://<IP_MAY_TINH>:8080")
                                .description("Physical device on the same Wi-Fi")
                ));
    }
}
