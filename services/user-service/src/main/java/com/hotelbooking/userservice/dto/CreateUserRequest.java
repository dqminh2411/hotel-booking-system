package com.hotelbooking.userservice.dto;

import java.util.UUID;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank @Email @Size(max = 255) String email,

        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "phone must contain 9 to 15 digits")
        String phone,

        @NotBlank @Size(max = 255) String fullName
) {
}
