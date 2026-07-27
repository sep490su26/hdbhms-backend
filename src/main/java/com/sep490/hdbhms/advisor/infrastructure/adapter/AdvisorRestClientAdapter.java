package com.sep490.hdbhms.advisor.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.sep490.hdbhms.advisor.application.port.in.command.AdvisorAskCommand;
import com.sep490.hdbhms.advisor.application.port.out.AdvisorClientPort;
import com.sep490.hdbhms.advisor.application.port.out.AdvisorFile;
import com.sep490.hdbhms.advisor.infrastructure.config.AdvisorProperties;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdvisorRestClientAdapter implements AdvisorClientPort {
    AdvisorProperties properties;
    RestClient.Builder builder;

    @NonFinal
    RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeoutMs());
        requestFactory.setReadTimeout(properties.getTimeoutMs());
        this.restClient = builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public JsonNode getKpiOverview(Long landlordId, String period) {
        return get("/kpi/overview", landlordId, period);
    }

    @Override
    public JsonNode getSuggestions(Long landlordId, String period) {
        return get("/copilot/suggestions", landlordId, period);
    }

    @Override
    public JsonNode getAnalysis(Long landlordId, String period) {
        return get("/copilot/analysis", landlordId, period);
    }

    @Override
    public JsonNode createSession(Long landlordId) {
        return post("/copilot/session", landlordId, null, null);
    }

    @Override
    public JsonNode getSessionHistory(Long landlordId, String sessionId) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/copilot/session/{sessionId}")
                            .queryParam("landlord_id", landlordId)
                            .build(sessionId))
                    .retrieve()
                    .body(JsonNode.class);
            return unwrap(response);
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public JsonNode ask(Long landlordId, String period, AdvisorAskCommand command) {
        return post("/copilot/ask", landlordId, period, command);
    }

    @Override
    public JsonNode generateReport(Long landlordId, String period) {
        return post("/copilot/report", landlordId, period, null);
    }

    @Override
    public JsonNode refreshReport(Long landlordId, String period) {
        return post("/copilot/report/refresh", landlordId, period, null);
    }

    @Override
    public AdvisorFile exportReportDocx(Long landlordId, String period) {
        try {
            ResponseEntity<byte[]> response = restClient.get()
                    .uri(uriBuilder -> addPeriod(
                            uriBuilder.path("/copilot/report/export-docx")
                                    .queryParam("landlord_id", landlordId),
                            period
                    ).build())
                    .retrieve()
                    .toEntity(byte[].class);
            return new AdvisorFile(
                    response.getBody(),
                    response.getHeaders().getFirst(org.springframework.http.HttpHeaders.CONTENT_TYPE),
                    response.getHeaders().getFirst(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION)
            );
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    private JsonNode get(String path, Long landlordId, String period) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> addPeriod(
                            uriBuilder.path(path).queryParam("landlord_id", landlordId),
                            period
                    ).build())
                    .retrieve()
                    .body(JsonNode.class);
            return unwrap(response);
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    private JsonNode post(String path, Long landlordId, String period, Object body) {
        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(uriBuilder -> addPeriod(
                            uriBuilder.path(path).queryParam("landlord_id", landlordId),
                            period
                    ).build());
            JsonNode response = (body == null ? request.retrieve() : request.body(body).retrieve())
                    .body(JsonNode.class);
            return unwrap(response);
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    private org.springframework.web.util.UriBuilder addPeriod(
            org.springframework.web.util.UriBuilder uriBuilder,
            String period
    ) {
        if (period != null && !period.isBlank()) {
            uriBuilder.queryParam("period", period);
        }
        return uriBuilder;
    }

    private JsonNode unwrap(JsonNode response) {
        if (response != null && response.has("data")) {
            return response.get("data");
        }
        return response;
    }

    private ResponseStatusException unavailable(Exception ex) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI advisor service is unavailable",
                ex
        );
    }
}
