package com.sala.challenge.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Payload used to authenticate an existing user")
public class LoginCredentialsDTO {

    @NotBlank(message = "Email is required")
    @Email
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Email must be a valid address"
    )
    @Schema(description = "User's email address", example = "mario.rossi@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "User's password")
    private String password;
}
