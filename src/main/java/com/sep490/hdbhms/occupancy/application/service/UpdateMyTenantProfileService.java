package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.VehicleStatus;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.VehicleType;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.EmergencyContactEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.VehicleEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaEmergencyContactRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaVehicleRepository;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyTenantProfileUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateMyTenantProfileUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdateTenantProfileRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.TenantProfileResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateMyTenantProfileService implements UpdateMyTenantProfileUseCase {

    private final JpaPersonProfileRepository personProfileRepository;
    private final JpaEmergencyContactRepository emergencyContactRepository;
    private final JpaVehicleRepository vehicleRepository;
    private final JpaFileMetadataRepository fileMetadataRepository;
    private final GetMyTenantProfileUseCase getMyTenantProfileUseCase;

    @Override
    @Transactional
    public TenantProfileResponse execute(UpdateTenantProfileRequest request) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        if (userId == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }

        PersonProfileEntity profile = personProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        // Update Person Profile
        profile.setPhone(request.phone());
        profile.setEmail(request.email());
        profile.setUpdatedAt(LocalDateTime.now());
        personProfileRepository.save(profile);

        // Update Emergency Contacts
        updateEmergencyContacts(profile, request.emergencyContacts());

        // Update Vehicles
        updateVehicles(profile, request.vehicles());

        // Return updated profile
        return getMyTenantProfileUseCase.execute();
    }

    private void updateEmergencyContacts(PersonProfileEntity profile, List<UpdateTenantProfileRequest.EmergencyContactDto> requestContacts) {
        emergencyContactRepository.deleteAllByTenantProfile_Id(profile.getId());
        if (requestContacts != null && !requestContacts.isEmpty()) {
            List<EmergencyContactEntity> contactsToSave = requestContacts.stream().map(dto ->
                    EmergencyContactEntity.builder()
                            .tenantProfile(profile)
                            .fullName(dto.fullName())
                            .relationship(dto.relationship())
                            .phone(dto.phone())
                            .build()
            ).collect(Collectors.toList());
            emergencyContactRepository.saveAll(contactsToSave);
        }
    }

    private void updateVehicles(PersonProfileEntity profile, List<UpdateTenantProfileRequest.VehicleDto> requestVehicles) {
        List<VehicleEntity> existingVehicles = vehicleRepository.findByProfile_IdAndStatus(profile.getId(), VehicleStatus.ACTIVE);
        Map<String, VehicleEntity> existingVehiclesMap = existingVehicles.stream()
                .collect(Collectors.toMap(VehicleEntity::getLicensePlate, Function.identity()));

        if (requestVehicles != null) {
            Set<String> requestPlates = requestVehicles.stream()
                    .map(UpdateTenantProfileRequest.VehicleDto::licensePlate)
                    .collect(Collectors.toSet());

            // Add or update vehicles
            for (UpdateTenantProfileRequest.VehicleDto dto : requestVehicles) {
                VehicleEntity vehicle = existingVehiclesMap.get(dto.licensePlate());
                if (vehicle != null) {
                    // Update existing
                    if (dto.imageFileId() != null) {
                        vehicle.setImageFile(fileMetadataRepository.findById(dto.imageFileId()).orElse(null));
                    } else {
                        vehicle.setImageFile(null);
                    }
                    if (dto.vehicleType() != null) {
                        vehicle.setVehicleType(VehicleType.valueOf(dto.vehicleType()));
                    }
                } else {
                    // Create new
                    vehicle = VehicleEntity.builder()
                            .profile(profile)
                            .licensePlate(dto.licensePlate())
                            .vehicleType(dto.vehicleType() != null ? VehicleType.valueOf(dto.vehicleType()) : VehicleType.MOTORBIKE)
                            .status(VehicleStatus.ACTIVE)
                            .build();
                    if (dto.imageFileId() != null) {
                        vehicle.setImageFile(fileMetadataRepository.findById(dto.imageFileId()).orElse(null));
                    }
                    existingVehicles.add(vehicle);
                }
            }

            // Logically delete omitted vehicles
            for (VehicleEntity vehicle : existingVehicles) {
                if (!requestPlates.contains(vehicle.getLicensePlate()) && vehicle.getStatus() == VehicleStatus.ACTIVE) {
                    vehicle.setStatus(VehicleStatus.INACTIVE);
                    vehicle.setDeletedAt(LocalDateTime.now());
                    vehicle.setActiveUniqueToken(null);
                }
            }
        } else {
            // If request is null, delete all active
            for (VehicleEntity vehicle : existingVehicles) {
                vehicle.setStatus(VehicleStatus.INACTIVE);
                vehicle.setDeletedAt(LocalDateTime.now());
                vehicle.setActiveUniqueToken(null);
            }
        }

        vehicleRepository.saveAll(existingVehicles);
    }
}

