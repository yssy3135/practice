package com.example.customvalidator.user.controller;


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UserValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserRegistrationConstraint {

    String message() default "Invalid UserRequest";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    String userType();

    String name();

    String phone();

}
