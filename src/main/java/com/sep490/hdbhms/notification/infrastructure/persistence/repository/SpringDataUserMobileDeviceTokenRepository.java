package com.sep490.hdbhms.notification.infrastructure.persistence.repository;

import com.sep490.hdbhms.notification.application.port.out.UserMobileDeviceTokenRepository;
import com.sep490.hdbhms.notification.domain.model.UserMobileDeviceToken;
import com.sep490.hdbhms.notification.infrastructure.persistence.jpa.JpaUserMobileDeviceTokenRepository;
import com.sep490.hdbhms.notification.infrastructure.persistence.mapper.UserMobileDeviceTokenPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataUserMobileDeviceTokenRepository implements UserMobileDeviceTokenRepository {
    JpaUserMobileDeviceTokenRepository jpaUserMobileDeviceTokenRepository;
    UserMobileDeviceTokenPersistenceMapper userMobileDeviceTokenPersistenceMapper;

    @Override
    public UserMobileDeviceToken save(UserMobileDeviceToken mobileDeviceToken) {
        return userMobileDeviceTokenPersistenceMapper.toDomain(
                jpaUserMobileDeviceTokenRepository.save(
                        userMobileDeviceTokenPersistenceMapper.toEntity(
                                mobileDeviceToken
                        )
                )
        );
    }

    @Override
    public List<String> findActiveTokenByUserId(Long userId) {
        return jpaUserMobileDeviceTokenRepository.findActiveTokensByUserId(userId);
    }

    @Override
    public java.util.Optional<UserMobileDeviceToken> findByUserIdAndToken(Long userId, String token) {
        return jpaUserMobileDeviceTokenRepository.findByUser_IdAndToken(userId, token)
                .map(userMobileDeviceTokenPersistenceMapper::toDomain);
    }

    @Override
    public void deleteByToken(String token) {
        jpaUserMobileDeviceTokenRepository.deleteByToken(token);
    }
}
