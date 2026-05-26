package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.CreateDefaultOwnerAccountUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.shared.constant.DefaultConfig;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateDefaultOwnerAccountService implements CreateDefaultOwnerAccountUseCase {
    DefaultConfig defaultConfig;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    PersonProfileRepository personProfileRepository;

    @Override
    public void execute() {
        if (userRepository.existsAnOwnerAccount()) {
            return;
        }
        if (
                userRepository.existsByEmail(defaultConfig.getOwner().getEmail())
                        || userRepository.existsByPhone(defaultConfig.getOwner().getPhone())
        ) {
            throw new AppException(ApiErrorCode.ACCOUNT_EXISTED);
        }
        User user = User.newUser(
                defaultConfig.getOwner().getPhone(),
                defaultConfig.getOwner().getEmail(),
                defaultConfig.getOwner().getPassword(),
                Role.OWNER
        );
        user.activeAccount();
        user.changePassword(passwordEncoder.encode(defaultConfig.getOwner().getPassword()));
        user = userRepository.save(user);
        PersonProfile personProfile = PersonProfile.create(
                user.getId(),
                defaultConfig.getOwner().getFullName(),
                defaultConfig.getOwner().getPhone(),
                defaultConfig.getOwner().getEmail()
        );
        personProfileRepository.save(personProfile);
    }
}
