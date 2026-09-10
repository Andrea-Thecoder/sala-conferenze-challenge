package com.sala.challenge.api;

import java.util.UUID;

import com.sala.challenge.dto.PagedResultDTO;
import com.sala.challenge.dto.SimpleResultDTO;
import com.sala.challenge.dto.conference.BaseDetailConferenceHallDTO;
import com.sala.challenge.dto.conference.CreateConferenceHallDTO;
import com.sala.challenge.dto.conference.DetailConferenceHallDTO;
import com.sala.challenge.dto.conference.UpdateConferenceHallDTO;
import com.sala.challenge.dto.search.ConferenceHallSearchRequest;
import com.sala.challenge.exception.ExceptionResponse;
import com.sala.challenge.services.ConferenceHallService;
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
 * @Authenticated a livello di classe: un CUSTOMER deve poter consultare le sale
 * disponibili, non solo ADMIN/ORGANIZER. Create/update restano ad ADMIN/
 * ORGANIZER — a differenza di Building (ADMIN only, infrastruttura), la
 * conference hall è la "sala" del dominio della challenge, e il README la
 * assegna esplicitamente all'organizzatore ("organizzatori, che possono creare
 * e gestire le proprie sale"). Non esiste ownership per-organizzatore sulla
 * singola sala (solo il ruolo è verificato): qualunque ORGANIZER può gestire
 * qualunque sala, stessa semplificazione già adottata per Building/Booking.
 * <p>
 * Due livelli di cancellazione: {@code PATCH .../disable} (soft, ADMIN/ORGANIZER)
 * marca la sala come non disponibile; {@code DELETE} (hard, solo ADMIN) la rimuove
 * fisicamente e in modo irreversibile.
 */
@Path("/conference-halls")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "ConferenceHall", description = "Conference hall management")
@Slf4j
public class ConferenceHallResource {

    @Inject
    ConferenceHallService conferenceHallService;

    @POST
    @RolesAllowed({"ADMIN", "ORGANIZER"})
    @Operation(summary = "Create a new conference hall", description = "Creates a new conference hall inside the given building. Requires the ADMIN or ORGANIZER role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Conference hall created",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload or building not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN or ORGANIZER role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<UUID> createConferenceHall(
            @RequestBody(description = "Conference hall data", required = true)
            @Valid CreateConferenceHallDTO dto) {
        log.info("ConferenceHallResource - createConferenceHall : Creating new conference hall");
        UUID conferenceHallId = conferenceHallService.createConferenceHall(dto);
        return SimpleResultDTO.<UUID>builder()
                .payload(conferenceHallId)
                .message("Conference hall successfully created")
                .build();
    }

    @GET
    @Path("/{conferenceHallId}")
    @Operation(summary = "Get a conference hall's detail", description = "Returns the given conference hall's detail, including its building.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Conference hall detail retrieved",
                    content = @Content(schema = @Schema(implementation = DetailConferenceHallDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "400", description = "Conference hall not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public DetailConferenceHallDTO getConferenceHallById(
            @Schema(description = "ID of the conference hall to fetch", type = SchemaType.STRING, format = "uuid")
            @PathParam("conferenceHallId") UUID conferenceHallId) {
        log.info("ConferenceHallResource - getConferenceHallById");
        return conferenceHallService.getConferenceHallDetailById(conferenceHallId);
    }

    @GET
    @Operation(summary = "List conference halls", description = "Paginated listing of conference halls. A CUSTOMER always sees available (enabled) halls only, regardless of the enabled filter; ADMIN/ORGANIZER see every hall and may filter by availability.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Conference halls retrieved",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public PagedResultDTO<BaseDetailConferenceHallDTO> findAllConferenceHalls(
            @BeanParam ConferenceHallSearchRequest request) {
        log.info("ConferenceHallResource - findAllConferenceHalls : page {} size {}", request.getPage(), request.getSize());
        return conferenceHallService.findAllConferenceHalls(request);
    }

    @PATCH
    @Path("/{conferenceHallId}")
    @RolesAllowed({"ADMIN", "ORGANIZER"})
    @Operation(summary = "Update a conference hall", description = "Updates a conference hall's name, note, size, price per hour, floor and room number. Does not affect availability — use the disable or delete endpoints for that. Requires the ADMIN or ORGANIZER role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Conference hall updated",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload, or conference hall not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN or ORGANIZER role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> updateConferenceHall(
            @Schema(description = "ID of the conference hall to update", type = SchemaType.STRING, format = "uuid")
            @PathParam("conferenceHallId") UUID conferenceHallId,
            @RequestBody(description = "Updated conference hall data", required = true)
            @Valid UpdateConferenceHallDTO dto) {
        log.info("ConferenceHallResource - updateConferenceHall : Updating conference hall {}", conferenceHallId);
        conferenceHallService.updateConferenceHall(conferenceHallId, dto);
        return SimpleResultDTO.<Void>builder().message("Conference hall updated").build();
    }

    @PATCH
    @Path("/{conferenceHallId}/disable")
    @RolesAllowed({"ADMIN", "ORGANIZER"})
    @Operation(summary = "Disable a conference hall (soft delete)", description = "Marks the conference hall as unavailable (enabled=false) without deleting it. Requires the ADMIN or ORGANIZER role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Conference hall disabled",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN or ORGANIZER role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "400", description = "Conference hall not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> disableConferenceHall(
            @Schema(description = "ID of the conference hall to disable", type = SchemaType.STRING, format = "uuid")
            @PathParam("conferenceHallId") UUID conferenceHallId) {
        log.info("ConferenceHallResource - disableConferenceHall : Disabling conference hall {}", conferenceHallId);
        conferenceHallService.disableConferenceHall(conferenceHallId);
        return SimpleResultDTO.<Void>builder().message("Conference hall disabled").build();
    }

    @DELETE
    @Path("/{conferenceHallId}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Delete a conference hall (hard delete)", description = "Permanently and irreversibly deletes a conference hall. Requires the ADMIN role — an ORGANIZER can only disable a hall (see the disable endpoint), not remove it.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Conference hall deleted",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "400", description = "Conference hall not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> deleteConferenceHall(
            @Schema(description = "ID of the conference hall to delete", type = SchemaType.STRING, format = "uuid")
            @PathParam("conferenceHallId") UUID conferenceHallId) {
        log.info("ConferenceHallResource - deleteConferenceHall : Deleting conference hall {}", conferenceHallId);
        conferenceHallService.deleteConferenceHall(conferenceHallId);
        return SimpleResultDTO.<Void>builder().message("Conference hall deleted").build();
    }
}
