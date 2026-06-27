package com.sep490.hdbhms.changerequest.infrastructure.persistence.mapper;

import com.sep490.hdbhms.changerequest.domain.model.ChangeRequestEvent;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEntity;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEventEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.EntityManager;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ChangeRequestEventPersistenceMapper {

    @Autowired
    protected EntityManager entityManager;

    @Mapping(source = "changeRequest.id", target = "requestId")
    @Mapping(source = "actedBy.id", target = "actedBy")
    public abstract ChangeRequestEvent toDomain(ChangeRequestEventEntity entity);

    @Mapping(target = "changeRequest", expression = "java(mapChangeRequest(domain.getRequestId()))")
    @Mapping(target = "actedBy", expression = "java(mapUser(domain.getActedBy()))")
    public abstract ChangeRequestEventEntity toEntity(ChangeRequestEvent domain);

    protected UserEntity mapUser(Long id) {
        if (id == null) return null;
        return entityManager.getReference(UserEntity.class, id);
    }

    protected ChangeRequestEntity mapChangeRequest(Long id) {
        if (id == null) return null;
        return entityManager.getReference(ChangeRequestEntity.class, id);
    }
}
