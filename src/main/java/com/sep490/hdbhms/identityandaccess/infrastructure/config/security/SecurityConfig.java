package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {
    public static final String BASE_OAUTH2_AUTHORIZATION_URI = "/oauth2/authorize";
    public static final String BASE_OAUTH2_CALLBACK_URI = "/oauth2/callback/*";

    TokenAuthenticationFilter tokenAuthenticationFilter;
    String[] publicUrls = {
            "/**",
            "/accounts/**",
            "/balance/**",
            "/stories/**",
            "/resources/download/**",
            "/roles/**",
            "/login",
            "/refresh",
            "/logout",
            "/introspect",
            "/forgot-password",
            "/reset-password",
            "/outbound",
            "/oauth2/**",
            "/internal/**",
            "/docs/**",
            "/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**",
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        authorizeRequests ->
                                authorizeRequests
                                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                        .requestMatchers(publicUrls).permitAll()
                                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
        ;
        http.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:3000", "http://127.0.0.1:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
