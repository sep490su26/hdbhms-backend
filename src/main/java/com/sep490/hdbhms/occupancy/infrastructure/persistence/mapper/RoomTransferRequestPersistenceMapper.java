package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaDebtSnapshotRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomTransferRequestEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaTenantRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomTransferRequestPersistenceMapper {

    JpaTenantRepository jpaTenantRepository;
    JpaLeaseContractRepository jpaLeaseContractRepository;
    JpaRoomRepository jpaRoomRepository;
    JpaDebtSnapshotRepository jpaDebtSnapshotRepository;
    JpaPersonProfileRepository jpaPersonProfileRepository;
    JpaUserRepository jpaUserRepository;
    ObjectMapper objectMapper;

    public RoomTransferRequest toDomain(RoomTransferRequestEntity entity) {
        if (entity == null) return null;
        return RoomTransferRequest.builder()
                .id(entity.getId())
                .requestCode(entity.getRequestCode())
                .requesterId(entity.getRequester() != null ? entity.getRequester().getId() : null)
                .oldContractId(entity.getOldContract() != null ? entity.getOldContract().getId() : null)
                .oldRoomId(entity.getOldRoom() != null ? entity.getOldRoom().getId() : null)
                .targetRoomId(entity.getTargetRoom() != null ? entity.getTargetRoom().getId() : null)
                .transferringTenantProfileIds(readProfileIds(entity.getTransferringTenantProfileIds()))
                .nominatedHolderProfileId(entity.getNominatedHolderProfile() != null ? entity.getNominatedHolderProfile().getId() : null)
                .targetTransferType(entity.getTargetTransferType())
                .targetContractId(entity.getTargetContract() != null ? entity.getTargetContract().getId() : null)

                .requestedTransferDate(entity.getRequestedTransferDate())
                .reason(entity.getReason())
                .reservedSlots(entity.getReservedSlots())
                .reservationExpiresAt(entity.getReservationExpiresAt())
                .targetHolderApprovedById(entity.getTargetHolderApprovedBy() != null ? entity.getTargetHolderApprovedBy().getId() : null)
                .targetHolderApprovedAt(entity.getTargetHolderApprovedAt())
                .targetHolderRejectedAt(entity.getTargetHolderRejectedAt())

                .status(entity.getStatus())
                .debtSnapshotId(entity.getDebtSnapshot() != null ? entity.getDebtSnapshot().getId() : null)
                .newContractId(entity.getNewContract() != null ? entity.getNewContract().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public RoomTransferRequestEntity toEntity(RoomTransferRequest domain) {
        if (domain == null) return null;
        return RoomTransferRequestEntity.builder()
                .id(domain.getId())
                .requestCode(domain.getRequestCode())
                .requester(domain.getRequesterId() != null
                        ? jpaTenantRepository.findById(domain.getRequesterId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND))
                        : null)
                .oldContract(domain.getOldContractId() != null
                        ? jpaLeaseContractRepository.findById(domain.getOldContractId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND))
                        : null)
                .oldRoom(domain.getOldRoomId() != null
                        ? jpaRoomRepository.findById(domain.getOldRoomId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND))
                        : null)
                .targetRoom(domain.getTargetRoomId() != null
                        ? jpaRoomRepository.findById(domain.getTargetRoomId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND))
                        : null)
                .transferringTenantProfileIds(writeProfileIds(domain.getTransferringTenantProfileIds()))
                .nominatedHolderProfile(domain.getNominatedHolderProfileId() != null
                        ? jpaPersonProfileRepository.findById(domain.getNominatedHolderProfileId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND))
                        : null)
                .targetTransferType(domain.getTargetTransferType())
                .targetContract(domain.getTargetContractId() != null
                        ? jpaLeaseContractRepository.findById(domain.getTargetContractId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND))
                        : null)

                .requestedTransferDate(domain.getRequestedTransferDate())
                .reason(domain.getReason())
                .reservedSlots(domain.getReservedSlots())
                .reservationExpiresAt(domain.getReservationExpiresAt())
                .targetHolderApprovedBy(domain.getTargetHolderApprovedById() != null
                        ? jpaUserRepository.findById(domain.getTargetHolderApprovedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND))
                        : null)
                .targetHolderApprovedAt(domain.getTargetHolderApprovedAt())
                .targetHolderRejectedAt(domain.getTargetHolderRejectedAt())

                .status(domain.getStatus())
                .debtSnapshot(domain.getDebtSnapshotId() != null
                        ? jpaDebtSnapshotRepository.findById(domain.getDebtSnapshotId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND))
                        : null)
                .newContract(domain.getNewContractId() != null
                        ? jpaLeaseContractRepository.findById(domain.getNewContractId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND))
                        : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private java.util.List<Long> readProfileIds(String value) {
        if (value == null || value.isBlank()) {
            return java.util.List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<java.util.List<Long>>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid room transfer occupant payload", exception);
        }
    }

    private String writeProfileIds(java.util.List<Long> value) {
        if (value == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize room transfer occupant payload", exception);
        }
    }
}
