package com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.CreateUserCommand;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.model.LoginHistory;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request.UserCreationRequest;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.UserResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.LoginHistoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface UserWebMapper {

    CreateUserCommand toCommand(UserCreationRequest request);

    UserResponse toAccountResponse(User user);

    LoginHistoryResponse toLoginHistoryResponse(LoginHistory loginHistory);
}
