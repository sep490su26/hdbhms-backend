package com.sep490.hdbhms.shared.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpUtils {
    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    public static String getUserAgent(HttpServletRequest request) {
        return request != null ? request.getHeader(HttpHeaders.USER_AGENT) : null;
    }

    /**
     * Returns the client IP address from the request, considering proxy headers.
     *
     * @param request HttpServletRequest
     * @return client IP address as string, or null if not available
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        for (var header : IP_HEADER_CANDIDATES) {
            var ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (header.equals("X-Forwarded-For") && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        return request.getRemoteAddr();
    }
}
