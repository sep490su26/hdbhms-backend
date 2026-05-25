package com.sep490.hdbhms.modules.mobile.controller;

import com.sep490.hdbhms.modules.mobile.dto.MobileContractListItem;
import com.sep490.hdbhms.modules.mobile.dto.MobileLeaseContractResponse;
import com.sep490.hdbhms.modules.mobile.service.MobileLeaseContractService;
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
@RequestMapping("/api/v1/tenants/{tenantId}/contracts")
@Tag(name = "Tenant Mobile Contracts", description = "Tenant self-service mobile contract APIs")
public class MobileLeaseContractController {

    private final MobileLeaseContractService mobileLeaseContractService;

    public MobileLeaseContractController(MobileLeaseContractService mobileLeaseContractService) {
        this.mobileLeaseContractService = mobileLeaseContractService;
    }

    @GetMapping("/my-list")
    @Operation(
            summary = "List all lease contracts for authenticated tenant",
            description = "Returns all lease contracts (active, expiring, expired, etc.) for the tenant scope.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<MobileContractListItem>> listMyContracts(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId
    ) {
        return ResponseEntity.ok(
                mobileLeaseContractService.listMyContracts(Long.parseLong(jwt.getSubject()), tenantId)
        );
    }

    @GetMapping("/{contractId}")
    @Operation(
            summary = "Get lease contract detail by ID",
            description = "Returns full detail of a specific lease contract.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contract found"),
            @ApiResponse(responseCode = "403", description = "Tenant does not belong to current user"),
            @ApiResponse(responseCode = "404", description = "CONTRACT_NOT_FOUND")
    })
    public ResponseEntity<MobileLeaseContractResponse> getContractById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @PathVariable Long contractId
    ) {
        return ResponseEntity.ok(
                mobileLeaseContractService.getContractById(Long.parseLong(jwt.getSubject()), tenantId, contractId)
        );
    }

    @GetMapping("/my-active")
    @Operation(
            summary = "Get current tenant active lease contract",
            description = "Returns the active or expiring-soon lease contract for the authenticated tenant scope.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contract found"),
            @ApiResponse(responseCode = "403", description = "Tenant does not belong to current user"),
            @ApiResponse(responseCode = "404", description = "CONTRACT_NOT_FOUND")
    })
    public ResponseEntity<MobileLeaseContractResponse> getMyActiveContract(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId
    ) {
        return ResponseEntity.ok(
                mobileLeaseContractService.getMyActiveContract(Long.parseLong(jwt.getSubject()), tenantId)
        );
    }
}

