package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountByEmailQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountByIdQuery;
import com.sep490.hdbhms.identityandaccess.domain.model.User;

public interface GetUserUseCase {
    User getByEmail(GetAccountByEmailQuery command);
    User getById(GetAccountByIdQuery command);
}
