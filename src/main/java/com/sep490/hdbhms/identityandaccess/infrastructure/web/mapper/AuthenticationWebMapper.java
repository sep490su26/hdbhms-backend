package com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.*;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.IntrospectTokenQuery;
import com.sep490.hdbhms.identityandaccess.domain.model.Authentication;
import com.sep490.hdbhms.identityandaccess.domain.model.Introspect;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request.*;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.AuthenticationResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.IntrospectResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface AuthenticationWebMapper {
    AuthenticationResponse toResponse(Authentication authentication);

    IntrospectResponse toResponse(Introspect introspect);

    LoginCommand toCommand(AuthenticationRequest request);

    IntrospectTokenQuery toCommand(IntrospectRequest request);

    RefreshAccessTokenCommand toCommand(RefreshRequest request);

    LogoutCommand toCommand(LogoutRequest request);

    RequestResetPasswordCommand toCommand(AccountPasswordResetRequest request);

    ResetPasswordCommand toCommand(ResetPasswordRequest request);
}
