package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountLoginHistoryQuery;
import com.sep490.hdbhms.identityandaccess.domain.model.LoginHistory;
import org.springframework.data.domain.Page;

public interface GetAccountLoginHistoryListUseCase {
    Page<LoginHistory> execute(GetAccountLoginHistoryQuery command);
}
