package com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.*;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.IntrospectTokenQuery;
import com.sep490.hdbhms.identityandaccess.domain.model.Authentication;
import com.sep490.hdbhms.identityandaccess.domain.model.Introspect;
import com.sep490.hdbhms.identityandaccess.domain.model.MobileAuthentication;
import com.sep490.hdbhms.identityandaccess.domain.model.WebAuthentication;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request.*;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.AuthenticationResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.IntrospectResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface AuthenticationWebMapper {
    default AuthenticationResponse toResponse(Authentication authentication) {
        switch (authentication) {
            case null -> {
                return null;
            }
            case WebAuthentication webAuthentication -> {
                return AuthenticationResponse.builder()
                        .token(webAuthentication.token())
                        .role(webAuthentication.role())
                        .authorized(webAuthentication.authorized())
                        .build();
            }
            case MobileAuthentication mobileAuthentication -> {
                return AuthenticationResponse.builder()
                        .token(mobileAuthentication.token())
                        .sessionId(mobileAuthentication.sessionId())
                        .role(mobileAuthentication.role())
                        .tenantId(mobileAuthentication.tenantId())
                        .propertyId(mobileAuthentication.propertyId())
                        .authorized(mobileAuthentication.authorized())
                        .build();
            }
            default -> {
            }
        }
        return null;
    }

    IntrospectResponse toResponse(Introspect introspect);

    LoginCommand toCommand(AuthenticationRequest request);

    IntrospectTokenQuery toCommand(IntrospectRequest request);

    RefreshAccessTokenCommand toCommand(RefreshRequest request);

    LogoutCommand toCommand(LogoutRequest request);

    RequestResetPasswordCommand toCommand(AccountPasswordResetRequest request);

    ResetPasswordCommand toCommand(ResetPasswordRequest request);
}
