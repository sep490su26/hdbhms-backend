package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.SendPreCreatedAccountPort;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaTenantAccountProvisioningRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantAccountProvisioningIdentityTest {
    private static final String PHONE = "0900000000";
    private static final String EMAIL = "tenant@example.com";

    @Test
    void differentPhoneAndEmailUsersAreRejected() {
        User phoneUser = activeUser(1L, PHONE, "phone@example.com");
        User emailUser = activeUser(2L, "0900000001", EMAIL);
        UserRepository userRepository = mock(UserRepository.class);
        PersonProfileRepository profileRepository = mock(PersonProfileRepository.class);
        when(profileRepository.findByPhone(PHONE)).thenReturn(Optional.empty());
        when(userRepository.findByPhoneAndDeletedAtIsNull(PHONE)).thenReturn(Optional.of(phoneUser));
        when(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).thenReturn(Optional.of(emailUser));

        TenantAccountProvisioningService service = service(userRepository, profileRepository);

        AppException exception = assertThrows(
                AppException.class,
                () -> service.validateIdentity(PHONE, EMAIL)
        );

        assertEquals(ApiErrorCode.TENANT_ACCOUNT_IDENTITY_CONFLICT, exception.getApiErrorCode());
    }

    @Test
    void candidateAlreadyLinkedToAnotherProfileIsRejected() {
        User user = activeUser(1L, PHONE, EMAIL);
        UserRepository userRepository = mock(UserRepository.class);
        PersonProfileRepository profileRepository = mock(PersonProfileRepository.class);
        when(profileRepository.findByPhone(PHONE)).thenReturn(Optional.empty());
        when(userRepository.findByPhoneAndDeletedAtIsNull(PHONE)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailAndDeletedAtIsNull("other@example.com"))
                .thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(
                PersonProfile.builder().id(99L).build()
        ));

        TenantAccountProvisioningService service = service(userRepository, profileRepository);

        AppException exception = assertThrows(
                AppException.class,
                () -> service.validateIdentity(PHONE, "other@example.com")
        );

        assertEquals(ApiErrorCode.TENANT_ACCOUNT_IDENTITY_CONFLICT, exception.getApiErrorCode());
    }

    @Test
    void linkedProfileRemainsAuthoritativeOverContactCandidates() {
        User linkedUser = activeUser(1L, PHONE, EMAIL);
        User otherUser = activeUser(2L, "0900000001", "other@example.com");
        UserRepository userRepository = mock(UserRepository.class);
        PersonProfileRepository profileRepository = mock(PersonProfileRepository.class);
        PersonProfile profile = PersonProfile.builder().id(10L).userId(1L).build();
        when(profileRepository.findByPhone(PHONE)).thenReturn(Optional.of(profile));
        when(profileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(userRepository.findById(1L)).thenReturn(Optional.of(linkedUser));
        when(userRepository.findByEmailAndDeletedAtIsNull("other@example.com"))
                .thenReturn(Optional.of(otherUser));

        TenantAccountProvisioningService service = service(userRepository, profileRepository);

        assertDoesNotThrow(() -> service.validateIdentity(PHONE, "other@example.com"));
    }

    private TenantAccountProvisioningService service(
            UserRepository userRepository,
            PersonProfileRepository profileRepository
    ) {
        return new TenantAccountProvisioningService(
                mock(JdbcTemplate.class),
                userRepository,
                mock(TenantRepository.class),
                mock(PasswordEncoder.class),
                profileRepository,
                mock(SendPreCreatedAccountPort.class),
                mock(JpaTenantAccountProvisioningRepository.class),
                mock(PlatformTransactionManager.class)
        );
    }

    private User activeUser(Long id, String phone, String email) {
        User user = User.builder()
                .id(id)
                .phone(phone)
                .email(email)
                .role(Role.TENANT)
                .build();
        user.activeAccount();
        return user;
    }
}
