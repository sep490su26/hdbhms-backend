package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountLoginHistoryQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.GetAccountLoginHistoryListUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.LoginHistoryRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.LoginHistory;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetLoginHistoryListService implements GetAccountLoginHistoryListUseCase {
    LoginHistoryRepository loginHistoryRepository;

    @Override
    public Page<LoginHistory> execute(GetAccountLoginHistoryQuery command) {
        return loginHistoryRepository.findAllByAccountId(
                command.accountId(),
                command.statuses(),
                command.pageable()
        );
    }
}
