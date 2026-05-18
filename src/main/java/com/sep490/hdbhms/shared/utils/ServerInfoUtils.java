package com.sep490.hdbhms.shared.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerInfoUtils implements ApplicationListener<WebServerInitializedEvent> {

    private final Environment environment;

    //    @Value("${server.port}")
    private int actualPort;          // set when the embedded server starts (for random port case)
    private String cachedBaseUrl;

    // ------------------------------------------------------------------------
    //  Public API
    // ------------------------------------------------------------------------

    /**
     * Returns the base URL of the application, e.g. "http://localhost:8080/api"
     * Uses the best available information (from request if passed, otherwise from environment/network).
     */
    public String getBaseUrl() {
        if (cachedBaseUrl != null) {
            return cachedBaseUrl;
        }
        String scheme = "http";   // you can make this configurable if you have HTTPS
        String host = getDisplayIp();
        int port = getPort();
        String contextPath = getContextPath();
        String baseUrl = scheme + "://" + host + ":" + port + contextPath;
        log.info("?   {}", baseUrl);
        cachedBaseUrl = baseUrl;
        return baseUrl;
    }

    /**
     * Returns the base URL using the current HTTP request (most accurate).
     */
    public String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();        // could be "localhost", an IP, or a hostname
        int port = request.getServerPort();
        String contextPath = request.getContextPath();
        return scheme + "://" + host + ":" + port + contextPath;
    }

    /**
     * Returns the server IP, replacing loopback addresses with "localhost".
     */
    public String getDisplayIp() {
        String ip = getActualIp();
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.startsWith("localhost")) {
            return "localhost";
        }
        return "localhost";
    }

    /**
     * Returns the server port. Works even if server.port=0 (random port).
     */
    public int getPort() {
        if (actualPort != 0) {
            return actualPort;
        }
        if (environment != null) {
            return environment.getProperty("server.port", Integer.class, 8080);
        }
        return 8080;
    }

    /**
     * Returns the application context path (e.g., "/api"), or an empty string if none.
     */
    public String getContextPath() {
        log.info("what {}", environment.toString());
        if (environment != null) {
            return environment.getProperty("server.servlet.context-path", "");
        }
        return "";
    }

    // ------------------------------------------------------------------------
    //  Internal helpers
    // ------------------------------------------------------------------------

    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent event) {
        // Capture the actual bound port when the server starts
        this.actualPort = event.getWebServer().getPort();
        this.cachedBaseUrl = null;   // invalidate cache
        log.info("{} - {}", actualPort, getBaseUrl());
    }

    private String getActualIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // fall through
        }
        return "127.0.0.1";
    }
}