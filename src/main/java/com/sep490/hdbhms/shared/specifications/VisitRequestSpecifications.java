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
            var propertyJoin = root.join("property", JoinType.LEFT);
            var roomJoin = root.join("room", JoinType.LEFT);
            var roomPropertyJoin = roomJoin.join("property", JoinType.LEFT);
            return cb.equal(
                    cb.coalesce(roomPropertyJoin.get("propertyCode"), propertyJoin.get("propertyCode")),
                    propertyCode
            );
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
            var propertyJoin = root.join("property", JoinType.LEFT);
            var roomJoin = root.join("room", JoinType.LEFT);
            var roomPropertyJoin = roomJoin.join("property", JoinType.LEFT);
            return cb.equal(
                    cb.coalesce(roomPropertyJoin.get("id"), propertyJoin.get("id")),
                    propertyId
            );
        };
    }

    public static Specification<VisitRequestEntity> hasAnyPropertyId(List<Long> propertyIds) {
        return (root, query, cb) -> {
            if (propertyIds == null) return null;
            if (propertyIds.isEmpty()) return cb.disjunction();
            var propertyJoin = root.join("property", JoinType.LEFT);
            var roomJoin = root.join("room", JoinType.LEFT);
            var roomPropertyJoin = roomJoin.join("property", JoinType.LEFT);
            return cb.coalesce(roomPropertyJoin.get("id"), propertyJoin.get("id")).in(propertyIds);
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
        return (root, query, cb) -> {
            if (status == null) return null;
            if (status == VisitRequestStatus.NOT_VIEWED) {
                return cb.or(cb.equal(root.get("status"), status), cb.isNull(root.get("status")));
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<VisitRequestEntity> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<VisitRequestEntity> deleted() {
        return (root, query, cb) -> cb.isNotNull(root.get("deletedAt"));
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
