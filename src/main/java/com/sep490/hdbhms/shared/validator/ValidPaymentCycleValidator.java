package com.sep490.hdbhms.shared.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPaymentCycleValidator implements ConstraintValidator<ValidPaymentCycle, Integer> {
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return value == 1 || value == 3;
    }
}
