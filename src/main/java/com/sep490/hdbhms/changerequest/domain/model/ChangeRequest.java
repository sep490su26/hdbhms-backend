package com.sep490.hdbhms.changerequest.domain.model;

import com.sep490.hdbhms.changerequest.domain.valueObjects.AssignedRole;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequestType;
import com.sep490.hdbhms.changerequest.domain.valueObjects.RequesterRole;
import com.sep490.hdbhms.changerequest.domain.valueObjects.TargetType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangeRequest {
    Long id;
    String requestCode;
    RequestType requestType;
    Long requesterId;
    RequesterRole requesterRole;
    TargetType targetType;
    Long targetId;
    String title;
    String description;
    String requestPayload;
    Long evidenceFileId;
    AssignedRole assignedRole;
    Long assignedTo;
    @Builder.Default
    RequestStatus status = RequestStatus.PENDING;
    String resolutionNote;
    Long resolvedBy;
    LocalDateTime resolvedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public void approve(Long managerId) {
        if (this.status != RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved.");
        }
        this.status = RequestStatus.APPROVED;
        this.resolvedBy = managerId;
        this.resolvedAt = LocalDateTime.now();
    }

    public void reject(Long managerId, String note) {
        if (this.status != RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected.");
        }
        this.status = RequestStatus.REJECTED;
        this.resolvedBy = managerId;
        this.resolutionNote = note;
        this.resolvedAt = LocalDateTime.now();
    }
}
