package com.sep490.hdbhms.identityandaccess.application.port.out;

import com.sep490.hdbhms.identityandaccess.domain.model.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LoginHistoryRepository {
    LoginHistory save(LoginHistory loginHistory);

    Page<LoginHistory> findAllByAccountId(Long accountId, List<String> statuses, Pageable pageable);
}
