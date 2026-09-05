package com.naxos.challenge.dto.user;

import com.naxos.challenge.model.enumerator.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Payload to change a user's role")
public class RoleUpdateDTO {

    @NotNull(message = "Role is required")
    @Schema(description = "New role for the user. REVOKED is not accepted here — use the dedicated revoke endpoint, which also disables the account and kills active sessions.", example = "ORGANIZER")
    private Role role;
}
