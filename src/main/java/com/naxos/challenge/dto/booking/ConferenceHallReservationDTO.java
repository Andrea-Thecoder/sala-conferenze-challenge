package com.naxos.challenge.dto.booking;

import com.naxos.challenge.model.Booking;
import com.naxos.challenge.model.ConferenceHall;
import com.naxos.challenge.model.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class ConferenceHallReservationDTO {

    @NotNull(message = "Conference hall is required")
    @Schema(description = "ID of the conference hall to book")
    private UUID conferenceHallId;

    @NotNull(message = "Start date/time is required")
    @Future(message = "Start date/time must be in the future")
    @Schema(description = "Booking start date/time", example = "2026-10-01T09:00:00")
    private LocalDateTime startDateTime;

    @NotNull(message = "End date/time is required")
    @Future(message = "End date/time must be in the future")
    @Schema(description = "Booking end date/time", example = "2026-10-01T11:00:00")
    private LocalDateTime endDateTime;

    @AssertTrue(message = "End date/time must be after start date/time")
    private boolean isTimeRangeValid() {
        return startDateTime == null || endDateTime == null || endDateTime.isAfter(startDateTime);
    }

    public Booking toEntity(ConferenceHall conferenceHall, User user) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStartDateTime(startDateTime);
        booking.setEndDateTime(endDateTime);
        booking.setConferenceHall(conferenceHall);
        return booking;
    }
}
