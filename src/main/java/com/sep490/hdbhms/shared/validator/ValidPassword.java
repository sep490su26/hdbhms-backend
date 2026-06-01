package com.sep490.hdbhms.shared.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Size(min = 8)
@Pattern(regexp = ".*[a-zA-Z].*")
@Pattern(regexp = ".*\\d.*")
@ReportAsSingleViolation
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
public @interface ValidPassword {
    String message() default "Password must be at least 8 characters, contain a letter and a digit";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
