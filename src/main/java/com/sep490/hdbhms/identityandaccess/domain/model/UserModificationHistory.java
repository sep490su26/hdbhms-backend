package com.sep490.hdbhms.identityandaccess.domain.model;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.ModificationType;
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
public class UserModificationHistory {
    final String id;
    Long accountId;

    ModificationType type;

    String oldValue;
    String newValue;

    final LocalDateTime changedAt;

    public static UserModificationHistory newUserModificationHistory(
            Long accountId,
            ModificationType type,
            String oldValue,
            String newValue
    ) {
        return UserModificationHistory.builder()
                .accountId(accountId)
                .type(type)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
    }
}
