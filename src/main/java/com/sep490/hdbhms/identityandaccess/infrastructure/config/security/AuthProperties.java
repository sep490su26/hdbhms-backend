package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
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