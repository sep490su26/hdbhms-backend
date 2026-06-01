package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.EmergencyContactEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.VehicleEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaEmergencyContactRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaVehicleRepository;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.VehicleType;
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

        // Update Emergency Contacts (Delete & Re-insert)
        emergencyContactRepository.deleteAllByTenantProfileId(profile.getId());
        if (request.emergencyContacts() != null) {
            for (UpdateTenantProfileRequest.EmergencyContactDto dto : request.emergencyContacts()) {
                EmergencyContactEntity contact = EmergencyContactEntity.builder()
                        .tenantProfile(profile)
                        .fullName(dto.fullName())
                        .relationship(dto.relationship())
                        .phone(dto.phone())
                        .build();
                emergencyContactRepository.save(contact);
            }
        }

        // Update Vehicles (Soft Delete & Re-insert)
        vehicleRepository.softDeleteAllByProfileId(profile.getId());
        if (request.vehicles() != null) {
            for (UpdateTenantProfileRequest.VehicleDto dto : request.vehicles()) {
                FileMetadataEntity image = null;
                if (dto.imageFileId() != null) {
                    image = fileMetadataRepository.findById(dto.imageFileId()).orElse(null);
                }

                VehicleType type = VehicleType.MOTORBIKE;
                try {
                    if (dto.vehicleType() != null && !dto.vehicleType().isBlank()) {
                        type = VehicleType.valueOf(dto.vehicleType().toUpperCase());
                    }
                } catch (IllegalArgumentException e) {
                    // ignore and use default
                }

                VehicleEntity vehicle = VehicleEntity.builder()
                        .profile(profile)
                        .licensePlate(dto.licensePlate())
                        .vehicleType(type)
                        .imageFile(image)
                        .build();
                vehicleRepository.save(vehicle);
            }
        }

        // Return updated profile
        return getMyTenantProfileUseCase.execute();
    }
}
