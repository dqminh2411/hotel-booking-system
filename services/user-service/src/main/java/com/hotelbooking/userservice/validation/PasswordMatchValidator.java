package com.hotelbooking.userservice.validation;

import com.hotelbooking.userservice.dto.RegisterUserRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator
        implements ConstraintValidator<PasswordMatch, RegisterUserRequest> {

    @Override
    public boolean isValid(RegisterUserRequest request,
                           ConstraintValidatorContext context) {

        if (request.password() == null || request.rePassword() == null) {
            return true; // @NotBlank handles null/empty
        }

        if (request.password().equals(request.rePassword())) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        context.buildConstraintViolationWithTemplate("Passwords do not match")
            .addPropertyNode("rePassword")
            .addConstraintViolation();

        return false;
    }
}
