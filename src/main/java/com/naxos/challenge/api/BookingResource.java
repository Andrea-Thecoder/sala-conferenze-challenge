package com.naxos.challenge.api;

import java.util.UUID;

import com.naxos.challenge.dto.PagedResultDTO;
import com.naxos.challenge.dto.SimpleResultDTO;
import com.naxos.challenge.dto.booking.BaseDetailBookingDTO;
import com.naxos.challenge.dto.booking.CreateBookingDTO;
import com.naxos.challenge.dto.booking.DetailBookingDTO;
import com.naxos.challenge.dto.booking.InsertStatusDTO;
import com.naxos.challenge.dto.booking.UpdateBookingDTO;
import com.naxos.challenge.dto.search.BookingSearchRequest;
import com.naxos.challenge.exception.ExceptionResponse;
import com.naxos.challenge.services.BookingService;
import com.naxos.challenge.services.record.BookingInsertStatus;
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
 * @Authenticated a livello di classe: prenotare una hall e consultare/modificare/
 * cancellare le proprie prenotazioni è un'azione di qualunque utente loggato, non
 * solo ADMIN/ORGANIZER. Una CUSTOMER vede e gestisce solo le proprie prenotazioni
 * (enforced server-side in BookingService contro il subject del token); ADMIN/
 * ORGANIZER possono operare su quelle di chiunque.
 */
@Path("/bookings")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Booking", description = "Conference hall booking management")
@Slf4j
public class BookingResource {

    @Inject
    BookingService bookingService;

