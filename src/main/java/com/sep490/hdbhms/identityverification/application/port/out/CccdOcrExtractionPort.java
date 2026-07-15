package com.sep490.hdbhms.identityverification.application.port.out;

import com.sep490.hdbhms.identityverification.domain.model.CccdExtractedIdentity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface CccdOcrExtractionPort {
    Optional<CccdExtractedIdentity> extract(MultipartFile cccdImage);
}
