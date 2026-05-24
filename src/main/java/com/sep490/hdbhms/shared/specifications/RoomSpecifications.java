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
            if (ids == null || ids.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
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

    public static Specification<RoomEntity> priceBetween(
            Long minPrice,
            Long maxPrice
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(
                        root.get("listedPrice"),
                        minPrice,
                        maxPrice
                );
    }
}
