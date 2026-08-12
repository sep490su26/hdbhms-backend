package com.sep490.hdbhms.identityverification.application.service;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.sep490.hdbhms.identityverification.application.port.in.command.UploadIdentityVerificationCommand;
import com.sep490.hdbhms.identityverification.application.port.in.usecase.UploadIdentityVerificationUseCase;
import com.sep490.hdbhms.identityverification.application.port.out.CccdOcrExtractionPort;
import com.sep490.hdbhms.identityverification.domain.model.CccdExtractedIdentity;
import com.sep490.hdbhms.identityverification.infrastructure.web.dto.response.IdentityVerificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadIdentityVerificationService implements UploadIdentityVerificationUseCase {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final CccdOcrExtractionPort cccdOcrExtractionPort;

    @Override
    public IdentityVerificationResponse execute(UploadIdentityVerificationCommand command) {
        validateImageFile(command.frontImage(), true);
        validateImageFile(command.backImage(), false);

        CccdExtractedIdentity data = cccdOcrExtractionPort.extract(command.frontImage(), command.backImage())
                .orElse(null);
        if (data == null) {
            log.debug("Identity document extraction failed: the recognition service returned empty data.");
            return IdentityVerificationResponse.builder()
                    .success(false)
                    .code("CCCD_EXTRACTION_FAILED")
                    .message("Không thể trích xuất dữ liệu CCCD từ ảnh đã tải lên")
                    .qrExtracted(false)
                    .ocrExtracted(false)
                    .build();
        }

        log.debug("The recognition service completed identity document extraction.");
        return IdentityVerificationResponse.builder()
                .success(true)
                .code("CCCD_VISION_EXTRACTED")
                .message("Đã trích xuất dữ liệu CCCD")
                .qrExtracted(false)
                .ocrExtracted(true)
                .extractionMethod("VISION_OCR")
                .extractedIdentity(toResponse(data))
                .build();
    }

    private void validateImageFile(MultipartFile file, boolean front) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }
    }

    private IdentityVerificationResponse.ExtractedIdentity toResponse(CccdExtractedIdentity data) {
        return IdentityVerificationResponse.ExtractedIdentity.builder()
                .idNumber(data.idNumber())
                .fullName(data.fullName())
                .dob(data.dob())
                .gender(data.gender() == null ? null : data.gender().toVietnameseLabel())
                .address(data.address())
                .issuedDate(data.issuedDate())
                .issuedPlace(data.issuedPlace())
                .oldIdNumber(data.oldIdNumber())
                .build();
    }
}
