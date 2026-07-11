package com.sep490.hdbhms.identityverification.domain.model;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Gender;

import java.time.LocalDate;

public record CccdExtractedIdentity(
        String rawPayload,
        String idNumber,
        String oldIdNumber,
        String fullName,
        LocalDate dob,
        Gender gender,
        String address,
        LocalDate issuedDate
) {
}
