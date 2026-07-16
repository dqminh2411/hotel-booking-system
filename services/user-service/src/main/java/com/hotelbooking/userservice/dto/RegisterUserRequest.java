package com.hotelbooking.userservice.dto;

import com.hotelbooking.userservice.validation.PasswordMatch;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordMatch
public record RegisterUserRequest(
        @NotBlank @Email @Size(max = 255) String email,

        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "phone must contain 9 to 15 digits")
        String phone,

        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "password must contain uppercase, lowercase and number"
        )
        String password,
        
        @NotBlank
        String rePassword,

        @NotBlank @Size(max = 255) String fullName
) {
}
