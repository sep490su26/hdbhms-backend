package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.IntrospectTokenQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.IntrospectTokenUseCase;
import com.sep490.hdbhms.identityandaccess.domain.model.Introspect;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.TokenProvider;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.nimbusds.jose.JOSEException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IntrospectTokenService implements IntrospectTokenUseCase {
    TokenProvider tokenProvider;

    @Override
    public Introspect execute(IntrospectTokenQuery command) {
        boolean isValid = false;
        try {
            tokenProvider.verifyToken(command.token(), false);
            isValid = true;
        } catch (AppException ignored) {
        } catch (ParseException | JOSEException e) {
            throw new AppException(ApiErrorCode.INVALID_JWT_TOKEN);
        }
        return new Introspect(isValid);
    }
}
