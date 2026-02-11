package com.example.zylo.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank(message = "name is required")
    @Size(min = 3, message = "name must be at least 3 characters")
    private String name;


    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
