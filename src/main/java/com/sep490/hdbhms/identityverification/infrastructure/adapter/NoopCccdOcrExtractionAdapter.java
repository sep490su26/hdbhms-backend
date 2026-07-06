package com.sep490.hdbhms.identityverification.infrastructure.adapter;

import com.sep490.hdbhms.identityverification.application.port.out.CccdOcrExtractionPort;
import com.sep490.hdbhms.identityverification.domain.model.CccdExtractedIdentity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Component
@ConditionalOnMissingBean(CccdOcrExtractionPort.class)
public class NoopCccdOcrExtractionAdapter implements CccdOcrExtractionPort {
    @Override
    public Optional<CccdExtractedIdentity> extract(MultipartFile cccdImage) {
        return Optional.empty();
    }
}
