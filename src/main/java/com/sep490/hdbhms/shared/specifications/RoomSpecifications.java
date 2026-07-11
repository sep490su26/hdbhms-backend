package com.sep490.hdbhms.shared.specifications;

import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomSpecifications {

    public static Specification<RoomEntity> idIn(List<Long> ids) {
        return (root, query, criteriaBuilder) -> {
            if (ids == null) {
                return criteriaBuilder.conjunction();
            }
            if (ids.isEmpty()) return criteriaBuilder.disjunction();
            return root.get("id").in(ids);
        };
    }

    public static Specification<RoomEntity> statusIn(RoomStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(
                        root.get("status"),
                        status
                );
    }

    public static Specification<RoomEntity> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    }

    public static Specification<RoomEntity> priceBetween(
            Long minPrice,
            Long maxPrice
    ) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return null;
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("listedPrice"), minPrice, maxPrice);
            } else if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("confirmedAt"), minPrice);
            } else {
                return cb.lessThanOrEqualTo(root.get("confirmedAt"), maxPrice);
            }
        };
    }
}
