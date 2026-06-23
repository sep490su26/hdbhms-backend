package com.sep490.hdbhms.modules.tenant.controller;

import com.sep490.hdbhms.modules.tenant.dto.IdentityVerificationResponse;
import com.sep490.hdbhms.modules.tenant.service.IdentityVerificationService;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/me")
public class IdentityVerificationController {
    private final IdentityVerificationService identityVerificationService;

    public IdentityVerificationController(IdentityVerificationService identityVerificationService) {
        this.identityVerificationService = identityVerificationService;
    }

    @PostMapping(
            value = "/identity-verification",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<IdentityVerificationResponse> uploadIdentity(
            @PathVariable Long tenantId,
            @RequestPart("portrait_file") MultipartFile portraitFile,
            @RequestPart("id_card_front_file") MultipartFile idCardFrontFile,
            @RequestPart("id_card_back_file") MultipartFile idCardBackFile
    ) {
        return ResponseEntity.ok(identityVerificationService.uploadIdentity(
                AuthUtils.getCurrentAuthenticationId(),
                tenantId,
                portraitFile,
                idCardFrontFile,
                idCardBackFile
        ));
    }
}
