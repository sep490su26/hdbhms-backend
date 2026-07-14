package com.sep490.hdbhms.shared.specifications;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositAgreementEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DepositAgreementSpecifications {
    public static Specification<DepositAgreementEntity> idIn(List<Long> ids) {
        return (root, query, criteriaBuilder) -> {
            if (ids == null || ids.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("id").in(ids);
        };
    }

    public static Specification<DepositAgreementEntity> statusIn(DepositAgreementStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<DepositAgreementEntity> statusesIn(List<DepositAgreementStatus> statuses) {
        return (root, query, criteriaBuilder) -> {
            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<DepositAgreementEntity> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            var depositForm = root.join("depositForm", JoinType.LEFT);
            var room = root.join("room", JoinType.INNER);
            return cb.or(
                    cb.like(cb.lower(root.get("depositCode")), pattern),
                    cb.like(cb.lower(depositForm.get("fullName")), pattern),
                    cb.like(cb.lower(depositForm.get("phone")), pattern),
                    cb.like(cb.lower(room.get("roomCode")), pattern)
            );
        };
    }

    public static Specification<DepositAgreementEntity> onFloor(Long floorId) {
        return (root, query, cb) -> floorId == null
                ? cb.conjunction()
                : cb.equal(root.get("room").get("floor").get("id"), floorId);
    }

    public static Specification<DepositAgreementEntity> signingDateBetween(
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) {
                return cb.between(root.get("confirmedAt"), from, to);
            } else if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("confirmedAt"), from);
            } else {
                return cb.lessThanOrEqualTo(root.get("confirmedAt"), to);
            }
        };
    }
}
