package com.sep490.hdbhms.booking.infrastructure.web.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchDepositCheckoutRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsApplicantUnderEighteenOnDobField() {
        BatchDepositCheckoutRequest request = new BatchDepositCheckoutRequest();
        request.setDob(LocalDate.now().minusYears(17));

        assertTrue(validator.validate(request).stream().anyMatch(violation ->
                violation.getPropertyPath().toString().equals("dob")
                        && violation.getMessage().equals("Phải từ 18 tuổi trở lên")
        ));
    }

    @Test
    void acceptsApplicantOnEighteenthBirthday() {
        BatchDepositCheckoutRequest request = new BatchDepositCheckoutRequest();
        request.setDob(LocalDate.now().minusYears(18));

        assertFalse(validator.validate(request).stream().anyMatch(violation ->
                violation.getPropertyPath().toString().equals("dob")
                        && violation.getMessage().equals("Phải từ 18 tuổi trở lên")
        ));
    }
}
