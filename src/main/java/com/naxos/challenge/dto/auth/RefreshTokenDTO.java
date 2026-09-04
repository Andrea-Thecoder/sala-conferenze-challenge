package com.naxos.challenge.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Payload carrying the raw (unhashed) refresh token issued at login/refresh")
public class RefreshTokenDTO {

    @NotBlank(message = "Refresh token is required")
    @Schema(description = "Raw refresh token, as returned by /auth/login or /auth/refresh")
    private String refreshToken;
}
