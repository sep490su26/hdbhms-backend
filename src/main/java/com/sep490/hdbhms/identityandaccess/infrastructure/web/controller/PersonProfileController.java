package com.sep490.hdbhms.identityandaccess.infrastructure.web.controller;

import com.sep490.hdbhms.file.application.port.in.query.GetFileMetadataFromIdQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.GetFileMetadataFromIdUseCase;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.GetMyPersonProfileUseCase;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.PersonProfileResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper.PersonProfileWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/person-profiles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonProfileController {
    PersonProfileWebMapper personProfileWebMapper;
    GetMyPersonProfileUseCase getMyPersonProfileUseCase;
    GetFileMetadataFromIdUseCase getFileMetadataFromIdUseCase;

    @GetMapping("/me")
    public ApiResponse<PersonProfileResponse> getMyPersonProfile() {
        PersonProfile personProfile = getMyPersonProfileUseCase.execute();
        FileMetadata fileMetadata = getFileMetadataFromIdUseCase.execute(
                new GetFileMetadataFromIdQuery(personProfile.getPortraitFileId())
        );
        return ApiResponse.<PersonProfileResponse>builder()
                .data(
                        personProfileWebMapper.toResponse(
                                personProfile,
                                fileMetadata
                        )
                )
                .build();
    }
}
