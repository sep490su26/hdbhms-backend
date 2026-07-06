package com.sep490.hdbhms.shared.specifications;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountSpecifications {

    public static Specification<UserEntity> idIn(List<Long> ids) {
        return (root, query, criteriaBuilder) -> {
            if (ids == null || ids.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("id").in(ids);
        };
    }

    public static Specification<UserEntity> statusIn(AccountStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<UserEntity> roleIn(Role role) {
        return (root, query, criteriaBuilder) ->
                role == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("role"), role);
    }

    public static Specification<UserEntity> rolesIn(List<Role> roles) {
        return (root, query, criteriaBuilder) -> {
            if (roles == null || roles.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("role").in(roles);
        };
    }
}
