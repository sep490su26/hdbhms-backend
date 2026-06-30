package com.sep490.hdbhms.shared.specifications;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PermissionRequestStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PermissionRequestEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PermissionRequestSpecifications {
    public static Specification<PermissionRequestEntity> idIn(List<Long> ids) {
        return (root, query, criteriaBuilder) -> {
            if (ids == null || ids.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("id").in(ids);
        };
    }

    public static Specification<PermissionRequestEntity> statusIn(PermissionRequestStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(root.get("status"), status);
    }
}
