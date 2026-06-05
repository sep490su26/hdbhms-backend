package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetResidentOnboardingStatusQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetStaffOnboardingStatusQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.GetOnboardingStatusUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.IdentityDocumentRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.DocumentType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.OnboardingAction;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.OnboardingStatusResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetOnboardingStatusService implements GetOnboardingStatusUseCase {
    UserRepository userRepository;
    PersonProfileRepository personProfileRepository;
    IdentityDocumentRepository identityDocumentRepository;
    JdbcTemplate jdbcTemplate;

    @Override
    public OnboardingStatusResponse ofResident(GetResidentOnboardingStatusQuery query) {
        User user = userRepository.findById(query.userId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        List<OnboardingAction> actions = new ArrayList<>();
        int priority = 1;

        if (user.isMustChangePassword()) {
            actions.add(
                    OnboardingAction.builder()
                            .actionKey("CHANGE_PASSWORD")
                            .label("Change temporary password")
                            .completed(false)
                            .priority(priority++)
                            .actionUrl("/change-temporary-password")
                            .build()
            );
        } else {
            actions.add(
                    OnboardingAction.builder()
                            .actionKey("CHANGE_PASSWORD")
                            .label("Change password")
                            .completed(true)
                            .priority(priority++)
                            .build()
            );
        }
        if (isIdentityVerificationRequired(user)) {
            boolean hasCompleted = hasCompletedIdentityVerification(user);
            actions.add(
                    OnboardingAction.builder()
                            .actionKey("IDENTITY_VERIFICATION")
                            .label("Upload identity documents")
                            .completed(hasCompleted)
                            .priority(priority++)
                            .actionUrl(hasCompleted ? null : "/identity-upload")
                            .build()
            );
        }
        boolean allCompleted = actions.stream().allMatch(OnboardingAction::isCompleted);

        return OnboardingStatusResponse.builder()
                .userId(user.getId())
                .onBoardingCompleted(allCompleted)
                .actions(actions)
                .build();
    }

    @Override
    public OnboardingStatusResponse ofStaff(GetStaffOnboardingStatusQuery query) {
        User user = userRepository.findById(query.userId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        List<OnboardingAction> actions = new ArrayList<>();
        int priority = 1;

        if (user.isMustChangePassword()) {
            actions.add(
                    OnboardingAction.builder()
                            .actionKey("CHANGE_PASSWORD")
                            .label("Change temporary password")
                            .completed(false)
                            .priority(priority++)
                            .actionUrl("/change-temporary-password")
                            .build()
            );
        } else {
            actions.add(
                    OnboardingAction.builder()
                            .actionKey("CHANGE_PASSWORD")
                            .label("Change password")
                            .completed(true)
                            .priority(priority++)
                            .build()
            );
        }
        boolean allCompleted = actions.stream().allMatch(OnboardingAction::isCompleted);

        return OnboardingStatusResponse.builder()
                .userId(user.getId())
                .onBoardingCompleted(allCompleted)
                .actions(actions)
                .build();
    }

    private boolean isIdentityVerificationRequired(User user) {
        return user.getRole() == Role.LEAD
                || user.getRole() == Role.TENANT;
    }

    private boolean hasCompletedIdentityVerification(User user) {
        PersonProfile personProfile = personProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        return personProfile.getPortraitFileId() != null
                && hasActiveCccdImages(personProfile.getId());
    }

    private boolean hasActiveCccdImages(Long profileId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM identity_documents
                        WHERE profile_id = ?
                          AND doc_type = ?
                          AND status = 'ACTIVE'
                          AND front_file_id IS NOT NULL
                          AND back_file_id IS NOT NULL
                        """,
                Integer.class,
                profileId,
                DocumentType.CCCD.name()
        );
        return count != null && count > 0;
    }
}
