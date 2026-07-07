package com.sep490.hdbhms.identityverification.infrastructure.web.controller;

import com.sep490.hdbhms.identityverification.application.port.in.command.UploadIdentityVerificationCommand;
import com.sep490.hdbhms.identityverification.application.port.in.usecase.UploadIdentityVerificationUseCase;
import com.sep490.hdbhms.identityverification.infrastructure.web.dto.response.IdentityVerificationResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity-verification")
public class IdentityVerificationController {
    private final UploadIdentityVerificationUseCase uploadIdentityVerificationUseCase;

    @PostMapping(value = "/cccd/qr/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<IdentityVerificationResponse> scanCccdQr(
            @RequestPart("cccdImage") MultipartFile cccdImage
    ) {
        UploadIdentityVerificationCommand command = UploadIdentityVerificationCommand.builder()
                .cccdImage(cccdImage)
                .build();

        return ApiResponse.<IdentityVerificationResponse>builder()
                .data(uploadIdentityVerificationUseCase.execute(command))
                .build();
    }
}
