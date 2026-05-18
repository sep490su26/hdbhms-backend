package com.sep490.hdbhms.shared.specifications;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
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

    public static Specification<UserEntity> statusIn(List<AccountStatus> statuses) {
        return (root, query, criteriaBuilder) ->
                statuses == null || statuses.isEmpty() ? criteriaBuilder.conjunction() :
                        root.get("status").in(statuses);
    }

    public static Specification<UserEntity> roleIn(List<Role> role) {
        return (root, query, criteriaBuilder) ->
                role == null || role.isEmpty() ? criteriaBuilder.conjunction() :
                        root.get("role").in(role);
    }
}
