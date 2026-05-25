package com.sep490.hdbhms.shared.utils;

import com.nimbusds.jwt.SignedJWT;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.text.ParseException;
import java.util.Arrays;

import static com.sep490.hdbhms.shared.utils.SessionUtils.ACCESS_TOKEN_COOKIE_NAME;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthUtils {
    private static final String BEARER_PREFIX = "Bearer ";

    public static Long getCurrentAuthenticationId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        return null;
    }

    public static String extractToken() {
        var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        var request = attributes.getRequest();

        // 1. Try Authorization header first
        String header = request.getHeader("Authorization");
        if (!StringUtils.isEmpty(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }

        // 2. Fallback to cookie
        var cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> ACCESS_TOKEN_COOKIE_NAME.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public static Long getIdFromToken() {
        var token = extractToken();
        return getIdFromToken(token);
    }

    public static Long getIdFromToken(String token) {
        if (token == null) {
            return null;
        }
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String sub = signedJWT.getJWTClaimsSet().getSubject();
            return Long.parseLong(sub);
        } catch (ParseException | NumberFormatException e) {
            log.error("Failed to parse JWT token", e);
            return null;
        }
    }
}
