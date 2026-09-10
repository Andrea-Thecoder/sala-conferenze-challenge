package com.sala.challenge.api;

import com.sala.challenge.dto.SimpleResultDTO;
import com.sala.challenge.dto.auth.AuthTokenDTO;
import com.sala.challenge.dto.auth.LoginCredentialsDTO;
import com.sala.challenge.dto.auth.RefreshTokenDTO;
import com.sala.challenge.dto.user.RoleUpdateDTO;
import com.sala.challenge.dto.user.UserRegistrationDTO;
import com.sala.challenge.exception.ExceptionResponse;
import com.sala.challenge.services.AuthService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
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
                    content = @Content(schema = @Schema(implementation = AuthTokenDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid credentials, or user disabled/revoked",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public AuthTokenDTO login(
            @RequestBody(description = "Email and password", required = true)
            @Valid LoginCredentialsDTO credentials) {
        return authService.login(credentials);
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh the session", description = "Takes ONLY the raw refresh token as input (no valid access token needed: this is exactly the case where it has already expired). Rotates the refresh token (revokes the old one, issues a new one in the same family) and issues a new access token. A refresh token that was already revoked, if presented again, triggers reuse detection: the entire family is revoked.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "New token pair issued",
                    content = @Content(schema = @Schema(implementation = AuthTokenDTO.class))),
            @APIResponse(responseCode = "400", description = "Refresh token not found, expired, reused after revocation, or user disabled/revoked",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public AuthTokenDTO refresh(
            @RequestBody(description = "Raw refresh token to renew", required = true)
            @Valid RefreshTokenDTO dto) {
        return authService.refresh(dto.getRefreshToken());
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

    @PATCH
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
            @APIResponse(responseCode = "400", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> activateUser(
            @Schema(description = "ID of the user to activate", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId) {
        authService.activateUser(userId);
        return SimpleResultDTO.<Void>builder().message("User activated").build();
    }

    @PATCH
    @Path("/users/{userId}/role")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Change a user's role", description = "Promotes/changes the role of a user (e.g. CUSTOMER to ORGANIZER) — an authorization change, not a profile edit, same category as activate/revoke. REVOKED is not accepted here — use the dedicated revoke endpoint, which also disables the account and kills active sessions. Requires the ADMIN role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Role changed",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "REVOKED role requested here (use the revoke endpoint instead), or user not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> changeRole(
            @Schema(description = "ID of the user whose role is being changed", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId,
            @RequestBody(description = "New role", required = true)
            @Valid RoleUpdateDTO dto) {
        authService.changeRole(userId, dto);
        return SimpleResultDTO.<Void>builder().message("Role changed").build();
    }

    @DELETE
    @Path("/users/{userId}/revoke")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Revoke a user's account", description = "Administrative action for a compromised or no-longer-trusted account: disables login (active=false), sets the role to REVOKED, and atomically revokes every active refresh token for the user on Postgres — one transaction, one commit. Access tokens already issued are blacklisted on Redis right after, best-effort. Requires the ADMIN role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "User revoked",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "400", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> revokeUser(
            @Schema(description = "ID of the user to revoke", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId) {
        authService.revokeUser(userId);
        return SimpleResultDTO.<Void>builder().message("User revoked").build();
    }

    @DELETE
    @Path("/users/{userId}/sessions")
    @Authenticated
    @Operation(summary = "Revoke my sessions", description = "Self-service \"log out everywhere\": revokes every active refresh token for the given user on Postgres and blacklists every access token issued so far on Redis. Any authenticated user may call this, but only for themselves — userId must match the caller's own subject.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "All sessions of the user have been revoked",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "userId does not match the caller",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> revokeMySessions(
            @Schema(description = "ID of the user whose sessions must all be revoked (must be the caller)", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId) {
        authService.revokeMySessions(userId);
        return SimpleResultDTO.<Void>builder().message("All sessions revoked").build();
    }

    @DELETE
    @Path("/admin/users/{userId}/sessions")
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
