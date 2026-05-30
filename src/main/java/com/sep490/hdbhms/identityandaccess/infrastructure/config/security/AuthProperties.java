package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.auth")
@Validated
public class AuthProperties {
    @NotBlank(message = "app.auth.token-secret must not be blank")
    @Size(min = 64, message = "app.auth.token-secret must be at least 64 characters for HS512")
    String tokenSecret;
    long tokenExpirationSec;
    long tokenRefreshSec;
    OAuth2 oauth2 = new OAuth2();

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OAuth2 {
        String defaultRedirectUri;
        List<String> authorizedRedirectUris = new ArrayList<>();
    }
}
