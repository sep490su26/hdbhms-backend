package com.sep490.hdbhms.shared.specifications;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.LoginMethod;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.LoginStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.LoginHistoryEntity;
import com.sep490.hdbhms.maintenance.domain.value_objects.MaintenanceTicketStatus;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MaintenanceTicketSpecifications {
    public static Specification<MaintenanceTicketEntity> idIn(List<Long> ids) {
        return (root, query, criteriaBuilder) -> {
            if (ids == null || ids.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("id").in(ids);
        };
    }

    public static Specification<MaintenanceTicketEntity> statusIn(MaintenanceTicketStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(root.get("status"), status);
    }

//    public static Specification<LoginHistoryEntity> typeIn(List<LoginMethod> methods) {
//        return (root, query, criteriaBuilder) ->
//                methods == null || methods.isEmpty() ? criteriaBuilder.conjunction()
//                        : root.get("method").in(methods);
//    }
}
