package com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper;

import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.infrastructure.web.mapper.FileMetadataWebMapper;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.PersonProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN, uses = {FileMetadataWebMapper.class})
public interface PersonProfileWebMapper {
    @Mapping(target = "id", source = "personProfile.id")
    @Mapping(target = "createdAt", source = "personProfile.createdAt")
    @Mapping(target = "deletedAt", source = "personProfile.deletedAt")
    PersonProfileResponse toResponse(PersonProfile personProfile, FileMetadata fileMetadata);
}
