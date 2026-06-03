package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
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

        // Return updated profile
        return getMyTenantProfileUseCase.execute();
    }
}
