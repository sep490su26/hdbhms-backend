package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.LoginHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaLoginHistoryRepository extends JpaRepository<LoginHistoryEntity, Long>, JpaSpecificationExecutor<LoginHistoryEntity> {
    @Query(value = """
                SELECT login_history_id FROM sep490.hdbhms.login_history WHERE account_id = ?1
            """, nativeQuery = true)
    List<String> getAllIdsByAccountId(Long accountId);
}
