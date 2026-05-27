package com.sep490.hdbhms.portal.application.service;

import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.portal.application.port.in.query.GetHomeQuery;
import com.sep490.hdbhms.portal.application.port.in.usecase.GetHomeUseCase;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.HomeResponse;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.UserHomeResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetHomeService implements GetHomeUseCase {
    TenantRepository tenantRepository;
    FileMetadataRepository fileMetadataRepository;
    PersonProfileRepository personProfileRepository;

    @Override
    public HomeResponse execute(GetHomeQuery query) {
        tenantRepository.findByUserId(query.userId()).orElseThrow(
                () -> new RuntimeException("Tenant not found")
        );
        PersonProfile personProfile = personProfileRepository.findByUserId(query.userId())
                .orElseThrow(
                        () -> new RuntimeException("Person profile not found")
                );
        FileMetadata portraitFile = fileMetadataRepository.findById(personProfile.getPortraitFileId())
                .orElse(null);
        return HomeResponse.builder()
                .user(
                        UserHomeResponse.builder()
                                .fullName(personProfile.getFullName())
                                .avatarUrl(portraitFile == null ? null : "/files/download/" + portraitFile.getId())
                                .build()
                )
                .build();
    }
}
