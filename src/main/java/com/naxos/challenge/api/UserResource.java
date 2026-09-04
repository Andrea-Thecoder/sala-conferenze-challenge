package com.naxos.challenge.api;

import java.util.List;
import java.util.UUID;

import com.naxos.challenge.dto.PagedResultDTO;
import com.naxos.challenge.dto.SimpleResultDTO;
import com.naxos.challenge.dto.search.UserSearchRequest;
import com.naxos.challenge.dto.user.PhoneNumberUpdateDTO;
import com.naxos.challenge.dto.user.BaseDetailUserDTO;
import com.naxos.challenge.exception.ExceptionResponse;
import com.naxos.challenge.services.UserService;
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
 * senza token) — non duplicata qui. findAll/deleteUser sono ristretti oltre
 * l'autenticazione ad ADMIN/ORGANIZER: elencare o cancellare utenti arbitrari non è
 * un'azione che un qualunque utente loggato (es. CUSTOMER) deve poter fare.
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

    @PATCH
    @Path("/{userId}/phone-number")
    @Operation(summary = "Update a user's phone number", description = "Updates the phone number of the given user. A CUSTOMER may only target themselves (enforced server-side against the access token subject); ADMIN/ORGANIZER may target any user.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Phone number updated",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid phone number, already in use, or a CUSTOMER targeting another user",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
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
    @RolesAllowed({"ADMIN", "ORGANIZER"})
    @Operation(summary = "Delete a user", description = "Permanently deletes a user account. Requires the ADMIN or ORGANIZER role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "User deleted",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN or ORGANIZER role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> deleteUser(
            @Schema(description = "ID of the user to delete", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId) {
        log.info("UserResource - deleteUser : Deleting user {}", userId);
        userService.deleteUser(userId);
        return SimpleResultDTO.<Void>builder().message("User deleted").build();
    }
}
