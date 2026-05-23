package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountsQuery;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import org.springframework.data.domain.Page;

public interface GetListAccountsUseCase {
    Page<User> execute(GetAccountsQuery command);
}
