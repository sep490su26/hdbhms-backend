package com.sep490.hdbhms.shared.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class FullNameValidator implements ConstraintValidator<FullName, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        String trimmed = value.trim();
        String[] parts = trimmed.split("\\s+");
        return parts.length >= 2 && Arrays.stream(parts).noneMatch(String::isEmpty);
    }
}