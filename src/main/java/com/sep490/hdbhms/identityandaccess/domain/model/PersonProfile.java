package com.sep490.hdbhms.identityandaccess.domain.model;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Gender;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PersonProfile {
    Long id;
    Long userId;
    String fullName;
    LocalDate dob;
    @Builder.Default
    Gender gender = Gender.UNKNOWN;
    String phone;
    String email;
    String permanentAddress;
    Long portraitFileId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;

    public static PersonProfile create(
            Long userId,
            String fullName,
            LocalDate dob,
            String phone,
            String email,
            String permanentAddress,
            Long portraitFileId
    ) {
        return PersonProfile.builder()
                .userId(userId)
                .fullName(fullName)
                .dob(dob)
                .phone(phone)
                .email(email)
                .permanentAddress(permanentAddress)
                .portraitFileId(portraitFileId)
                .build();
    }

    public static PersonProfile createForStaff(
            Long userId,
            String fullName,
            String phone,
            String email
    ) {
        return PersonProfile.builder()
                .userId(userId)
                .fullName(fullName)
                .phone(phone)
                .email(email)
                .build();
    }

    public void setPhone(String phone) {
        this.phone = phone;
        updatedAt = LocalDateTime.now();
    }

    public void setEmail(String email) {
        this.email = email;
        updatedAt = LocalDateTime.now();
    }
}