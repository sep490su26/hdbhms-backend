package com.sep490.hdbhms.identityandaccess.domain.model;

public record WebAuthentication(String token, boolean authorized) implements Authentication {
}
