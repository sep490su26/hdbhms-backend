package com.sep490.hdbhms.maintenance.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceReview {
    Long id;
    Long ticketId;
    Long reviewerUserId;
    Integer rating;
    String comment;
    LocalDateTime createdAt;
}