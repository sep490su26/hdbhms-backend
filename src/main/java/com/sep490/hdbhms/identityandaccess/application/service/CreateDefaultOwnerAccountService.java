package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.CreateUserCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.CreateDefaultOwnerAccountUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
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

    @Override
    public void execute() {
        if (userRepository.existsAnOwnerAccount()) {
            return;
        }
        CreateUserCommand command = new CreateUserCommand(
                defaultConfig.getOwner().getEmail(),
                defaultConfig.getOwner().getPhone(),
                defaultConfig.getOwner().getPassword(),
                Role.OWNER
        );
        if (
                userRepository.existsByEmail(command.getEmail())
                        || userRepository.existsByPhone(command.getPhone())
        ) {
            throw new AppException(ApiErrorCode.ACCOUNT_EXISTED);
        }
        User user = User.newUser(
                command.getEmail(),
                command.getPhone(),
                command.getPassword(),
                command.getInitialRole()
        );
        user.changePassword(passwordEncoder.encode(command.getPassword()));
        userRepository.save(user);
    }
}
