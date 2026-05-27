package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveDepositFormCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ApproveDepositFormUseCase;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposit/forms")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositFormController {
    ApproveDepositFormUseCase approveDepositFormUseCase;

//    @PostMapping("/{depositFormId}/approve")
//    public ApiResponse<Void> approveDepositForm(@PathVariable Long depositFormId) {
//        approveDepositFormUseCase.approveAndInitiatePayment(
//                new ApproveDepositFormCommand(depositFormId)
//        );
//        return ApiResponse.<Void>builder().build();
//    }
}
