package com.sep490.hdbhms.shared.specifications;

import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.VisitRequestEntity;
import jakarta.persistence.criteria.JoinType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VisitRequestSpecifications {

    public static Specification<VisitRequestEntity> idIn(List<Long> ids) {
        return (root, query, criteriaBuilder) -> {
            if (ids == null || ids.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("id").in(ids);
        };
    }

    public static Specification<VisitRequestEntity> hasPropertyCode(String propertyCode) {
        return (root, query, cb) -> {
            if (propertyCode == null || propertyCode.isBlank()) return null;
            var propertyJoin = root.join("property", JoinType.INNER);
            return cb.equal(propertyJoin.get("propertyCode"), propertyCode);
        };
    }

    public static Specification<VisitRequestEntity> hasRoomCode(String roomCode) {
        return (root, query, cb) -> {
            if (roomCode == null || roomCode.isBlank()) return null;
            var roomJoin = root.join("room", JoinType.LEFT);
            return cb.equal(roomJoin.get("roomCode"), roomCode);
        };
    }

    public static Specification<VisitRequestEntity> hasPropertyId(Long propertyId) {
        return (root, query, cb) -> {
            if (propertyId == null) return null;
            var propertyJoin = root.join("property", JoinType.INNER);
            return cb.equal(propertyJoin.get("id"), propertyId);
        };
    }

    public static Specification<VisitRequestEntity> hasRoomId(Long roomId) {
        return (root, query, cb) -> {
            if (roomId == null) return null;
            var roomJoin = root.join("room", JoinType.LEFT);
            return cb.equal(roomJoin.get("id"), roomId);
        };
    }

    public static Specification<VisitRequestEntity> hasStatus(VisitRequestStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<VisitRequestEntity> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<VisitRequestEntity> preferredStartBetween(
            LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) {
                return cb.between(root.get("preferredStart"), from, to);
            } else if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("preferredStart"), from);
            } else {
                return cb.lessThanOrEqualTo(root.get("preferredStart"), to);
            }
        };
    }
}
