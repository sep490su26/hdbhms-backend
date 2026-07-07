package com.sep490.hdbhms.identityandaccess.domain.model;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;

public record WebAuthentication(String token, Role role, boolean mustChangePassword, boolean authorized) implements Authentication {
}
