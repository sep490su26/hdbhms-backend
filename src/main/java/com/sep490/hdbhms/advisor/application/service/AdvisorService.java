package com.sep490.hdbhms.advisor.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sep490.hdbhms.advisor.application.port.in.command.AdvisorAskCommand;
import com.sep490.hdbhms.advisor.application.port.out.AdvisorFile;
import com.sep490.hdbhms.advisor.application.port.out.AdvisorClientPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdvisorService {
    AdvisorClientPort advisorClientPort;

    public JsonNode getKpiOverview(Long ownerId, String period) {
        return advisorClientPort.getKpiOverview(ownerId, period);
    }

    public JsonNode getSuggestions(Long ownerId, String period) {
        return advisorClientPort.getSuggestions(ownerId, period);
    }

    public JsonNode getAnalysis(Long ownerId, String period) {
        return advisorClientPort.getAnalysis(ownerId, period);
    }

    public JsonNode createSession(Long ownerId) {
        return advisorClientPort.createSession(ownerId);
    }

    public JsonNode getSessionHistory(Long ownerId, String sessionId) {
        return advisorClientPort.getSessionHistory(ownerId, sessionId);
    }

    public JsonNode ask(Long ownerId, String period, AdvisorAskCommand command) {
        return advisorClientPort.ask(ownerId, period, command);
    }

    public JsonNode generateReport(Long ownerId, String period) {
        return advisorClientPort.generateReport(ownerId, period);
    }

    public JsonNode refreshReport(Long ownerId, String period) {
        return advisorClientPort.refreshReport(ownerId, period);
    }

    public AdvisorFile exportReportDocx(Long ownerId, String period) {
        return advisorClientPort.exportReportDocx(ownerId, period);
    }
}
