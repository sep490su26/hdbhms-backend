package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.UserModificationHistory;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserModificationHistoryEntity;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserModificationHistoryPersistenceMapper {
    UserRepository userRepository;
    UserPersistenceMapper userPersistenceMapper;

    public UserModificationHistory toDomain(UserModificationHistoryEntity entity) {
        return UserModificationHistory.builder()
//                .id(entity.getId())
                .accountId(entity.getUser().getId())
                .type(entity.getType())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .changedAt(entity.getChangedAt())
                .build();
    }

    public UserModificationHistoryEntity toEntity(UserModificationHistory domain) {
        var userEntity = userRepository.findById(domain.getAccountId())
                .map(userPersistenceMapper::toEntity)
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));

        return UserModificationHistoryEntity.builder()
//                .id(domain.getId())
                .user(userEntity)
                .type(domain.getType())
                .oldValue(domain.getOldValue())
                .newValue(domain.getNewValue())
                .changedAt(domain.getChangedAt())
                .build();
    }
}
