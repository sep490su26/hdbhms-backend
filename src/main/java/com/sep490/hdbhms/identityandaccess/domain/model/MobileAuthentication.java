package com.sep490.hdbhms.identityandaccess.domain.model;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;

public record MobileAuthentication(
        String token,
        String sessionId,
        Role role,
        Long tenantId,
        Long propertyId,
        boolean mustChangePassword,
        boolean authorized
) implements Authentication {
}
