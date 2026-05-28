package com.sep490.hdbhms.shared.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AgeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Age {
    String message() default "You must be at least 18 years old";

    int age() default 18;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}