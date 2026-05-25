package com.sep490.hdbhms.modules.mobile.controller;

import com.sep490.hdbhms.modules.mobile.dto.MobileContractListItem;
import com.sep490.hdbhms.modules.mobile.dto.MobileDepositContractResponse;
import com.sep490.hdbhms.modules.mobile.service.MobileDepositContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/deposits")
@Tag(name = "Tenant Mobile Deposit Contracts", description = "Tenant deposit contract APIs for mobile")
public class MobileDepositContractController {

    private final MobileDepositContractService depositContractService;

    public MobileDepositContractController(MobileDepositContractService depositContractService) {
        this.depositContractService = depositContractService;
    }

    @GetMapping("/my-list")
    @Operation(
            summary = "List all deposit agreements for authenticated tenant",
            description = "Returns all deposit agreements linked to the tenant scope.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<MobileContractListItem>> listMyDeposits(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId
    ) {
        return ResponseEntity.ok(
                depositContractService.listMyDeposits(Long.parseLong(jwt.getSubject()), tenantId)
        );
    }

    @GetMapping("/{depositId}")
    @Operation(
            summary = "Get deposit agreement detail by ID",
            description = "Returns full detail of a specific deposit agreement.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposit found"),
            @ApiResponse(responseCode = "403", description = "Tenant does not belong to current user"),
            @ApiResponse(responseCode = "404", description = "DEPOSIT_NOT_FOUND")
    })
    public ResponseEntity<MobileDepositContractResponse> getDepositById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @PathVariable Long depositId
    ) {
        return ResponseEntity.ok(
                depositContractService.getDepositById(Long.parseLong(jwt.getSubject()), tenantId, depositId)
        );
    }
}
