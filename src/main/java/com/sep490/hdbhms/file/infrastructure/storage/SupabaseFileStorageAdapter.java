package com.sep490.hdbhms.file.infrastructure.storage;

import com.sep490.hdbhms.file.application.port.out.FileStoragePort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnProperty(prefix = "app.file.storage", name = "provider", havingValue = "supabase")
public class SupabaseFileStorageAdapter implements FileStoragePort {
    HttpClient httpClient;
    SupabaseStorageConfiguration.SupabaseStorageSettings settings;

    @Override
    public String put(String storageKey, InputStream content, long contentLength, String contentType) throws IOException {
        HttpRequest.Builder requestBuilder = authorizedRequest(objectUrl(storageKey))
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> content));
        if (contentType != null && !contentType.isBlank()) {
            requestBuilder.header("Content-Type", contentType);
        }

        HttpResponse<String> response = sendText(requestBuilder.build());
        requireSuccess("put", storageKey, response);
        return storageKey;
    }

    @Override
    public byte[] get(String storageKey) throws IOException {
        HttpResponse<byte[]> response = sendBytes(authorizedRequest(objectUrl(storageKey))
                .GET()
                .build());
        requireSuccess("get", storageKey, response);
        return response.body();
    }

    @Override
    public void delete(String storageKey) throws IOException {
        String body = "{\"prefixes\":[\"" + jsonEscape(storageKey) + "\"]}";
        HttpResponse<String> response = sendText(authorizedRequest(removeUrl())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build());
        requireSuccess("delete", storageKey, response);
    }

    private HttpRequest.Builder authorizedRequest(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + settings.serviceRoleKey())
                .header("apikey", settings.serviceRoleKey());
    }

    private String objectUrl(String storageKey) {
        return baseUrl() + "/storage/v1/object/"
                + encodePathSegment(settings.bucket()) + "/" + encodePath(storageKey);
    }

    private String removeUrl() {
        return baseUrl() + "/storage/v1/object/"
                + encodePathSegment(settings.bucket()) + "/remove";
    }

    private String baseUrl() {
        return settings.url().replaceAll("/+$", "");
    }

    private static String encodePath(String path) {
        String[] segments = path.split("/", -1);
        StringBuilder encoded = new StringBuilder();
        for (String segment : segments) {
            if (!encoded.isEmpty()) {
                encoded.append('/');
            }
            encoded.append(encodePathSegment(segment));
        }
        return encoded.toString();
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private HttpResponse<String> sendText(HttpRequest request) throws IOException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Supabase storage request was interrupted", exception);
        }
    }

    private HttpResponse<byte[]> sendBytes(HttpRequest request) throws IOException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Supabase storage request was interrupted", exception);
        }
    }

    private void requireSuccess(String operation, String storageKey, HttpResponse<?> response) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unable to " + operation + " Supabase object " + storageKey
                    + " (HTTP " + response.statusCode() + "): " + response.body());
        }
    }
}
