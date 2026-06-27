package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.DepositFormCoOccupant;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositFormCoOccupantEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositFormEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositFormPersistenceMapper {
    JpaRoomRepository jpaRoomRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;

    public DepositForm toDomain(DepositFormEntity entity) {
        if (entity == null) return null;
        return DepositForm.builder()
                .id(entity.getId())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .idNumber(entity.getIdNumber())
                .permanentAddress(entity.getPermanentAddress())
                .dob(entity.getDob())
                .idIssueDate(entity.getIdIssueDate())
                .idIssuePlace(entity.getIdIssuePlace())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .idFrontFileId(entity.getIdFrontFile() != null ? entity.getIdFrontFile().getId() : null)
                .idBackFileId(entity.getIdBackFile() != null ? entity.getIdBackFile().getId() : null)
                .portraitFileId(entity.getPortraitFile() != null ? entity.getPortraitFile().getId() : null)
                .expectedMoveInDate(entity.getExpectedMoveInDate())
                .expectedLeaseSignDate(entity.getExpectedLeaseSignDate())
                .paymentDueAt(entity.getPaymentDueAt())
                .depositExpiresAt(entity.getDepositExpiresAt())
                .depositMonths(entity.getDepositMonths())
                .paymentCycleMonths(entity.getPaymentCycleMonths())
                .occupantCount(entity.getOccupantCount())
                .coOccupants(toCoOccupants(entity.getCoOccupants()))
                .status(entity.getStatus())
                .confirmedAt(entity.getConfirmedAt())
                .rejectReason(entity.getRejectReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public DepositFormEntity toEntity(DepositForm domain) {
        if (domain == null) return null;
        DepositFormEntity entity = DepositFormEntity.builder()
                .id(domain.getId())
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.findById(domain.getRoomId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_FORM_NOT_FOUND))
                        : null)
                .idNumber(domain.getIdNumber())
                .permanentAddress(domain.getPermanentAddress())
                .dob(domain.getDob())
                .idIssueDate(domain.getIdIssueDate())
                .idIssuePlace(domain.getIdIssuePlace())
                .fullName(domain.getFullName())
                .email(domain.getEmail())
                .phone(domain.getPhone())
                .idFrontFile(domain.getIdFrontFileId() != null
                        ? jpaFileMetadataRepository.getReferenceById(domain.getIdFrontFileId())
                        : null)
                .idBackFile(domain.getIdBackFileId() != null
                        ? jpaFileMetadataRepository.getReferenceById(domain.getIdBackFileId())
                        : null)
                .portraitFile(domain.getPortraitFileId() != null
                        ? jpaFileMetadataRepository.getReferenceById(domain.getPortraitFileId())
                        : null)
                .expectedMoveInDate(domain.getExpectedMoveInDate())
                .expectedLeaseSignDate(domain.getExpectedLeaseSignDate())
                .paymentDueAt(domain.getPaymentDueAt())
                .depositMonths(domain.getDepositMonths())
                .paymentCycleMonths(domain.getPaymentCycleMonths())
                .occupantCount(domain.getOccupantCount())
                .depositExpiresAt(domain.getDepositExpiresAt())
                .status(domain.getStatus())
                .confirmedAt(domain.getConfirmedAt())
                .rejectReason(domain.getRejectReason())
                .createdAt(domain.getCreatedAt())
                .build();
        entity.setCoOccupants(toCoOccupantEntities(domain.getCoOccupants(), entity));
        return entity;
    }

    private List<DepositFormCoOccupant> toCoOccupants(List<DepositFormCoOccupantEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(entity -> DepositFormCoOccupant.builder()
                        .id(entity.getId())
                        .fullName(entity.getFullName())
                        .phone(entity.getPhone())
                        .displayOrder(entity.getDisplayOrder())
                        .build())
                .toList();
    }

    private List<DepositFormCoOccupantEntity> toCoOccupantEntities(
            List<DepositFormCoOccupant> coOccupants,
            DepositFormEntity depositForm
    ) {
        if (coOccupants == null) {
            return new ArrayList<>();
        }
        return coOccupants.stream()
                .map(coOccupant -> {
                    DepositFormCoOccupantEntity entity = DepositFormCoOccupantEntity.builder()
                            .id(coOccupant.getId())
                            .depositForm(depositForm)
                            .fullName(coOccupant.getFullName())
                            .phone(coOccupant.getPhone())
                            .displayOrder(coOccupant.getDisplayOrder())
                            .build();
                    entity.setDepositForm(depositForm);
                    return entity;
                })
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
}
