package com.sep490.hdbhms.identityandaccess.domain.model;

public record Authentication(String token, boolean authorized) {
}
