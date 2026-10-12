package com.example.vbtest.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating and updating a user.
 */
public record AppUserRequest(
        @NotBlank(message = "username must not be blank")
        @Size(max = 100, message = "username must be at most 100 characters")
        String username,

        @NotBlank(message = "password must not be blank")
        @Size(max = 100, message = "password must be at most 100 characters")
        String password) {
}
