package com.sep490.hdbhms.occupancy.infrastructure.web.security;

import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.AuthProperties;
import com.sep490.hdbhms.shared.utils.HashUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class DepositAccessTokenService {
    public static final String HEADER_NAME = "X-Deposit-Access-Token";
    public static final String PAYMENT_SCOPE = "PAYMENT";
    public static final String BATCH_SCOPE = "BATCH";

    private static final String SIGNING_CONTEXT = "deposit-capability:v1";
    private static final Duration TOKEN_TTL = Duration.ofDays(7);

    private final AuthProperties authProperties;
    private final Clock clock;

    public DepositAccessTokenService(AuthProperties authProperties) {
        this(authProperties, Clock.systemUTC());
    }

    DepositAccessTokenService(AuthProperties authProperties, Clock clock) {
        this.authProperties = authProperties;
        this.clock = clock;
    }

    public String issue(String scope, Long resourceId) {
        requireResource(scope, resourceId);
        long expiresAt = Instant.now(clock).plus(TOKEN_TTL).getEpochSecond();
        return expiresAt + "." + sign(scope, resourceId, expiresAt);
    }

    public void requireValid(String token, String scope, Long resourceId) {
        requireResource(scope, resourceId);
        if (token == null || token.isBlank()) {
            throw forbidden();
        }

        String[] parts = token.trim().split("\\.", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw forbidden();
        }

        long expiresAt;
        try {
            expiresAt = Long.parseLong(parts[0]);
        } catch (NumberFormatException exception) {
            throw forbidden();
        }
        if (Instant.now(clock).getEpochSecond() >= expiresAt) {
            throw forbidden();
        }

        byte[] expected = sign(scope, resourceId, expiresAt).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = parts[1].getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw forbidden();
        }
    }

    private String sign(String scope, Long resourceId, long expiresAt) {
        String payload = String.join(":", SIGNING_CONTEXT, scope, resourceId.toString(), String.valueOf(expiresAt));
        return HashUtils.hmacSHA512(authProperties.getTokenSecret(), payload);
    }

    private static void requireResource(String scope, Long resourceId) {
        if ((!PAYMENT_SCOPE.equals(scope) && !BATCH_SCOPE.equals(scope)) || resourceId == null || resourceId <= 0) {
            throw new IllegalArgumentException("Unsupported deposit capability resource");
        }
    }

    private static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Deposit access token is missing or invalid.");
    }
}
