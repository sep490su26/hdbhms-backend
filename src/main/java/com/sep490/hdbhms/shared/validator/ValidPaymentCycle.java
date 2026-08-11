package com.sep490.hdbhms.shared.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPaymentCycleValidator.class)
public @interface ValidPaymentCycle {
    String message() default "Chu kỳ thanh toán chỉ nhận 1 hoặc 3 tháng";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
