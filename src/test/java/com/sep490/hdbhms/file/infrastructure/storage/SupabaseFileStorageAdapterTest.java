package com.sep490.hdbhms.file.infrastructure.storage;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupabaseFileStorageAdapterTest {
    private HttpServer server;
    private List<RequestCapture> requests;

    @BeforeEach
    void setUp() throws IOException {
        requests = new ArrayList<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/storage/v1/object/files", this::handleStorageRequest);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void usesSupabaseStorageRestApiForPutGetAndDelete() throws Exception {
        SupabaseFileStorageAdapter adapter = new SupabaseFileStorageAdapter(
                HttpClient.newHttpClient(),
                new SupabaseStorageConfiguration.SupabaseStorageSettings(
                        "http://localhost:" + server.getAddress().getPort(),
                        "files",
                        "service-role-key"
                )
        );

        String key = "files/file-id.txt";
        assertEquals(key, adapter.put(key, new java.io.ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), 5, "text/plain"));
        assertArrayEquals("stored".getBytes(StandardCharsets.UTF_8), adapter.get(key));
        adapter.delete(key);

        assertEquals(3, requests.size());
        assertEquals("POST", requests.get(0).method());
        assertEquals("/storage/v1/object/files/files/file-id.txt", requests.get(0).path());
        assertEquals("hello", requests.get(0).body());
        assertEquals("GET", requests.get(1).method());
        assertEquals("POST", requests.get(2).method());
        assertTrue(requests.get(2).body().contains("files/file-id.txt"));
        assertEquals("Bearer service-role-key", requests.get(0).authorization());
    }

    private void handleStorageRequest(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        requests.add(new RequestCapture(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                new String(body, StandardCharsets.UTF_8),
                exchange.getRequestHeaders().getFirst("Authorization")
        ));

        byte[] response = "GET".equals(exchange.getRequestMethod())
                ? "stored".getBytes(StandardCharsets.UTF_8)
                : "{}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        try (var outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    private record RequestCapture(String method, String path, String body, String authorization) {
    }
}
