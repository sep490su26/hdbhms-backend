package com.sep490.hdbhms.advisor.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.sep490.hdbhms.advisor.application.port.in.command.AdvisorAskCommand;

public interface AdvisorClientPort {
    JsonNode getKpiOverview(Long landlordId, String period);

    JsonNode getSuggestions(Long landlordId, String period);

    JsonNode getAnalysis(Long landlordId, String period);

    JsonNode createSession(Long landlordId);

    JsonNode getSessionHistory(Long landlordId, String sessionId);

    JsonNode ask(Long landlordId, String period, AdvisorAskCommand command);

    JsonNode generateReport(Long landlordId, String period);

    JsonNode refreshReport(Long landlordId, String period);

    AdvisorFile exportReportDocx(Long landlordId, String period);
}
