package com.sep490.hdbhms.maintenance.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceReview;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceReviewEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaintenanceReviewPersistenceMapper {
    JpaMaintenanceTicketRepository jpaMaintenanceTicketRepository;
    JpaUserRepository jpaUserRepository;

    public MaintenanceReview toDomain(MaintenanceReviewEntity entity) {
        if (entity == null) return null;
        return MaintenanceReview.builder()
                .id(entity.getId())
                .ticketId(entity.getTicket() != null ? entity.getTicket().getId() : null)
                .reviewerUserId(entity.getReviewerUser() != null ? entity.getReviewerUser().getId() : null)
                .rating(entity.getRating())
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public MaintenanceReviewEntity toEntity(MaintenanceReview domain) {
        if (domain == null) return null;
        return MaintenanceReviewEntity.builder()
                .id(domain.getId())
                .ticket(domain.getTicketId() != null
                        ? jpaMaintenanceTicketRepository.getReferenceById(domain.getTicketId())
                        : null)
                .reviewerUser(domain.getReviewerUserId() != null
                        ? jpaUserRepository.getReferenceById(domain.getReviewerUserId())
                        : null)
                .rating(domain.getRating())
                .comment(domain.getComment())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