    @POST
    @Operation(summary = "Book conference halls", description = "Creates one or more bookings for the caller. Each requested hall/time range is validated independently (hall enabled, no overlap) — the response reports how many succeeded and, for each failure, which hall and why.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Batch processed (see payload for per-item results)",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<InsertStatusDTO> createBookings(
            @RequestBody(description = "Bookings data", required = true)
            @Valid CreateBookingDTO dto) {
        log.info("BookingResource - createBookings : Creating new bookings");
        InsertStatusDTO status = bookingService.createBookings(dto);
        return SimpleResultDTO.<InsertStatusDTO>builder()
                .payload(status)
                .message("Bookings processed")
                .build();
    }

    @POST
    @Path("/admin/{userId}")
    @RolesAllowed({"ADMIN", "ORGANIZER"})
    @Operation(summary = "Book conference halls on behalf of a user", description = "Same as the self-service booking endpoint, but the bookings are created for the given user instead of the caller. Requires the ADMIN or ORGANIZER role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Batch processed (see payload for per-item results)",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN or ORGANIZER role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<InsertStatusDTO> createBookingsForUser(
            @Schema(description = "ID of the user the bookings are created for", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId,
            @RequestBody(description = "Bookings data", required = true)
            @Valid CreateBookingDTO dto) {
        log.info("BookingResource - createBookingsForUser : Creating new bookings for user {}", userId);
        InsertStatusDTO status = bookingService.createBookings(userId, dto);
        return SimpleResultDTO.<InsertStatusDTO>builder()
                .payload(status)
                .message("Bookings processed")
                .build();
    }

    @GET
    @Path("/{bookingId}")
    @Operation(summary = "Get a booking's detail", description = "Returns the given booking's detail, including the booked conference hall. A CUSTOMER may only view their own bookings; ADMIN/ORGANIZER may view any.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Booking detail retrieved",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "A CUSTOMER targeting another user's booking",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<DetailBookingDTO> getBookingById(
            @Schema(description = "ID of the booking to fetch", type = SchemaType.STRING, format = "uuid")
            @PathParam("bookingId") UUID bookingId) {
        log.info("BookingResource - getBookingById : Fetching booking {}", bookingId);
        DetailBookingDTO dto = bookingService.getBookingById(bookingId);
        return SimpleResultDTO.<DetailBookingDTO>builder().payload(dto).build();
    }

    @GET
    @Operation(summary = "List bookings", description = "Paginated listing of bookings. A CUSTOMER always sees only their own bookings (the userId filter is ignored for them); ADMIN/ORGANIZER see every booking and may filter by userId.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Bookings retrieved",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public PagedResultDTO<BaseDetailBookingDTO> findAllBookings(
            @BeanParam BookingSearchRequest request) {
        log.info("BookingResource - findAllBookings : page {} size {}", request.getPage(), request.getSize());
        return bookingService.findAllBookings(request);
    }

    @PATCH
    @Path("/{bookingId}")
    @Operation(summary = "Reschedule a booking", description = "Changes the time range of the given booking and recomputes its total cost. Rejected (payload populated) if the new range overlaps another booking on the same hall, or — for a CUSTOMER only — if the booking is already paid or starts in less than 24 hours. A CUSTOMER may only reschedule their own bookings; ADMIN/ORGANIZER may reschedule any and bypass the paid/24h restriction.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Booking rescheduled if the payload is absent; otherwise rejected for the reason in the payload",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload or a CUSTOMER targeting another user's booking",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<BookingInsertStatus> updateBooking(
            @Schema(description = "ID of the booking to reschedule", type = SchemaType.STRING, format = "uuid")
            @PathParam("bookingId") UUID bookingId,
            @RequestBody(description = "New time range", required = true)
            @Valid UpdateBookingDTO dto) {
        log.info("BookingResource - updateBooking : Rescheduling booking {}", bookingId);
        BookingInsertStatus status = bookingService.updateBooking(bookingId, dto);
        return SimpleResultDTO.<BookingInsertStatus>builder()
                .payload(status)
                .message(status == null ? "Booking updated" : "Booking not updated")
                .build();
    }

    @PATCH
    @Path("/admin/{userId}/{bookingId}")
    @RolesAllowed({"ADMIN", "ORGANIZER"})
    @Operation(summary = "Reschedule a user's booking", description = "Same as the self-service reschedule endpoint, but explicitly targets the given user's booking (rejected if the booking does not belong to that user). Requires the ADMIN or ORGANIZER role — bypasses the paid/24h restriction.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Booking rescheduled if the payload is absent; otherwise rejected for the reason in the payload",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload or booking does not belong to the given user",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN or ORGANIZER role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<BookingInsertStatus> updateBookingForUser(
            @Schema(description = "ID of the user the booking must belong to", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId,
            @Schema(description = "ID of the booking to reschedule", type = SchemaType.STRING, format = "uuid")
            @PathParam("bookingId") UUID bookingId,
            @RequestBody(description = "New time range", required = true)
            @Valid UpdateBookingDTO dto) {
        log.info("BookingResource - updateBookingForUser : Rescheduling booking {} for user {}", bookingId, userId);
        BookingInsertStatus status = bookingService.updateBooking(userId, bookingId, dto);
        return SimpleResultDTO.<BookingInsertStatus>builder()
                .payload(status)
                .message(status == null ? "Booking updated" : "Booking not updated")
                .build();
    }

    @DELETE
    @Path("/{bookingId}")
    @Operation(summary = "Cancel a booking", description = "Permanently deletes the given booking, regardless of payment status or how close the start time is. A CUSTOMER may only cancel their own bookings; ADMIN/ORGANIZER may cancel any.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Booking deleted",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "A CUSTOMER targeting another user's booking",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> deleteBooking(
            @Schema(description = "ID of the booking to delete", type = SchemaType.STRING, format = "uuid")
            @PathParam("bookingId") UUID bookingId) {
        log.info("BookingResource - deleteBooking : Deleting booking {}", bookingId);
        bookingService.deleteBooking(bookingId);
        return SimpleResultDTO.<Void>builder().message("Booking deleted").build();
    }

    @DELETE
    @Path("/admin/{userId}/{bookingId}")
    @RolesAllowed({"ADMIN", "ORGANIZER"})
    @Operation(summary = "Cancel a user's booking", description = "Same as the self-service cancel endpoint, but explicitly targets the given user's booking (rejected if the booking does not belong to that user). Requires the ADMIN or ORGANIZER role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Booking deleted",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Booking does not belong to the given user",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN or ORGANIZER role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> deleteBookingForUser(
            @Schema(description = "ID of the user the booking must belong to", type = SchemaType.STRING, format = "uuid")
            @PathParam("userId") UUID userId,
            @Schema(description = "ID of the booking to delete", type = SchemaType.STRING, format = "uuid")
            @PathParam("bookingId") UUID bookingId) {
        log.info("BookingResource - deleteBookingForUser : Deleting booking {} for user {}", bookingId, userId);
        bookingService.deleteBooking(userId, bookingId);
        return SimpleResultDTO.<Void>builder().message("Booking deleted").build();
    }
}
