package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.repository;

import com.sep490.hdbhms.identityandaccess.application.port.out.UserModificationHistoryRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.UserModificationHistory;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserModificationHistoryRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper.UserModificationHistoryPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataUserModificationRepository implements UserModificationHistoryRepository {
    JpaUserModificationHistoryRepository jpaUserModificationHistoryRepository;
    UserModificationHistoryPersistenceMapper userModificationHistoryPersistenceMapper;

    @Override
    public UserModificationHistory save(UserModificationHistory userModificationHistory) {
        return userModificationHistoryPersistenceMapper.toDomain(
                jpaUserModificationHistoryRepository.save(
                        userModificationHistoryPersistenceMapper.toEntity(
                                userModificationHistory
                        )
                )
        );
    }

    @Override
    public Instant getLatestUsernameModificationInstant(Long accountId) {
        return jpaUserModificationHistoryRepository
                .getLatestUsernameModificationTimestamp(accountId);
    }

    @Override
    public Instant getLatestEmailModificationInstant(Long accountId) {
        return jpaUserModificationHistoryRepository
                .getLatestEmailModificationTimestamp(accountId);
    }

    @Override
    public Instant getLatestPasswordResetInstant(Long accountId) {
        return jpaUserModificationHistoryRepository
                .getLatestPasswordResetTimestamp(accountId);
    }

    @Override
    public int getNumberOfPasswordResetOfTheDays(Long accountId, int days) {
        return jpaUserModificationHistoryRepository
                .getNumberOfPasswordResetOfTheDays(accountId, days);
    }

    @Override
    public Set<String> getTopLatestPasswordChangeValue(Long accountId, int top) {
        return jpaUserModificationHistoryRepository
                .getTopLatestPasswordChangeValuesByAccount_Id(accountId, top);
    }

    @Override
    public Optional<UserModificationHistory> findById(String id) {
        return jpaUserModificationHistoryRepository.findById(id)
                .map(userModificationHistoryPersistenceMapper::toDomain);
    }

    @Override
    public List<UserModificationHistory> findByAccountId(Long userId) {
        return jpaUserModificationHistoryRepository.findByUser_Id(userId).stream()
                .map(userModificationHistoryPersistenceMapper::toDomain)
                .toList();
    }
}
