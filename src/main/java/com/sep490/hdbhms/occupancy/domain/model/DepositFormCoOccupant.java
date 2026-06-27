package com.sep490.hdbhms.occupancy.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositFormCoOccupant {
    Long id;
    String fullName;
    String phone;
    Integer displayOrder;
}
