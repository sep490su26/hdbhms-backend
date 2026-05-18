package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserStatusLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserStatusLogRepository extends JpaRepository<UserStatusLogEntity, Long> {
}
