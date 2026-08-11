package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.PromoteRoleCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.PromoteRoleUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromoteRoleService implements PromoteRoleUseCase {
    UserRepository userRepository;

    @Override
    public void execute(PromoteRoleCommand command) {
        if (command.toRole() != Role.MANAGER && command.toRole() != Role.ACCOUNTANT) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        user.assignRole(command.toRole());
        //TODO: Audit the role promotion
        userRepository.save(user);
    }
}
