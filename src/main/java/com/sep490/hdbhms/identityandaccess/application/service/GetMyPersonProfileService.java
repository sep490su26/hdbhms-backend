package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.GetMyPersonProfileUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetMyPersonProfileService implements GetMyPersonProfileUseCase {
    PersonProfileRepository personProfileRepository;

    @Override
    public PersonProfile execute() {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        if (userId == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        return personProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
    }
}
