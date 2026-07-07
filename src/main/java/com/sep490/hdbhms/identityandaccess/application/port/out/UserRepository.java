package com.sep490.hdbhms.identityandaccess.application.port.out;

import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsAnOwnerAccount();

    List<User> findAll();

    Optional<User> findById(Long id);

    Page<User> findAll(List<Long> ids, Role role, AccountStatus status, Pageable pageable);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    List<Long> findIdsByFullText(String keyword);

    List<Long> findIdsByRolesAndStatus(Collection<Role> roles, AccountStatus status);

    Optional<User> findByPhoneOrEmailAndDeletedAtIsNull(String phone, String email);

    Optional<User> findOwner();
}
