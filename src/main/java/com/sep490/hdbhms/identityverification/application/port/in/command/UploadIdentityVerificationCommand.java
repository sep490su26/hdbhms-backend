package com.sep490.hdbhms.identityverification.application.port.in.command;

import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

@Builder
public record UploadIdentityVerificationCommand(
        MultipartFile cccdImage
) {
}
