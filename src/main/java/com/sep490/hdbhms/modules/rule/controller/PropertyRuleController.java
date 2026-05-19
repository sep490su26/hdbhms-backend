package com.sep490.hdbhms.modules.rule.controller;

import com.sep490.hdbhms.modules.rule.dto.PropertyRuleResponse;
import com.sep490.hdbhms.modules.rule.service.PropertyRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/rules")
@Tag(name = "Tenant Mobile Rules", description = "Tenant self-service mobile property rule APIs")
public class PropertyRuleController {

    private final PropertyRuleService propertyRuleService;

    public PropertyRuleController(PropertyRuleService propertyRuleService) {
        this.propertyRuleService = propertyRuleService;
    }

    @GetMapping
    @Operation(
            summary = "Get active property rules for current tenant",
            description = "Returns active rules of the property where the authenticated tenant is currently renting.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rules returned"),
            @ApiResponse(responseCode = "403", description = "Tenant does not belong to current user"),
            @ApiResponse(responseCode = "404", description = "CURRENT_PROPERTY_NOT_FOUND")
    })
    public ResponseEntity<PropertyRuleResponse> getRules(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId
    ) {
        return ResponseEntity.ok(
                propertyRuleService.getRulesForCurrentTenant(Long.parseLong(jwt.getSubject()), tenantId)
        );
    }
}
