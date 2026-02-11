package com.example.zylo.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @NotBlank(message = "id is required")
    @NotNull
    private Long id;

    @NotBlank(message = "name is required")
    @Size(min = 3, message = "name must be at least 3 characters")
    private String name;

    @NotBlank(message = "email is required")
    @Size(min = 8, message = "email must be at least 8 characters")
    private String email;
}
