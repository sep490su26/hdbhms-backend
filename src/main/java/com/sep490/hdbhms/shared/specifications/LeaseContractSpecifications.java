package com.sep490.hdbhms.shared.specifications;

import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LeaseContractSpecifications {
    public static Specification<LeaseContractEntity> idIn(List<Long> ids) {
        return (root, query, criteriaBuilder) -> {
            if (ids == null || ids.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("id").in(ids);
        };
    }

    public static Specification<LeaseContractEntity> statusIn(LeaseStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<LeaseContractEntity> signingDateBetween(
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) {
                return cb.between(root.get("signedAt"), from, to);
            } else if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("signedAt"), from);
            } else {
                return cb.lessThanOrEqualTo(root.get("signedAt"), to);
            }
        };
    }
}
