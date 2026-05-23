package com.sep490.hdbhms.modules.tenant.controller;

import com.sep490.hdbhms.modules.tenant.dto.IdentityVerificationResponse;
import com.sep490.hdbhms.modules.tenant.service.IdentityVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/me")
@Tag(name = "Tenant Identity", description = "Optional tenant identity profile update APIs")
public class IdentityVerificationController {

    private final IdentityVerificationService identityVerificationService;

    public IdentityVerificationController(IdentityVerificationService identityVerificationService) {
        this.identityVerificationService = identityVerificationService;
    }

    @PostMapping(
            value = "/identity-verification",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Upload portrait and CCCD images",
            description = "Optional profile update endpoint. Mobile first-login onboarding no longer requires portrait or CCCD upload before entering Home."
    )
    public ResponseEntity<IdentityVerificationResponse> uploadIdentity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tenantId,
            @RequestPart("portrait_file") MultipartFile portraitFile,
            @RequestPart("id_card_front_file") MultipartFile idCardFrontFile,
            @RequestPart("id_card_back_file") MultipartFile idCardBackFile
    ) {
        return ResponseEntity.ok(identityVerificationService.uploadIdentity(
                Long.parseLong(jwt.getSubject()),
                tenantId,
                portraitFile,
                idCardFrontFile,
                idCardBackFile
        ));
    }
}
