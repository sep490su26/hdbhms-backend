package com.sep490.hdbhms.changerequest.infrastructure.persistence.mapper;

import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import jakarta.persistence.EntityManager;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ChangeRequestPersistenceMapper {

    @Autowired
    protected EntityManager entityManager;

    @Mapping(source = "requester.id", target = "requesterId")
    @Mapping(source = "evidenceFile.id", target = "evidenceFileId")
    @Mapping(source = "assignedTo.id", target = "assignedTo")
    @Mapping(source = "resolvedBy.id", target = "resolvedBy")
    public abstract ChangeRequest toDomain(ChangeRequestEntity entity);

    @Mapping(target = "requester", expression = "java(mapUser(domain.getRequesterId()))")
    @Mapping(target = "evidenceFile", expression = "java(mapFile(domain.getEvidenceFileId()))")
    @Mapping(target = "assignedTo", expression = "java(mapUser(domain.getAssignedTo()))")
    @Mapping(target = "resolvedBy", expression = "java(mapUser(domain.getResolvedBy()))")
    public abstract ChangeRequestEntity toEntity(ChangeRequest domain);

    protected UserEntity mapUser(Long id) {
        if (id == null) return null;
        return entityManager.getReference(UserEntity.class, id);
    }

    protected FileMetadataEntity mapFile(Long id) {
        if (id == null) return null;
        return entityManager.getReference(FileMetadataEntity.class, id);
    }
}
