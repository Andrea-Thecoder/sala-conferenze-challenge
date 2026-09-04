package com.naxos.challenge.api;

import com.naxos.challenge.dto.SimpleResultDTO;
import com.naxos.challenge.dto.user.AuthTokenDTO;
import com.naxos.challenge.dto.user.LoginCredentialsDTO;
import com.naxos.challenge.dto.user.RefreshTokenDTO;
import com.naxos.challenge.dto.user.UserRegistrationDTO;
import com.naxos.challenge.exception.ExceptionResponse;
import com.naxos.challenge.services.AuthService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/**
 * Endpoint pubblici solo dove il flusso lo richiede davvero: {@code /logout} deve
 * funzionare anche con access token già scaduto (vedi {@code quarkus.http.auth.proactive=false}
 * in application.properties) — la revoca del refresh token non può dipendere da un
 * access token ancora valido, altrimenti il caso d'uso più comune del logout si romperebbe.
 * L'"autenticazione" di /refresh e /logout è il possesso del refresh token grezzo stesso
 * (segreto casuale, hash salvato in DB) — non un Bearer JWT.
 */
@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "Registration, login, refresh and logout")
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user (organizer or customer) with a hashed password; does not issue any token, the user must then authenticate via /login.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "User created",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload or email already registered",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<UUID> register(
            @RequestBody(description = "Registration data", required = true)
            @Valid UserRegistrationDTO dto) {
        UUID userId = authService.registerUser(dto);
        return SimpleResultDTO.<UUID>builder()
                .payload(userId)
                .message("User successfully registered")
                .build();
    }

    @POST
    @Path("/login")
    @Operation(summary = "Authenticate a user", description = "Verifies the credentials and issues a new token pair: a short-lived JWT access token and a long-lived raw refresh token (the client must keep it for /refresh and /logout).")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Login successful, token pair issued",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid credentials, or user disabled/revoked",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<AuthTokenDTO> login(
            @RequestBody(description = "Email and password", required = true)
            @Valid LoginCredentialsDTO credentials) {
        AuthTokenDTO tokens = authService.login(credentials);
        return SimpleResultDTO.<AuthTokenDTO>builder().payload(tokens).build();
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh the session", description = "Takes ONLY the raw refresh token as input (no valid access token needed: this is exactly the case where it has already expired). Rotates the refresh token (revokes the old one, issues a new one in the same family) and issues a new access token. A refresh token that was already revoked, if presented again, triggers reuse detection: the entire family is revoked.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "New token pair issued",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Refresh token not found, expired, reused after revocation, or user disabled/revoked",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<AuthTokenDTO> refresh(
            @RequestBody(description = "Raw refresh token to renew", required = true)
            @Valid RefreshTokenDTO dto) {
        AuthTokenDTO tokens = authService.refresh(dto.getRefreshToken());
        return SimpleResultDTO.<AuthTokenDTO>builder().payload(tokens).build();
    }

    @POST
    @Path("/logout")
    @Operation(summary = "End the current session", description = "Public endpoint: only requires the refresh token in the body to revoke that session. If the client also sends a still-valid access token in the Authorization header, it is blacklisted on Redis as a best-effort reinforcement for the remaining window; if the access token is already expired, missing, or invalid, the refresh token is revoked anyway (that's the part that actually matters, see AuthService.logout).")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Logout performed (idempotent: no error if the refresh token no longer exists)",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class)))
    })
    public SimpleResultDTO<Void> logout(
            @RequestBody(description = "Raw refresh token to revoke", required = true)
            @Valid RefreshTokenDTO dto) {
        authService.logout(dto.getRefreshToken());
        return SimpleResultDTO.<Void>builder().message("Logout successful").build();
    }

    @POST
    @Path("/users/{userId}/activate")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Activate a registered user", description = "Administrative gate for /register: every new account is created inactive and cannot login until an ADMIN reviews and activates it. This is also what stops a self-registered ADMIN/ORGANIZER role from being usable without explicit review. Requires the ADMIN role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "User activated",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> activateUser(
            @Schema(description = "ID of the user to activate", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId) {
        authService.activateUser(userId);
        return SimpleResultDTO.<Void>builder().message("User activated").build();
    }

    @POST
    @Path("/users/{userId}/revoke")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Revoke a user's account", description = "Administrative action for a compromised or no-longer-trusted account: disables login (active=false) and sets the role to REVOKED, so the user is denied even if some other flow only checks the role. Does NOT by itself invalidate existing sessions — pair with /sessions/revoke-all/{userId} to also kill active refresh tokens and blacklist outstanding access tokens. Requires the ADMIN role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "User revoked",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> revokeUser(
            @Schema(description = "ID of the user to revoke", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId) {
        authService.revokeUser(userId);
        return SimpleResultDTO.<Void>builder().message("User revoked").build();
    }

    @POST
    @Path("/sessions/revoke-all/{userId}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Revoke all sessions of a user", description = "Administrative kill-switch for a compromised or just-revoked account: revokes every active refresh token for the user on Postgres and blacklists every access token issued so far on Redis. Requires the ADMIN role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "All sessions of the user have been revoked",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> revokeAllSessions(
            @Schema(description = "ID of the user whose sessions must all be revoked", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId) {
        authService.revokeAllSessions(userId);
        return SimpleResultDTO.<Void>builder().message("All sessions revoked").build();
    }
}
