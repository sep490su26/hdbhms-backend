package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.repository;

import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper.UserPersistenceMapper;
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

import java.util.Collection;
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
    public boolean existsAnOwnerAccount() {
        return jpaUserRepository.existsByRole(Role.OWNER);
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
    public Page<User> findAll(
            List<Long> ids,
            List<Role> roles,
            AccountStatus status,
            Pageable pageable
    ) {
        Specification<UserEntity> specification =
                Specification.where(AccountSpecifications.idIn(ids))
                        .and(AccountSpecifications.rolesIn(roles))
                        .and(AccountSpecifications.statusIn(status))
                        .and((root, query, cb) -> cb.notEqual(root.get("role"), Role.OWNER));
        return jpaUserRepository.findAll(specification, pageable)
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
    public List<Long> findIdsByRolesAndStatus(Collection<Role> roles, AccountStatus status) {
        return jpaUserRepository.findIdsByRolesAndStatus(roles, status);
    }

    @Override
    public Optional<User> findByPhoneOrEmailAndDeletedAtIsNull(String phone, String email) {
        return jpaUserRepository.findByPhoneOrEmailAndDeletedAtIsNull(phone, email)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findOwner() {
        return jpaUserRepository.findByRole(Role.OWNER)
                .map(userPersistenceMapper::toDomain);
    }
}
