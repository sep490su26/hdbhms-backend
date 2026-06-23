package com.sep490.hdbhms.shared.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class VietnamesePhoneValidator implements ConstraintValidator<VietnamesePhone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String cleaned = value.replaceAll("[\\s.\\-()]", "");
        if (cleaned.matches("0[35789]\\d{8}")) {
            return true;
        }
        return cleaned.matches("\\+84[35789]\\d{8}");
    }
}