package com.sep490.hdbhms.notification.infrastructure.persistence.jpa;

import com.sep490.hdbhms.notification.infrastructure.persistence.entity.UserMobileDeviceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaUserMobileDeviceTokenRepository extends JpaRepository<UserMobileDeviceTokenEntity, Long> {
    @Query("SELECT t.token FROM UserMobileDeviceTokenEntity t WHERE t.user.id = :userId AND t.isActive = true")
    List<String> findActiveTokensByUserId(@Param("userId") Long userId);

    java.util.Optional<UserMobileDeviceTokenEntity> findByUser_IdAndToken(Long userId, String token);

    void deleteByToken(String token);
}
