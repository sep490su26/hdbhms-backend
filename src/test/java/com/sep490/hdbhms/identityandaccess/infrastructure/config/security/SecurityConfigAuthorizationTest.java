package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig
@WebAppConfiguration
@ContextConfiguration(classes = {
        SecurityConfig.class,
        SecurityConfigAuthorizationTest.TestConfiguration.class
})
class SecurityConfigAuthorizationTest {

    @Autowired
    WebApplicationContext applicationContext;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void anonymousCanUseExplicitGuestAndAuthenticationEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/rooms"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotUseProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanUseProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe/protected")
                        .with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk());
    }

    @Test
    void tenantAndManagerCannotUseOwnerOnlyEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe/owner")
                        .with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/security-probe/owner")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanUseOwnerOnlyEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe/owner")
                        .with(user("owner").roles("OWNER")))
                .andExpect(status().isOk());
    }

    @Test
    void accountantCanUseOnlyTheGrantedFinanceProbe() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe/finance")
                        .with(user("accountant").roles("ACCOUNTANT")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/security-probe/owner")
                        .with(user("accountant").roles("ACCOUNTANT")))
                .andExpect(status().isForbidden());
    }

    @Configuration
    @EnableWebMvc
    static class TestConfiguration {

        @Bean
        TokenAuthenticationFilter tokenAuthenticationFilter() {
            return new TokenAuthenticationFilter(
                    mock(TokenProvider.class),
                    mock(CustomUserDetailsService.class)
            );
        }

        @Bean
        CorsProperties corsProperties() {
            CorsProperties properties = new CorsProperties();
            properties.setAllowedOriginPatterns(List.of("http://localhost:*"));
            return properties;
        }

        @Bean
        SecurityProbeController securityProbeController() {
            return new SecurityProbeController();
        }
    }

    @RestController
    static class SecurityProbeController {

        @GetMapping("/api/v1/rooms")
        String publicRooms() {
            return "rooms";
        }

        @PostMapping("/api/v1/auth/login")
        String publicLogin() {
            return "login";
        }

        @GetMapping("/api/v1/security-probe/protected")
        String protectedEndpoint() {
            return "protected";
        }

        @GetMapping("/api/v1/security-probe/owner")
        @PreAuthorize("hasRole('OWNER')")
        String ownerEndpoint() {
            return "owner";
        }

        @GetMapping("/api/v1/security-probe/finance")
        @PreAuthorize("hasAnyRole('OWNER','MANAGER','ACCOUNTANT')")
        String financeEndpoint() {
            return "finance";
        }
    }
}
