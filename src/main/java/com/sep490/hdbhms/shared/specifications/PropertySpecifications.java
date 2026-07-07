package com.sep490.hdbhms.shared.specifications;

import com.sep490.hdbhms.occupancy.domain.valueObjects.PropertyStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertySpecifications {
    public static Specification<PropertyEntity> idIn(List<Long> ids) {
        return (root, query, criteriaBuilder) -> {
            if (ids == null || ids.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("id").in(ids);
        };
    }

    public static Specification<PropertyEntity> statusIn(PropertyStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(root.get("status"), status);
    }
}
