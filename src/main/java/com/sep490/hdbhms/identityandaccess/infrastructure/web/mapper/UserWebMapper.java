package com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.CreateUserCommand;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.model.LoginHistory;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request.AccountCreationRequest;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.AccountResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.LoginHistoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface UserWebMapper {

    CreateUserCommand toCommand(AccountCreationRequest request);

    AccountResponse toAccountResponse(User user);

    LoginHistoryResponse toLoginHistoryResponse(LoginHistory loginHistory);
}
