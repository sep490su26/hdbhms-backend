package com.sep490.hdbhms.identityandaccess.application.port.out;

import com.sep490.hdbhms.identityandaccess.domain.model.UserModificationHistory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserModificationHistoryRepository {
    UserModificationHistory save(UserModificationHistory userModificationHistory);

    Instant getLatestUsernameModificationInstant(Long accountId);

    Instant getLatestEmailModificationInstant(Long accountId);

    Instant getLatestPasswordResetInstant(Long accountId);

    int getNumberOfPasswordResetOfTheDays(Long accountId, int days);

    Set<String> getTopLatestPasswordChangeValue(Long accountId, int top);

    Optional<UserModificationHistory> findById(String id);

    List<UserModificationHistory> findByAccountId(Long accountId);
}
