package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import com.nimbusds.jose.JOSEException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.CookieUtils;
import com.sep490.hdbhms.shared.utils.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import static com.sep490.hdbhms.shared.utils.SessionUtils.ACCESS_TOKEN_COOKIE_NAME;


@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    TokenProvider tokenProvider;
    CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        if (request.getMethod().equals(HttpMethod.OPTIONS.name())) {
            filterChain.doFilter(request, response);
            return;
        }
        var jwt = getJwtFromRequest(request);
        try {
            if (!StringUtils.isEmpty(jwt)) {
                var signedJWT = tokenProvider.verifyToken(jwt, false);
                var userId = signedJWT.getJWTClaimsSet().getSubject();
                var userDetails = userDetailsService.loadUserById(Long.valueOf(userId));
                var role = (String) signedJWT.getJWTClaimsSet().getClaim("role");

                var authorities = new ArrayList<GrantedAuthority>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (AppException ex) {
            SecurityContextHolder.clearContext();
        } catch (JOSEException | ParseException e) {
            throw new AppException(ApiErrorCode.INVALID_JWT_TOKEN);
        }
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        var bearerToken = request.getHeader("Authorization");
        if (StringUtils.isEmpty(bearerToken)) {
            return getJwtFromCookie(request);
        }
        return StringUtils.getTokenFromAuthorizationHeader(bearerToken);
    }

    private String getJwtFromCookie(HttpServletRequest request) {
        var accessTokenCookie = CookieUtils.getCookie(request, ACCESS_TOKEN_COOKIE_NAME)
                .orElse(null);
        if (accessTokenCookie != null) {
            return accessTokenCookie.getValue();
        }
        return null;
    }
}
