package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.CreateUserCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.CreateStaffUserUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.SendPreCreatedAccountPort;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.RandomPasswordUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateStaffUserService implements CreateStaffUserUseCase {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    PersonProfileRepository personProfileRepository;
    SendPreCreatedAccountPort sendPreCreatedAccountPort;

//    @Qualifier("usernameBloomFilter")
//    RBloomFilter<String> usernameBloomFilter;
//    @Qualifier("emailBloomFilter")
//    RBloomFilter<String> emailBloomFilter;

    @Override
    public User execute(CreateUserCommand command) {
        if (
                command.getInitialRole() != Role.MANAGER
                        && command.getInitialRole() != Role.ACCOUNTANT
        ) {
            throw new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND);
        }
        if (
                userRepository.existsByEmail(command.getEmail())
                        || userRepository.existsByPhone(command.getPhone())
        ) {
            throw new AppException(ApiErrorCode.ACCOUNT_EXISTED);
        }
        String randomPassword = RandomPasswordUtils.generatePassword(
                6,
                true,
                true
        );
        User user = User.newUser(
                command.getPhone(),
                command.getEmail(),
                passwordEncoder.encode(randomPassword),
                command.getInitialRole()
        );
        user.activeAccount();
        user = userRepository.save(user);
        PersonProfile personProfile = PersonProfile.createForStaff(
                user.getId(),
                command.getFullName(),
                command.getPhone(),
                command.getEmail()
        );
        personProfileRepository.save(personProfile);
        sendPreCreatedAccountPort.sendAccountInformation(
                null,
                null,
                user.getId(),
                command.getEmail(),
                command.getFullName(),
                command.getPhone(),
                randomPassword
        );
//        usernameBloomFilter.add(domain.getUsername());
//        emailBloomFilter.add(domain.getEmail());

        return user;
    }
}
