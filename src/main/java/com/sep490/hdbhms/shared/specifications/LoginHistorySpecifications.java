package com.sep490.hdbhms.shared.specifications;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.LoginMethod;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.LoginStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.LoginHistoryEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginHistorySpecifications {
    public static Specification<LoginHistoryEntity> idIn(List<Long> ids) {
        return (root, query, criteriaBuilder) -> {
            if (ids == null || ids.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("id").in(ids);
        };
    }

    public static Specification<LoginHistoryEntity> statusIn(List<LoginStatus> statuses) {
        return (root, query, criteriaBuilder) ->
                statuses == null || statuses.isEmpty() ? criteriaBuilder.conjunction()
                        : root.get("status").in(statuses);
    }

    public static Specification<LoginHistoryEntity> methodIn(List<LoginMethod> methods) {
        return (root, query, criteriaBuilder) ->
                methods == null || methods.isEmpty() ? criteriaBuilder.conjunction()
                        : root.get("method").in(methods);
    }
}
