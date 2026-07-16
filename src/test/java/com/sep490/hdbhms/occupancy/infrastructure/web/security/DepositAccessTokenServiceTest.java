package com.sep490.hdbhms.occupancy.infrastructure.web.security;

import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.sep490.hdbhms.occupancy.infrastructure.web.security.DepositAccessTokenService.BATCH_SCOPE;
import static com.sep490.hdbhms.occupancy.infrastructure.web.security.DepositAccessTokenService.PAYMENT_SCOPE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DepositAccessTokenServiceTest {
    private static final Instant ISSUED_AT = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void issuedTokenAuthorizesOnlyItsBoundResource() {
        DepositAccessTokenService service = serviceAt(ISSUED_AT);
        String token = service.issue(PAYMENT_SCOPE, 41L);

        assertDoesNotThrow(() -> service.requireValid(token, PAYMENT_SCOPE, 41L));
        assertForbidden(() -> service.requireValid(token, PAYMENT_SCOPE, 42L));
        assertForbidden(() -> service.requireValid(token, BATCH_SCOPE, 41L));
        assertForbidden(() -> service.requireValid(token + "tampered", PAYMENT_SCOPE, 41L));
    }

    @Test
    void missingAndExpiredTokensAreRejected() {
        String token = serviceAt(ISSUED_AT).issue(BATCH_SCOPE, 9L);

        assertForbidden(() -> serviceAt(ISSUED_AT).requireValid(null, BATCH_SCOPE, 9L));
        assertForbidden(() -> serviceAt(ISSUED_AT.plusSeconds(8 * 24 * 60 * 60L))
                .requireValid(token, BATCH_SCOPE, 9L));
    }

    private static DepositAccessTokenService serviceAt(Instant instant) {
        AuthProperties properties = new AuthProperties();
        properties.setTokenSecret("test-secret-that-is-long-enough-for-hs512-and-never-used-outside-tests-123456789");
        return new DepositAccessTokenService(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static void assertForbidden(Runnable call) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, call::run);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}
