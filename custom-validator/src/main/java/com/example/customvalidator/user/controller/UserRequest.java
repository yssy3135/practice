package com.example.customvalidator.user.controller;

import lombok.*;

import javax.validation.constraints.NotEmpty;

@UserRegistrationConstraint(name = "name",phone = "phone", userType = "userType")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class UserRequest {

    Long id;

    @NotEmpty
    String password;

    @NotEmpty
    String username;

    String name;

    String phone;

    String userType;

}
