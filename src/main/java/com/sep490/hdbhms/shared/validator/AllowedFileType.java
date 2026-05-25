package com.sep490.hdbhms.shared.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AllowedFileTypeValidator.class)
public @interface AllowedFileType {
    String message() default "Invalid file type";

    String[] allowedMimeTypes() default {};

    String[] allowedExtensions() default {};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
