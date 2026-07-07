package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.valueObjects.ContractEventType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractEvent {
    Long id;
    Long contractId;
    ContractEventType eventType;
    byte[] eventData;
    Long createdById;
    LocalDateTime createdAt;
}
