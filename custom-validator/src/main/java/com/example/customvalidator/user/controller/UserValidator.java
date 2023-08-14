package com.example.customvalidator.user.controller;

import org.springframework.util.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class UserValidator implements ConstraintValidator<UserRegistrationConstraint, UserRequest> {

    @Override
    public boolean isValid(UserRequest userRequest, ConstraintValidatorContext context) {

        String userType = userRequest.getUserType();
        String name = userRequest.getName();
        String phone = userRequest.getPhone();

        if(!userType.equals("manager")){
            return true;
        }

        return StringUtils.hasText(name) && StringUtils.hasText(phone);
    }




}
