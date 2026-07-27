package com.sep490.hdbhms.identityverification.application.service;

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
import org.springframework.web.server.ResponseStatusException;

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
        validateImageFile(command.frontImage(), "ảnh mặt trước CCCD");
        validateImageFile(command.backImage(), "ảnh mặt sau CCCD");

        CccdExtractedIdentity data = cccdOcrExtractionPort.extract(command.frontImage(), command.backImage())
                .orElse(null);
        if (data == null) {
            log.debug("CCCD extraction failed: vision service returned empty data.");
            return IdentityVerificationResponse.builder()
                    .success(false)
                    .code("CCCD_EXTRACTION_FAILED")
                    .message("Không thể trích xuất dữ liệu CCCD từ ảnh đã upload.")
                    .qrExtracted(false)
                    .ocrExtracted(false)
                    .build();
        }

        log.debug("CCCD extraction completed by vision service.");
        return IdentityVerificationResponse.builder()
                .success(true)
                .code("CCCD_VISION_EXTRACTED")
                .message("Đã trích xuất dữ liệu CCCD.")
                .qrExtracted(false)
                .ocrExtracted(true)
                .extractionMethod("VISION_OCR")
                .extractedIdentity(toResponse(data))
                .build();
    }

    private void validateImageFile(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Vui lòng upload " + label + "."
            );
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    label + " quá lớn, vui lòng chọn ảnh khác."
            );
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Định dạng " + label + " không hợp lệ.");
        }
    }

    private IdentityVerificationResponse.ExtractedIdentity toResponse(CccdExtractedIdentity data) {
        return IdentityVerificationResponse.ExtractedIdentity.builder()
                .idNumber(data.idNumber())
                .fullName(data.fullName())
                .dob(data.dob())
                .gender(data.gender() == null ? null : data.gender().name())
                .address(data.address())
                .issuedDate(data.issuedDate())
                .issuedPlace(data.issuedPlace())
                .oldIdNumber(data.oldIdNumber())
                .build();
    }
}
