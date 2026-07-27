package com.sep490.hdbhms.advisor.infrastructure.web.dto.request;

import com.sep490.hdbhms.advisor.application.port.in.command.AdvisorAskCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdvisorAskRequest(
        @NotBlank
        @Size(min = 1, max = 2000)
        String question,
        String sessionId
) {
    public AdvisorAskCommand toCommand() {
        return new AdvisorAskCommand(question, sessionId);
    }
}
