package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.UpdateMyPersonProfileCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.UpdateMyPersonProfileUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateMyPersonProfileService implements UpdateMyPersonProfileUseCase {
    PersonProfileRepository personProfileRepository;

    @Override
    public PersonProfile execute(UpdateMyPersonProfileCommand command) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        if (userId == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }

        PersonProfile profile = personProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ApiErrorCode.USER_PROFILE_NOT_FOUND));

        if (command.contactPhone() != null) {
            profile.setPhone(normalize(command.contactPhone()));
        }
        if (command.email() != null) {
            profile.setEmail(normalize(command.email()));
        }
        return personProfileRepository.save(profile);
    }

    private String normalize(String value) {
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
