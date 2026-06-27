package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantRole;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractOccupant {
    Long id;
    Long contractId;
    Long tenantId;
    Long tenantProfileId;
    @Builder.Default
    OccupantRole occupantRole = OccupantRole.CO_OCCUPANT;
    LocalDate moveInDate;
    LocalDate moveOutDate;
    @Builder.Default
    OccupantStatus status = OccupantStatus.ACTIVE;
    String disabledReason;
    Long disabledBy;
    LocalDateTime disabledAt;
    LocalDateTime createdAt;

    public void moveOut(LocalDate moveOutDate) {
        this.status = OccupantStatus.MOVED_OUT;
        this.moveOutDate = moveOutDate;
    }
}
