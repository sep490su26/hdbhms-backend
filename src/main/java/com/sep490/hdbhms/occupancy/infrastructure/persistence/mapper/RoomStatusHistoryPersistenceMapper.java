package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.RoomStatusHistory;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomStatusHistoryEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomStatusHistoryPersistenceMapper {

    JpaRoomRepository jpaRoomRepository;
    JpaUserRepository jpaUserRepository;

    public RoomStatusHistory toDomain(RoomStatusHistoryEntity entity) {
        if (entity == null) return null;
        return RoomStatusHistory.builder()
                .id(entity.getId())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .fromStatus(entity.getFromStatus())
                .toStatus(entity.getToStatus())
                .reason(entity.getReason())
                .changedById(entity.getChangedBy() != null ? entity.getChangedBy().getId() : null)
                .changedAt(entity.getChangedAt())
                .build();
    }

    public RoomStatusHistoryEntity toEntity(RoomStatusHistory domain) {
        if (domain == null) return null;
        return RoomStatusHistoryEntity.builder()
                .id(domain.getId())
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.findById(domain.getRoomId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .fromStatus(domain.getFromStatus())
                .toStatus(domain.getToStatus())
                .reason(domain.getReason())
                .changedBy(domain.getChangedById() != null
                        ? jpaUserRepository.findById(domain.getChangedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .changedAt(domain.getChangedAt())
                .build();
    }
}
