package com.sep490.hdbhms.identityandaccess.domain.model;

public record MobileAuthentication(String token, String sessionId, boolean authorized) implements Authentication {
}
