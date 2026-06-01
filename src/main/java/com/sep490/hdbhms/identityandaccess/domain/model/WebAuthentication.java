package com.sep490.hdbhms.identityandaccess.domain.model;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;

public record WebAuthentication(String token, Role role, boolean authorized) implements Authentication {
}
