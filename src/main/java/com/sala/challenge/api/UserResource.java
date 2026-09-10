package com.sala.challenge.api;

import java.util.List;
import java.util.UUID;

import com.sala.challenge.dto.PagedResultDTO;
import com.sala.challenge.dto.SimpleResultDTO;
import com.sala.challenge.dto.search.UserSearchRequest;
import com.sala.challenge.dto.auth.ChangePasswordDTO;
import com.sala.challenge.dto.user.DetailUserDTO;
import com.sala.challenge.dto.user.PhoneNumberUpdateDTO;
import com.sala.challenge.dto.user.BaseDetailUserDTO;
import com.sala.challenge.exception.ExceptionResponse;
import com.sala.challenge.services.AuthService;
import com.sala.challenge.services.UserService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * @Authenticated a livello di classe: nessun endpoint pubblico, serve sempre un
 * access token valido. La creazione utente resta su /auth/register (self-service,
 * senza token) — non duplicata qui. findAll è ristretto oltre l'autenticazione ad
 * ADMIN/ORGANIZER: elencare utenti arbitrari non è un'azione che un qualunque utente
 * loggato (es. CUSTOMER) deve poter fare. deleteUser è ADMIN-only (non anche
 * ORGANIZER): anonimizza in modo irreversibile nome/email/telefono/password e
 * revoca ogni sessione — un ORGANIZER non deve poter fare più danno di quanto gli
 * sia concesso da /auth/users/{userId}/revoke, che è ADMIN-only.
 */
@Path("/users")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "User", description = "User profile and administration")
@Slf4j
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    AuthService authService;


    @PATCH
    @Path("/{userId}/phone-number")
    @Operation(summary = "Update a user's phone number", description = "Updates the phone number of the given user. A CUSTOMER may only target themselves (enforced server-side against the access token subject); ADMIN/ORGANIZER may target any user.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Phone number updated",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid phone number or already in use",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "A CUSTOMER targeting another user",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> updatePhoneNumber(
            @Schema(description = "ID of the user whose phone number is being updated", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId,
            @RequestBody(description = "New phone number", required = true)
            @Valid PhoneNumberUpdateDTO dto) {
        log.info("UserResource - updatePhoneNumber : Updating phone number for user {}", userId);
        userService.updatePhoneNumber(userId, dto);
        return SimpleResultDTO.<Void>builder().message("Phone number updated").build();
    }

    @PATCH
    @Path("/{userId}/password")
    @Operation(summary = "Change my password", description = "Changes the password of the given user; requires the current password. Always self, regardless of role — userId must match the caller's own subject. An ADMIN cannot use this to reset someone else's password, since it requires knowing the current one.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Password changed",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Wrong current password or invalid new password",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "userId not matching the caller (self-only operation)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> changePassword(
            @Schema(description = "ID of the user changing their password (must be the caller)", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId,
            @RequestBody(description = "Current and new password", required = true)
            @Valid ChangePasswordDTO dto) {
        log.info("UserResource - changePassword : Password change requested for user {}", userId);
        userService.changePassword(userId, dto);
        return SimpleResultDTO.<Void>builder().message("Password changed").build();
    }

    @GET
    @Path("/{userId}")
    @Operation(summary = "Get a user's detail", description = "Returns the given user's profile plus their bookings. A CUSTOMER may only view themselves (enforced server-side against the access token subject); ADMIN/ORGANIZER may view any user.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "User detail retrieved",
                    content = @Content(schema = @Schema(implementation = DetailUserDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "A CUSTOMER targeting another user",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "400", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public DetailUserDTO getUserById(
            @Schema(description = "ID of the user to fetch", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId) {
        log.info("UserResource - getUserById ");
        return userService.getUserById(userId);
    }

    @GET
    @RolesAllowed({"ADMIN", "ORGANIZER"})
    @Operation(summary = "List users", description = "Paginated listing of all users. Requires the ADMIN or ORGANIZER role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Users retrieved",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN or ORGANIZER role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public PagedResultDTO<BaseDetailUserDTO> findAllUsers(
            @BeanParam UserSearchRequest request) {
        log.info("UserResource - findAllUsers : page {} size {}", request.getPage(), request.getSize());
        return userService.findAll(request);
    }

    @DELETE
    @Path("/{userId}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Delete a user", description = "GDPR-style erasure: does not physically remove the account (booking/refresh-token history must be preserved), but irreversibly anonymizes its identifying data (name, email, phone, password) and disables it (active=false, role=REVOKED), revoking every active session. Requires the ADMIN role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "User deleted (anonymized and revoked)",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "400", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> deleteUser(
            @Schema(description = "ID of the user to delete", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId) {
        log.info("UserResource - deleteUser : Deleting user {}", userId);
        authService.deleteUser(userId);
        return SimpleResultDTO.<Void>builder().message("User deleted").build();
    }
}
