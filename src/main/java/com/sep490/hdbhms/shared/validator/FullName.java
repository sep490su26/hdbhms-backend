package com.sep490.hdbhms.shared.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FullNameValidator.class)
public @interface FullName {
    String message() default "Please enter your full name (at least first and last name)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
