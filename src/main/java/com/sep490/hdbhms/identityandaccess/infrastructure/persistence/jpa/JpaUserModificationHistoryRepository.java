package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserModificationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface JpaUserModificationHistoryRepository extends JpaRepository<UserModificationHistoryEntity, String> {
    List<UserModificationHistoryEntity> findByUser_Id(Long accountId);

    @Query(value = """
                SELECT changed_at FROM sep490.hdbhms.account_modification_history WHERE account_id = ?1 AND type = 'USERNAME' ORDER BY changed_at DESC LIMIT 1
            """, nativeQuery = true)
    Instant getLatestUsernameModificationTimestamp(Long accountId);

    @Query(value = """
                SELECT changed_at FROM sep490.hdbhms.account_modification_history WHERE account_id = ?1 AND type = 'EMAIL' ORDER BY changed_at DESC LIMIT 1
            """, nativeQuery = true)
    Instant getLatestEmailModificationTimestamp(Long accountId);

    @Query(value = """
                SELECT old_value FROM sep490.hdbhms.account_modification_history WHERE account_id = ?1 AND type = 'PASSWORD_CHANGE' ORDER BY changed_at DESC LIMIT ?2
            """, nativeQuery = true)
    Set<String> getTopLatestPasswordChangeValuesByAccount_Id(Long accountId, int top);

    @Query(value = """
                SELECT changed_at FROM sep490.hdbhms.account_modification_history WHERE account_id = ?1 AND type = 'PASSWORD_RESET' ORDER BY changed_at DESC LIMIT 1
            """, nativeQuery = true)
    Instant getLatestPasswordResetTimestamp(Long accountId);

    @Query(value = """
                SELECT COUNT(account_modification_history_id) FROM sep490.hdbhms.account_modification_history WHERE account_id = ?1 AND type = 'PASSWORD_RESET' AND changed_at >= CURDATE() AND changed_at < CURDATE() + INTERVAL ?2 DAY
            """, nativeQuery = true)
    int getNumberOfPasswordResetOfTheDays(Long accountId, int days);
}
