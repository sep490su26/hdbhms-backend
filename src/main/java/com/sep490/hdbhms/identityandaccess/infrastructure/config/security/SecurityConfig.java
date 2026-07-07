package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import jakarta.servlet.DispatcherType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
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
    static final String[] PUBLIC_POST_URLS = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/introspect",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/deposit/checkout",
            "/api/v1/public/deposits/batch-checkout",
            "/api/v1/public/deposits/batches/*/cancel",
            "/api/v1/public/deposits/batches/*/expire",
            "/api/v1/deposit/contracts/preview",
            "/api/v1/deposit/payments/*/cancel",
            "/api/v1/deposit/payments/*/expire",
            "/api/v1/visit-requests",
            "/api/v1/webhook/**",
    };

    static final String[] PUBLIC_GET_URLS = {
            "/room-samples/**",
            "/api/v1/rooms",
            "/api/v1/rooms/*",
            "/api/v1/rooms/*/assets",
            "/api/v1/rooms/*/assets/*",
            "/api/v1/rooms/*/meter-readings/latest",
            "/api/v1/deposit/rooms/*/hold-status",
            "/api/v1/deposit/payments/*/status",
            "/api/v1/deposit/payments/*/contract",
            "/api/v1/public/deposits/batches/*/status",
            "/api/v1/public/properties/*/floor-plan",
            "/api/v1/properties",
            "/api/v1/properties/simple",
            "/api/v1/properties/*",
            "/api/v1/properties/*/rules",
            "/api/v1/properties/*/rooms/simple",
            "/api/v1/floors",
            "/api/v1/floors/*",
            "/api/v1/files/download/*",
            "/api/v1/health",
            "/api/v1/webhook/**",
    };

    static final String[] PUBLIC_DOC_URLS = {
            "/docs/**",
            "/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
    };

    @Bean
    @Order(0)
    public SecurityFilterChain depositCheckoutPublicChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(new AntPathRequestMatcher("/api/v1/deposit/checkout"))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .anyRequest().permitAll()
                )
                .build();
    }

    @Bean
    @Order(1)
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
                                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                        .requestMatchers("/error").permitAll()
                                        .requestMatchers(toRequestMatchers(HttpMethod.GET, PUBLIC_GET_URLS)).permitAll()
                                        .requestMatchers(toRequestMatchers(HttpMethod.POST, PUBLIC_POST_URLS)).permitAll()
                                        .requestMatchers(PUBLIC_DOC_URLS).permitAll()
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
        configuration.setAllowedOriginPatterns(List.of(
//                "http://localhost:*",
//                "http://127.0.0.1:*",
//                "http://10.0.2.2:*"
                "*"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-Client-Type"
        ));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static RequestMatcher[] toRequestMatchers(HttpMethod method, String[] patterns) {
        return Arrays.stream(patterns)
                .map(pattern -> new AntPathRequestMatcher(pattern, method.name()))
                .toArray(RequestMatcher[]::new);
    }
}
