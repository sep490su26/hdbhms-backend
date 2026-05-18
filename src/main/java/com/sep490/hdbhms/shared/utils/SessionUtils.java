package com.sep490.hdbhms.shared.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SessionUtils {
    public static final String SESSION_ID_COOKIE_NAME = "JSESSIONID";
    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String DEVICE_ID_COOKIE_NAME = "device_id";

    public static String getOrCreateDeviceId(HttpServletRequest request, HttpServletResponse response) {
        return CookieUtils.getCookie(request, DEVICE_ID_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElseGet(() -> {
                    String deviceId = UUID.randomUUID().toString();
                    CookieUtils.addCookie(response, DEVICE_ID_COOKIE_NAME, deviceId, 60 * 60 * 24 * 730);
                    return deviceId;
                });
    }
}
