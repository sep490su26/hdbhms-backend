package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.repository;

import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.specifications.AccountSpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataUserRepository implements UserRepository {
    JpaUserRepository jpaUserRepository;
    UserPersistenceMapper userPersistenceMapper;

//    @Qualifier("usernameBloomFilter")
//    RBloomFilter<String> usernameBloomFilter;
//    @Qualifier("emailBloomFilter")
//    RBloomFilter<String> emailBloomFilter;

    @EventListener(ApplicationReadyEvent.class)
    void fetch() {
//        findAll().forEach(account -> {
//            if (!usernameBloomFilter.contains(account.getUsername())) {
//                usernameBloomFilter.add(account.getUsername());
//            }
//            if (!emailBloomFilter.contains(account.getEmail())) {
//                emailBloomFilter.add(account.getEmail());
//            }
//        });
    }

    @Override
    public User save(User user) {
        return userPersistenceMapper.toDomain(
                jpaUserRepository.save(userPersistenceMapper.toEntity(user))
        );
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return jpaUserRepository.existsByPhoneAndDeletedAtIsNull(phone);
    }


    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpaUserRepository.findAll().stream()
                .map(userPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Page<User> findAll(List<Long> ids, List<String> roles, List<String> statuses, Pageable pageable) {
        Specification<UserEntity> accountEntitySpecification =
                Specification.where(AccountSpecifications.idIn(ids))
                        .and(AccountSpecifications.roleIn(toRoleEntityList(roles)))
                        .and(AccountSpecifications.statusIn(toAccountStatusList(statuses)));
        return jpaUserRepository.findAll(accountEntitySpecification, pageable)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {

        return jpaUserRepository.findByEmail(email)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return jpaUserRepository.findByPhone(phone)
                .map(userPersistenceMapper::toDomain);
    }


    @Override
    public List<Long> findIdsByFullText(String keyword) {
        return jpaUserRepository.findIdsByFullText(keyword);
    }

    @Override
    public Optional<User> findByPhoneOrEmailAndDeletedAtIsNull(String phone, String email) {
        return jpaUserRepository.findByPhoneOrEmailAndDeletedAtIsNull(phone, email)
                .map(userPersistenceMapper::toDomain);
    }

    private List<Role> toRoleEntityList(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(s -> {
                    try {
                        return Role.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new AppException(ApiErrorCode.UNDEFINED);
                    }
                }).toList();
    }

    private List<AccountStatus> toAccountStatusList(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Collections.emptyList();
        }
        return statuses.stream()
                .map(s -> {
                    try {
                        return AccountStatus.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new AppException(ApiErrorCode.UNDEFINED);
                    }
                }).toList();
    }
}
