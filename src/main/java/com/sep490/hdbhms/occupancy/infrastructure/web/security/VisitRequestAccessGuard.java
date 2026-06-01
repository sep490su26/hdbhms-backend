package com.sep490.hdbhms.occupancy.infrastructure.web.security;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component("visitRequestAccessGuard")
public class VisitRequestAccessGuard {
    private static final Set<String> MANAGER_AUTHORITIES = Set.of("ROLE_OWNER", "ROLE_MANAGER");

    private final Environment environment;

    public VisitRequestAccessGuard(Environment environment) {
        this.environment = environment;
    }

    public boolean canManage(Authentication authentication) {
        return hasAnyAuthority(authentication, MANAGER_AUTHORITIES) || isLocalDevProfile();
    }

    public boolean canForceDelete(Authentication authentication) {
        return hasAnyAuthority(authentication, Set.of("ROLE_OWNER")) || isLocalDevProfile();
    }

    private boolean hasAnyAuthority(Authentication authentication, Set<String> allowedAuthorities) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(allowedAuthorities::contains);
    }

    private boolean isLocalDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
}
