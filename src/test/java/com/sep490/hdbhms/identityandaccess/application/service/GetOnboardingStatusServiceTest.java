package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetResidentOnboardingStatusQuery;
import com.sep490.hdbhms.identityandaccess.application.port.out.IdentityDocumentRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.OnboardingStatusResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class GetOnboardingStatusServiceTest {
    UserRepository userRepository = mock(UserRepository.class);
    PersonProfileRepository personProfileRepository = mock(PersonProfileRepository.class);
    IdentityDocumentRepository identityDocumentRepository = mock(IdentityDocumentRepository.class);
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    void marksResidentOnboardingCompleteOnlyWhenIdentityMetadataIsComplete() {
        User user = tenantUser();
        PersonProfile profile = completeProfile();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(personProfileRepository.findByUserId(7L)).thenReturn(Optional.of(profile));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(17L), eq("CCCD")))
                .thenReturn(1);

        OnboardingStatusResponse response = service().ofResident(
                new GetResidentOnboardingStatusQuery(7L)
        );

        assertTrue(response.isOnBoardingCompleted());
        assertTrue(response.getActions().stream()
                .filter(action -> "IDENTITY_VERIFICATION".equals(action.getActionKey()))
                .allMatch(action -> action.isCompleted()));

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(query.capture(), eq(Integer.class), eq(17L), eq("CCCD"));
        assertTrue(query.getValue().contains("issued_date IS NOT NULL"));
        assertTrue(query.getValue().contains("issued_place IS NOT NULL"));
        assertTrue(query.getValue().contains("doc_number REGEXP"));
    }

    @Test
    void keepsResidentOnboardingIncompleteWhenProfileMetadataIsMissing() {
        User user = tenantUser();
        PersonProfile profile = PersonProfile.builder()
                .id(17L)
                .portraitFileId(91L)
                .email("tenant@example.com")
                .permanentAddress(null)
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(personProfileRepository.findByUserId(7L)).thenReturn(Optional.of(profile));

        OnboardingStatusResponse response = service().ofResident(
                new GetResidentOnboardingStatusQuery(7L)
        );

        assertFalse(response.isOnBoardingCompleted());
        assertTrue(response.getActions().stream()
                .filter(action -> "IDENTITY_VERIFICATION".equals(action.getActionKey()))
                .noneMatch(action -> action.isCompleted()));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void treatsEmailAsOptionalForResidentOnboarding() {
        User user = tenantUser();
        PersonProfile profile = PersonProfile.builder()
                .id(17L)
                .portraitFileId(91L)
                .permanentAddress("Ha Noi")
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(personProfileRepository.findByUserId(7L)).thenReturn(Optional.of(profile));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(17L), eq("CCCD")))
                .thenReturn(1);

        OnboardingStatusResponse response = service().ofResident(
                new GetResidentOnboardingStatusQuery(7L)
        );

        assertTrue(response.isOnBoardingCompleted());
        assertTrue(response.getActions().stream()
                .filter(action -> "IDENTITY_VERIFICATION".equals(action.getActionKey()))
                .allMatch(action -> action.isCompleted()));
    }

    private GetOnboardingStatusService service() {
        return new GetOnboardingStatusService(
                userRepository,
                personProfileRepository,
                identityDocumentRepository,
                jdbcTemplate
        );
    }

    private User tenantUser() {
        return User.builder()
                .id(7L)
                .role(Role.TENANT)
                .mustChangePassword(false)
                .build();
    }

    private PersonProfile completeProfile() {
        return PersonProfile.builder()
                .id(17L)
                .portraitFileId(91L)
                .email("tenant@example.com")
                .permanentAddress("Ha Noi")
                .build();
    }
}
