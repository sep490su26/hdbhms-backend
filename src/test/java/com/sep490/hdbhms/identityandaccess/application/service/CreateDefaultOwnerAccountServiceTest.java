package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.shared.constant.DefaultConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CreateDefaultOwnerAccountServiceTest {

    @Test
    void createsConfiguredOwnerEvenWhenSeedOwnerAlreadyExists() {
        DefaultConfig config = defaultConfig();
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PersonProfileRepository personProfileRepository = mock(PersonProfileRepository.class);
        CreateDefaultOwnerAccountService service = new CreateDefaultOwnerAccountService(
                config,
                userRepository,
                passwordEncoder,
                personProfileRepository
        );

        when(userRepository.findByPhoneOrEmailAndDeletedAtIsNull("0999999999", "alo@gmail.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("Tien260804")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(99L)
                    .phone(user.getPhone())
                    .email(user.getEmail())
                    .passwordHash(user.getPasswordHash())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .mustChangePassword(user.isMustChangePassword())
                    .build();
        });

        service.execute();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("0999999999", savedUser.getPhone());
        assertEquals("alo@gmail.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPasswordHash());
        assertEquals(Role.OWNER, savedUser.getRole());
        assertEquals(AccountStatus.ACTIVE, savedUser.getStatus());
        assertFalse(savedUser.isMustChangePassword());

        ArgumentCaptor<PersonProfile> profileCaptor = ArgumentCaptor.forClass(PersonProfile.class);
        verify(personProfileRepository).save(profileCaptor.capture());
        assertEquals(99L, profileCaptor.getValue().getUserId());
        assertEquals("Dang", profileCaptor.getValue().getFullName());
    }

    @Test
    void syncsConfiguredOwnerPasswordAndFirstLoginFlag() {
        DefaultConfig config = defaultConfig();
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PersonProfileRepository personProfileRepository = mock(PersonProfileRepository.class);
        CreateDefaultOwnerAccountService service = new CreateDefaultOwnerAccountService(
                config,
                userRepository,
                passwordEncoder,
                personProfileRepository
        );
        User existingOwner = User.builder()
                .id(10L)
                .phone("0999999999")
                .email("alo@gmail.com")
                .passwordHash("old-password")
                .role(Role.OWNER)
                .status(AccountStatus.PENDING_CONTRACT)
                .mustChangePassword(true)
                .build();

        when(userRepository.findByPhoneOrEmailAndDeletedAtIsNull("0999999999", "alo@gmail.com"))
                .thenReturn(Optional.of(existingOwner));
        when(passwordEncoder.matches("Tien260804", "old-password")).thenReturn(false);
        when(passwordEncoder.encode("Tien260804")).thenReturn("new-password");

        service.execute();

        assertEquals(AccountStatus.ACTIVE, existingOwner.getStatus());
        assertFalse(existingOwner.isMustChangePassword());
        assertEquals("new-password", existingOwner.getPasswordHash());
        verify(userRepository).save(existingOwner);
        verifyNoInteractions(personProfileRepository);
    }

    private DefaultConfig defaultConfig() {
        DefaultConfig config = new DefaultConfig();
        config.getOwner().setEmail("alo@gmail.com");
        config.getOwner().setFullName("Dang");
        config.getOwner().setPassword("Tien260804");
        config.getOwner().setPhone("0999999999");
        return config;
    }
}
