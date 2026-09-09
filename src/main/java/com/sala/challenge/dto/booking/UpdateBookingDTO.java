package com.sala.challenge.dto.booking;

import java.time.LocalDateTime;

import com.sala.challenge.model.Booking;
import com.sala.challenge.util.PricingUtils;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Payload to reschedule a booking")
public class UpdateBookingDTO {

    @NotNull(message = "Start date/time is required")
    @Future(message = "Start date/time must be in the future")
    @Schema(description = "New booking start date/time", example = "2026-10-01T09:00:00")
    private LocalDateTime startDateTime;

    @NotNull(message = "End date/time is required")
    @Future(message = "End date/time must be in the future")
    @Schema(description = "New booking end date/time", example = "2026-10-01T11:00:00")
    private LocalDateTime endDateTime;

    @AssertTrue(message = "End date/time must be after start date/time")
    private boolean isTimeRangeValid() {
        return startDateTime == null || endDateTime == null || endDateTime.isAfter(startDateTime);
    }

    public void toUpdate(Booking booking) {
        booking.setStartDateTime(startDateTime);
        booking.setEndDateTime(endDateTime);
        booking.setTotalCost(PricingUtils.computeTotalCost(booking.getConferenceHall().getPricePerHour(), startDateTime, endDateTime));
    }
}
