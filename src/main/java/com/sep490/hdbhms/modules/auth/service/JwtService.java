//package com.sep490.hdbhms.modules.auth.service;
//
//import com.sep490.hdbhms.modules.auth.dto.LoginResponse;
//import com.sep490.hdbhms.modules.user.entity.User;
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.time.Duration;
//import java.time.Instant;
//import java.util.List;
//import javax.crypto.SecretKey;
//import javax.crypto.spec.SecretKeySpec;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
//import org.springframework.security.oauth2.jwt.JwsHeader;
//import org.springframework.security.oauth2.jwt.JwtClaimsSet;
//import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
//import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//import com.nimbusds.jose.jwk.source.ImmutableSecret;
//
//@Service
//public class JwtService {
//
//    private final String secret;
//    private final Duration accessTokenExpiration;
//    private final Duration refreshTokenExpiration;
//
//    public JwtService(
//            @Value("${app.jwt.secret:${app.auth.token-secret:}}") String secret,
//            @Value("${app.jwt.access-token-expiration-minutes:15}") long accessTokenExpirationMinutes,
//            @Value("${app.jwt.refresh-token-expiration-days:7}") long refreshTokenExpirationDays
//    ) {
//        this.secret = secret;
//        this.accessTokenExpiration = Duration.ofMinutes(accessTokenExpirationMinutes);
//        this.refreshTokenExpiration = Duration.ofDays(refreshTokenExpirationDays);
//    }
//
//    public String createAccessToken(User user, List<LoginResponse.TenantInfo> tenants) {
//        Instant now = Instant.now();
//        JwtClaimsSet claims = JwtClaimsSet.builder()
//                .issuer("hdbhms")
//                .issuedAt(now)
//                .expiresAt(now.plus(accessTokenExpiration))
//                .subject(String.valueOf(user.getId()))
//                .claim("token_type", "access")
//                .claim("user_id", user.getId())
//                .claim("status", user.getStatus().name())
//                .claim("roles", tenants.stream().map(LoginResponse.TenantInfo::role).distinct().toList())
//                .build();
//        return encode(claims);
//    }
//
//    public String createRefreshToken(User user) {
//        Instant now = Instant.now();
//        JwtClaimsSet claims = JwtClaimsSet.builder()
//                .issuer("hdbhms")
//                .issuedAt(now)
//                .expiresAt(now.plus(refreshTokenExpiration))
//                .subject(String.valueOf(user.getId()))
//                .claim("token_type", "refresh")
//                .claim("user_id", user.getId())
//                .build();
//        return encode(claims);
//    }
//
//    public long getAccessTokenExpiresInSeconds() {
//        return accessTokenExpiration.toSeconds();
//    }
//
//    private String encode(JwtClaimsSet claims) {
//        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey()));
//        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
//        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
//    }
//
//    private SecretKey jwtSecretKey() {
//        if (!StringUtils.hasText(secret)) {
//            throw new IllegalStateException("app.jwt.secret is required");
//        }
//        try {
//            byte[] digest = MessageDigest.getInstance("SHA-256")
//                    .digest(secret.getBytes(StandardCharsets.UTF_8));
//            return new SecretKeySpec(digest, "HmacSHA256");
//        } catch (NoSuchAlgorithmException ex) {
//            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
//        }
//    }
//}
