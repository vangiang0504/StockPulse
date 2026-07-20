package com.training.starter.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email(message = "Email must be valid")
        String email,

        @Size(min = 1, max = 100)
        String fullName,

        Boolean active
) {}
