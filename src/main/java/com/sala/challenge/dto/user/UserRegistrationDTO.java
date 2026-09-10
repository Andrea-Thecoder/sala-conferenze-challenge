package com.sala.challenge.dto.user;

import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Payload used to register a new user (organizer or customer)")
public class UserRegistrationDTO {

    @NotBlank(message = "First name is required")
    @Schema(description = "User's first name", example = "Mario")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "User's last name", example = "Rossi")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Email must be a valid address"
    )
    @Schema(description = "User's email address, used as login identifier", example = "mario.rossi@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 32, message = "Password must be between 8 and 32 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).*$",
            message = "Password must contain at least one uppercase letter, one digit and one special character"
    )
    @Schema(description = "Password: 8-32 characters, at least one uppercase letter, one digit and one special character")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Size(max = 16, message = "Phone number must be at most 16 characters, including the optional leading '+'")
    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "Phone number must contain only digits, with at most one optional leading '+'"
    )
    @Schema(description = "Mobile phone number, optional leading '+' followed by 7 to 15 digits (16 characters max in total)", example = "+393331234567")
    private String phoneNumber;

    @NotNull(message = "Role is required")
    @Schema(description = "Role requested at registration", example = "CUSTOMER")
    private Role role;

    /**
     * REVOKED è uno stato applicativo (assegnato solo da /auth/users/{id}/revoke o
     * dall'anonimizzazione GDPR), non un ruolo richiedibile in self-registrazione —
     * stessa regola già applicata in RoleUpdateDTO/UserService.changeRole.
     */
    @AssertTrue(message = "REVOKED is not a role that can be requested at registration")
    private boolean isRoleRegistrable() {
        return role != Role.REVOKED;
    }

    public User toEntity() {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setRole(role);
        user.setActive(false);
        return user;

    }
}
