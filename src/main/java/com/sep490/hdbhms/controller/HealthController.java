//package com.sep490.hdbhms.controller;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.nio.charset.StandardCharsets;
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//import javax.sql.DataSource;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/v1")
//@Tag(name = "Health", description = "Backend dependency checks for mobile clients")
//public class HealthController {
//
//    private static final byte[] REDIS_PING = "*1\r\n$4\r\nPING\r\n".getBytes(StandardCharsets.US_ASCII);
//
//    private final DataSource dataSource;
//    private final String redisHost;
//    private final int redisPort;
//
//    public HealthController(
//            DataSource dataSource,
//            @Value("${spring.data.redis.host}") String redisHost,
//            @Value("${spring.data.redis.port}") int redisPort
//    ) {
//        this.dataSource = dataSource;
//        this.redisHost = redisHost;
//        this.redisPort = redisPort;
//    }
//
//    @GetMapping("/health")
//    @Operation(summary = "Check backend, database, and Redis status")
//    public Map<String, String> health() {
//        String databaseStatus = isDatabaseUp() ? "UP" : "DOWN";
////        String redisStatus = isRedisUp() ? "UP" : "DOWN";
////        String status = "UP".equals(databaseStatus) && "UP".equals(redisStatus) ? "UP" : "DOWN";
//
//        Map<String, String> response = new LinkedHashMap<>();
////        response.put("status", status);
//        response.put("database", databaseStatus);
////        response.put("redis", redisStatus);
//        return response;
//    }
//
//    private boolean isDatabaseUp() {
//        try (Connection connection = dataSource.getConnection()) {
//            return connection.isValid(2);
//        } catch (SQLException ex) {
//            return false;
//        }
//    }
//
////    private boolean isRedisUp() {
////        try (Socket socket = new Socket()) {
////            socket.connect(new InetSocketAddress(redisHost, redisPort), 2000);
////            socket.setSoTimeout(2000);
////
////            OutputStream outputStream = socket.getOutputStream();
////            outputStream.write(REDIS_PING);
////            outputStream.flush();
////
////            InputStream inputStream = socket.getInputStream();
////            byte[] response = inputStream.readNBytes(7);
////            return new String(response, StandardCharsets.US_ASCII).startsWith("+PONG");
////        } catch (IOException ex) {
////            return false;
////        }
////    }
//}
