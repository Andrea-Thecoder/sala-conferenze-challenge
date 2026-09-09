package com.sala.challenge.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Payload to change a user's password")
public class ChangePasswordDTO {

    @NotBlank(message = "Current password is required")
    @Schema(description = "The user's current password, verified before applying the change")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 32, message = "Password must be between 8 and 32 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).*$",
            message = "Password must contain at least one uppercase letter, one digit and one special character"
    )
    @Schema(description = "New password: 8-32 characters, at least one uppercase letter, one digit and one special character")
    private String newPassword;
}
