package com.sep490.hdbhms.identityandaccess.domain.model;

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
public class EmergencyContact {
    Long id;
    Long tenantProfileId;
    String fullName;
    String relationship;
    String phone;
    LocalDateTime createdAt;

    public static EmergencyContact create(String fullName, String relationship, String phone, Long tenantProfileId) {
        return EmergencyContact.builder()
                .fullName(fullName)
                .relationship(relationship)
                .phone(phone)
                .tenantProfileId(tenantProfileId)
                .build();
    }
}
