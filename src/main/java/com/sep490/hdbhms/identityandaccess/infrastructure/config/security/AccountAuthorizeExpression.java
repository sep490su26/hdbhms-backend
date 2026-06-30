package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.AccountStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component("accountAuthorize")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountAuthorizeExpression {
    public boolean isInStatus(AccountStatus status) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        var principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getAccountStatus().equals(status);
        }
        return false;
    }

    public boolean isVerified() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        var principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getEmailVerified();
        }
        return false;
    }
}
