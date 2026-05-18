package com.sep490.hdbhms.shared.config;

import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.CreateAccountUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper.AccountWebMapper;
import com.sep490.hdbhms.shared.constant.Default;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;

@Slf4j
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppInitConfig {
    CreateAccountUseCase createAccountUseCase;
    UserRepository userRepository;
    AccountWebMapper accountWebMapper;
    Default aDefault;

    @Bean
    ApplicationRunner init() {
        log.info("Initializing application");
        return args -> {
//            Role adminRoleEntity = null;
//            if (roleRepository.findByRoleName(PredefinedRoles.OWNER).isEmpty()) {
//                adminRoleEntity = roleRepository.save(Role.builder()
//                        .name(PredefinedRoles.OWNER)
//                        .description("Administrator role")
//                        .build()
//                );
//            }
//            if (roleRepository.findByRoleName(PredefinedRoles.MANAGER).isEmpty()) {
//                roleRepository.save(Role.builder()
//                        .name(PredefinedRoles.MANAGER)
//                        .description("Reader role")
//                        .build()
//                );
//            }
//            if (roleRepository.findByRoleName(PredefinedRoles.ACCOUNTANT).isEmpty()) {
//                roleRepository.save(Role.builder()
//                        .name(PredefinedRoles.ACCOUNTANT)
//                        .description("Content provider role")
//                        .build()
//                );
//            }
//            if (
//                    !accountRepository.existsByEmail(aDefault.getAdmin().getEmail())
//                            && !accountRepository.existsByUsername(aDefault.getAdmin().getUsername())
//            ) {
//                var accountCreationRequest = AccountCreationRequest.builder()
//                        .username(aDefault.getAdmin().getUsername())
//                        .email(aDefault.getAdmin().getEmail())
//                        .password(aDefault.getAdmin().getPassword())
//                        .build();
//                var createAccountCommand = accountWebMapper.toCommand(accountCreationRequest);
//
//                if (accountRepository.existsByUsername(aDefault.getAdmin().getUsername())) {
//                    return;
//                }
//                if (adminRoleEntity != null) {
//                    createAccountCommand.setInitialRole(adminRoleEntity.getName());
//                }
//                createAccountUseCase.execute(createAccountCommand);
//            }
//            log.info("Application initialized");
        };
    }

    @Autowired
    private ApplicationContext ctx;

    @PostConstruct
    public void checkControllers() {
        System.out.println("Controllers: " + ctx.getBeanNamesForAnnotation(Controller.class).length);
    }
}
