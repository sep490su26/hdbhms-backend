package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.repository;

import com.sep490.hdbhms.identityandaccess.application.port.out.LoginHistoryRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.LoginHistory;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.LoginStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaLoginHistoryRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper.LoginHistoryPersistenceMapper;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.specifications.LoginHistorySpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataLoginHistoryRepository implements LoginHistoryRepository {
    JpaLoginHistoryRepository jpaLoginHistoryRepository;
    LoginHistoryPersistenceMapper loginHistoryPersistenceMapper;

    @Override
    public LoginHistory save(LoginHistory loginHistory) {
        return loginHistoryPersistenceMapper.toDomain(
                jpaLoginHistoryRepository.save(
                        loginHistoryPersistenceMapper.toEntity(loginHistory)
                )
        );
    }

    @Override
    public Page<LoginHistory> findAllByAccountId(Long accountId, List<String> statuses, Pageable pageable) {
        var ids = jpaLoginHistoryRepository.getAllIdsByAccountId(accountId);
        var spec = Specification
                .where(LoginHistorySpecifications.idIn(ids))
                .and(LoginHistorySpecifications.statusIn(toLoginStatusList(statuses)));
        return jpaLoginHistoryRepository.findAll(spec, pageable).map(loginHistoryPersistenceMapper::toDomain);
    }

    private List<LoginStatus> toLoginStatusList(List<String> loginStatus) {
        if (loginStatus == null || loginStatus.isEmpty()) {
            return Collections.emptyList();
        }
        return loginStatus.stream()
                .map(s -> {
                    try {
                        return LoginStatus.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND);
                    }
                }).toList();
    }
}
