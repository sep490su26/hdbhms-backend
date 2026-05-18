package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.IntrospectTokenQuery;
import com.sep490.hdbhms.identityandaccess.domain.model.Introspect;

public interface IntrospectTokenUseCase {
    Introspect execute(IntrospectTokenQuery command);
}
