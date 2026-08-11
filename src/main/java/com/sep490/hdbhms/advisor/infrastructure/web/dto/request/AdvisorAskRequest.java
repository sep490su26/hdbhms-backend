package com.sep490.hdbhms.advisor.infrastructure.web.dto.request;

import com.sep490.hdbhms.advisor.application.port.in.command.AdvisorAskCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdvisorAskRequest(
        @NotBlank(message = "Vui lòng nhập câu hỏi")
        @Size(max = 2000, message = "Câu hỏi không được vượt quá 2000 ký tự")
        String question,
        String sessionId
) {
    public AdvisorAskCommand toCommand() {
        return new AdvisorAskCommand(question, sessionId);
    }
}
