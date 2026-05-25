//package com.sep490.hdbhms.modules.tenant.controller;
//
//import com.sep490.hdbhms.modules.tenant.dto.TenantProfileResponse;
//import com.sep490.hdbhms.modules.tenant.service.TenantProfileQueryService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/v1/tenants/{tenantId}/me")
//@Tag(name = "Tenant Mobile Profile", description = "Tenant self-service mobile profile APIs")
//public class TenantProfileController {
//
//    private final TenantProfileQueryService tenantProfileQueryService;
//
//    public TenantProfileController(TenantProfileQueryService tenantProfileQueryService) {
//        this.tenantProfileQueryService = tenantProfileQueryService;
//    }
//
//    @GetMapping("/profile")
//    @Operation(
//            summary = "Get current tenant profile",
//            description = "Returns the profile stored in database for the authenticated tenant in the requested tenant scope.",
//            security = @SecurityRequirement(name = "bearerAuth")
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Profile found"),
//            @ApiResponse(responseCode = "403", description = "Tenant does not belong to current user"),
//            @ApiResponse(responseCode = "404", description = "PROFILE_NOT_FOUND")
//    })
//    public ResponseEntity<TenantProfileResponse> getMyProfile(
//            @AuthenticationPrincipal Jwt jwt,
//            @PathVariable Long tenantId
//    ) {
//        return ResponseEntity.ok(
//                tenantProfileQueryService.getMyProfile(Long.parseLong(jwt.getSubject()), tenantId)
//        );
//    }
//}
