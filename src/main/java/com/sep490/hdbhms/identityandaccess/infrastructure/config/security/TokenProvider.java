package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.InvalidatedTokenEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaInvalidatedTokenRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.CookieUtils;
import com.sep490.hdbhms.shared.utils.StringUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.sep490.hdbhms.shared.utils.SessionUtils.*;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenProvider {
    AuthProperties authProperties;
    JpaInvalidatedTokenRepository invalidatedTokenRepository;
    RedisTemplate<String, Object> redisTemplate;

    static String refreshTokenKeyFormat = "refresh:sessionId:%s";
    static String tokenKey = "sessionId";
    static String userIdKey = "user-id";

    public String createAccessToken(UserPrincipal userPrincipal, String sessionId, HttpServletResponse response) {
        var key = String.format(refreshTokenKeyFormat, sessionId);

        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        var refreshToken = (String) redisTemplate.opsForHash().get(key, tokenKey);
        try {
            verifyToken(refreshToken, true);
        } catch (JOSEException | ParseException e) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        var storedUserId = (String) redisTemplate.opsForHash().get(key, userIdKey);
        if (!String.valueOf(userPrincipal.getId()).equals(storedUserId)) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }

        Date now = new Date();
        Date expiryDate = new Date(now.toInstant()
                .plus(
                        authProperties.getTokenExpirationSec(),
                        ChronoUnit.SECONDS
                ).toEpochMilli()
        );

        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet;

        jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userPrincipal.getName()))
                .issuer("hdbhms")
                .claim("role", userPrincipal.getRole())
                .issueTime(now)
                .expirationTime(expiryDate)
                .jwtID(UUID.randomUUID().toString())
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        try {
            jwsObject.sign(new MACSigner(secretBytes()));
            var accessToken = jwsObject.serialize();
            var expiryMillis = expiryDate.toInstant().toEpochMilli();
            var ttl = expiryMillis - now.toInstant().toEpochMilli();
            CookieUtils.addCookie(response, ACCESS_TOKEN_COOKIE_NAME, accessToken, Math.toIntExact(ttl / 1000));
            return accessToken;
        } catch (JOSEException e) {
            log.error("Can not create sessionId", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new refresh sessionId for the specified authenticated user.
     *
     * <p>This method generates a signed JWT refresh sessionId containing a unique
     * session ID and user ID. The refresh sessionId metadata is stored in Redis
     * for later validation and management. A cookie containing the session ID
     * is also added to the HTTP response, allowing the server to identify
     * the user's session in subsequent requests.</p>
     *
     * <p>The refresh sessionId has a longer lifespan than an access sessionId and can
     * be used to obtain new access tokens without requiring the user to log in again.
     * The session information and refresh sessionId are stored in Redis with an expiry
     * time matching the sessionId's lifetime.</p>
     *
     * @param userPrincipal the authenticated user's details used to populate the sessionId claims
     * @param response      the HTTP servlet response where the session ID cookie will be added
     * @return the generated session ID associated with the refresh sessionId
     * @throws RuntimeException if signing the JWT fails
     * @implNote <ul>
     * <li>The refresh sessionId and related metadata are stored in Redis as a hash under the key
     * {@code refresh:sessionId:{sessionId}}.</li>
     * <li>The session ID is also stored in a Redis set under {@code device_sessions:{userId}:{deviceId}}
     * to allow tracking multiple concurrent sessions per user.</li>
     * <li>The TTL in Redis is set to the exact expiration time of the refresh sessionId.</li>
     * <li>A cookie named {@code SESSION_ID_COOKIE_NAME} is created to persist the session ID
     * on the client side.</li>
     * </ul>
     */
    public String createRefreshToken(UserPrincipal userPrincipal, HttpServletRequest request, HttpServletResponse response) {
        var deviceId = getOrCreateDeviceId(request, response);
        var userId = String.valueOf(userPrincipal.getId());

        var previousSessions = redisTemplate.opsForZSet().range(
                String.format("device_sessions:%s:%s", userId, deviceId),
                0,
                -1);
        if (previousSessions != null && !previousSessions.isEmpty()) {
            previousSessions.forEach(s ->
                    clearSessionBySessionId(String.valueOf(s), userId, deviceId)
            );
        }

        Date now = new Date();
        Date expiryDate = new Date(now.toInstant()
                .plus(
                        authProperties.getTokenRefreshSec(),
                        ChronoUnit.SECONDS
                ).toEpochMilli()
        );
        var sessionId = UUID.randomUUID().toString();

        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        var jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issuer("hdbhms")
                .claim("session-id", sessionId)
                .issueTime(now)
                .jwtID(UUID.randomUUID().toString())
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        try {
            jwsObject.sign(new MACSigner(secretBytes()));
            var token = jwsObject.serialize();

            var key = String.format(refreshTokenKeyFormat, sessionId);
            redisTemplate.opsForHash().put(key, userIdKey, userId);
            redisTemplate.opsForHash().put(key, tokenKey, token);
            redisTemplate.opsForHash().put(key, "device-id", deviceId);
            redisTemplate.opsForHash().put(key, "issued-at", String.valueOf(now));
            redisTemplate.opsForHash().put(key, "expiry-at", String.valueOf(expiryDate));
            redisTemplate.opsForHash().put(key, "last-used-at", String.valueOf(now));
            var expiryMillis = expiryDate.toInstant().toEpochMilli();
            var ttl = expiryMillis - now.toInstant().toEpochMilli();
            redisTemplate.expire(key, ttl, TimeUnit.MILLISECONDS);
            redisTemplate.opsForZSet().add(String.format("device_sessions:%s:%s", userId, deviceId), sessionId, expiryMillis);

            //This sounds dumb, be aware if this causes unexpected results
            CookieUtils.addCookie(response, SESSION_ID_COOKIE_NAME, sessionId, Math.toIntExact(ttl / 1000));
            return sessionId;
        } catch (Exception e) {
            log.error("Can not create sessionId", e);
            throw new RuntimeException(e);
        }
    }

    public String createAccessToken(User user, String sessionId, HttpServletResponse response) {
        return createAccessToken(UserPrincipal.createFromBasicUser(user), sessionId, response);
    }

    public String createRefreshToken(User user, HttpServletRequest request, HttpServletResponse response) {
        return createRefreshToken(UserPrincipal.createFromBasicUser(user), request, response);
    }

    public String getRefreshToken(HttpServletRequest request, boolean updateLastUsedAt) {
        CookieUtils.getCookies(request).ifPresent(value -> Arrays.stream(value)
                .forEach(cookie -> log.info("Cookie: {}-{}", cookie.getName(), cookie.getValue())));
        var sessionCookie = CookieUtils.getCookie(request, SESSION_ID_COOKIE_NAME)
                .map(Cookie::getValue);
        if (sessionCookie.isEmpty()) {
            return null;
        }
        var key = String.format(refreshTokenKeyFormat, sessionCookie.get());
        var refreshToken = (String) redisTemplate.opsForHash().get(key, tokenKey);
        if (!StringUtils.isEmpty(refreshToken) && updateLastUsedAt) {
            redisTemplate.opsForHash().put(key, "last-used-at", String.valueOf(new Date()));
        }
        return refreshToken;
    }

    public String getRefreshToken(HttpServletRequest request) {
        return getRefreshToken(request, false);
    }

    public String getAccessTokenFromCookie(HttpServletRequest request) {
        return CookieUtils.getCookie(request, ACCESS_TOKEN_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(null);
    }

    public String getSessionIdFromCookie(HttpServletRequest request) {
        return CookieUtils.getCookie(request, SESSION_ID_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(null);
    }

    @SneakyThrows
    public Long getUserId(HttpServletRequest request) {
        var sessionId = getSessionIdFromCookie(request);
        if (StringUtils.isEmpty(sessionId)) {
            return null;
        }
        var key = String.format(refreshTokenKeyFormat, sessionId);
        return Long.parseLong((String) Objects.requireNonNull(redisTemplate.opsForHash().get(key, "user-id")));
    }

    public SignedJWT verifyToken(String token, boolean isRefreshToken) throws JOSEException, ParseException {
        var jwsVerifier = new MACVerifier(secretBytes());
        if (StringUtils.isEmpty(token)) {
            throw new AppException(ApiErrorCode.INVALID_JWT_TOKEN);
        }
        var signedJWT = SignedJWT.parse(token);
        var expiryTime = (isRefreshToken) ? new Date(
                signedJWT.getJWTClaimsSet().getIssueTime().toInstant()
                        .plus(authProperties.getTokenRefreshSec(), ChronoUnit.SECONDS)
                        .toEpochMilli()
        ) : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(jwsVerifier);
        if (!verified || !expiryTime.after(new Date())) {
            log.info("Token: {} expired", token);
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
            log.info("Token: {} is found in invalidated", token);
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        return signedJWT;
    }

    public void clearToken(String token, boolean isRefreshToken) {
        if (StringUtils.isEmpty(token)) {
            return;
        }
        SignedJWT signedJWT;
        try {
            signedJWT = verifyToken(token, isRefreshToken);
            var jit = signedJWT.getJWTClaimsSet().getJWTID();
            var expiryTime = (isRefreshToken) ? new Date(
                    signedJWT.getJWTClaimsSet().getIssueTime().toInstant()
                            .plus(authProperties.getTokenRefreshSec(), ChronoUnit.SECONDS)
                            .toEpochMilli()
            ) : signedJWT.getJWTClaimsSet().getExpirationTime();
            var invalidatedToken = InvalidatedTokenEntity.builder()
                    .id(jit)
                    .expiryTime(expiryTime)
                    .build();
            invalidatedTokenRepository.save(invalidatedToken);
            log.info("Moved sessionId: {} with id: {} to the invalidated sessionId repository", token, invalidatedToken.getId());
        } catch (JOSEException | ParseException | AppException e) {
            log.error(e.getMessage());
        }
    }

    private void clearSessionBySessionId(String sessionId, String userId, String deviceId) {
        String key = String.format(refreshTokenKeyFormat, sessionId);
        log.info(key);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.delete(key);
            log.info("Cleared session data from Redis for sessionId={}", sessionId);
        }

        redisTemplate.opsForZSet().remove(String.format("device_sessions:%s:%s", userId, deviceId), sessionId);
    }

    public void clearSession(HttpServletRequest request, HttpServletResponse response) {
        var sessionCookie = CookieUtils.getCookie(request, SESSION_ID_COOKIE_NAME);
        if (sessionCookie.isPresent()) {
            String sessionId = sessionCookie.get().getValue();

            String key = String.format(refreshTokenKeyFormat, sessionId);
            String userId = (String) redisTemplate.opsForHash().get(key, userIdKey);
            String deviceId = (String) redisTemplate.opsForHash().get(key, "device-id");

            if (!StringUtils.isEmpty(userId) && !StringUtils.isEmpty(deviceId)) {
                clearSessionBySessionId(sessionId, userId, deviceId);
            } else {
                // Fallback: just delete the key if we can't get the ids
                if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                    redisTemplate.delete(key);
                    log.info("Cleared session data from Redis for sessionId={} (without userId/deviceId)", sessionId);
                }
            }
        }
        CookieUtils.deleteCookie(request, response, SESSION_ID_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, ACCESS_TOKEN_COOKIE_NAME);
    }

    public void clearAllUserSessions(Long userId, HttpServletRequest request, HttpServletResponse response) {
        var deviceId = getOrCreateDeviceId(request, response);
        var userSessionsKey = String.format("device_sessions:%s:%s", userId, deviceId);
        var sessionIds = redisTemplate.opsForZSet().range(userSessionsKey, 0, -1);
        if (sessionIds != null && !sessionIds.isEmpty()) {
            sessionIds.forEach(sessionId -> {
                var key = String.format(refreshTokenKeyFormat, sessionId.toString());
                redisTemplate.delete(key);
                redisTemplate.opsForZSet().remove(userSessionsKey, sessionId);
            });
        }
    }

    public void clearAllUserSessionsExceptCurrent(Long userId, HttpServletRequest request, HttpServletResponse response) {
        var sessionCookie = CookieUtils.getCookie(request, SESSION_ID_COOKIE_NAME);
        if (sessionCookie.isPresent()) {
            String currentSessionId = sessionCookie.get().getValue();
            var deviceId = getOrCreateDeviceId(request, response);
            var userSessionsKey = String.format("device_sessions:%s:%s", userId, deviceId);
            var sessionIds = redisTemplate.opsForZSet().range(userSessionsKey, 0, -1);
            if (sessionIds != null && !sessionIds.isEmpty()) {
                sessionIds.forEach(sessionId -> {
                    if (!currentSessionId.equals(sessionId)) {
                        var key = String.format(refreshTokenKeyFormat, sessionId.toString());
                        redisTemplate.delete(key);
                        redisTemplate.opsForZSet().remove(userSessionsKey, sessionId);
                    }
                });
            }
        } else {
            clearAllUserSessions(userId, request, response);
        }
    }

    public boolean hasSession(HttpServletRequest request) {
        return CookieUtils.getCookie(request, SESSION_ID_COOKIE_NAME).isPresent();
    }

    private byte[] secretBytes() {
        return authProperties.getTokenSecret().getBytes(StandardCharsets.UTF_8);
    }
}
