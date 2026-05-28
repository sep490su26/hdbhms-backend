package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.CreateUserCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.CreateUserUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
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
public class CreateUserService implements CreateUserUseCase {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
//    @Qualifier("usernameBloomFilter")
//    RBloomFilter<String> usernameBloomFilter;
//    @Qualifier("emailBloomFilter")
//    RBloomFilter<String> emailBloomFilter;

    @Override
    public User execute(CreateUserCommand command) {
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
        user = userRepository.save(user);
        log.info(user.toString());

//        usernameBloomFilter.add(domain.getUsername());
//        emailBloomFilter.add(domain.getEmail());
        return user;
    }
}
