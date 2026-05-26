package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.CreateUserCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.CreateStaffUserUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateStaffUserService implements CreateStaffUserUseCase {
    JavaMailSender mailSender;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    PersonProfileRepository personProfileRepository;

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
            throw new AppException(ApiErrorCode.UNDEFINED);
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
        log.info(user.toString());
        PersonProfile personProfile = PersonProfile.create(
                user.getId(),
                command.getFullName(),
                command.getPhone(),
                command.getEmail()
        );
        personProfileRepository.save(personProfile);
        sendAccountDetails(command, randomPassword);
//        usernameBloomFilter.add(domain.getUsername());
//        emailBloomFilter.add(domain.getEmail());

        return user;
    }

    @Async
    protected void sendAccountDetails(CreateUserCommand command, String randomPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("[Nhà trọ Hải Đăng] Gửi thông tin tài khoản người dùng");
        message.setTo(command.getEmail());
        message.setText(
                String.format(
                        """
                                Kính gửi Anh/Chị %s,
                                
                                Hệ thống đã tạo tài khoản cho Anh/Chị thành công.
                                
                                Thông tin đăng nhập:
                                
                                Tên đăng nhập: %s
                                Mật khẩu tạm thời: %s
                                
                                
                                Vui lòng đăng nhập và đổi mật khẩu sau lần đăng nhập đầu tiên để đảm bảo an toàn tài khoản.
                                Trân trọng.
                                """,
                        command.getFullName(),
                        command.getPhone(),
                        randomPassword
                )
        );
        mailSender.send(message);
        CompletableFuture.completedFuture(message);
    }
}
