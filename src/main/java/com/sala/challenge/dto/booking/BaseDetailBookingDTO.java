package com.sala.challenge.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.sala.challenge.model.Booking;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Booking summary")
public class BaseDetailBookingDTO {

    private UUID id;
    private UUID conferenceHallId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private BigDecimal totalCost;
    private boolean paid;

    public static BaseDetailBookingDTO of(Booking booking) {
        BaseDetailBookingDTO dto = new BaseDetailBookingDTO();
        dto.populate(booking);
        return dto;
    }

    protected void populate(Booking booking) {
        this.id = booking.getId();
        this.conferenceHallId = booking.getConferenceHall().getId();
        this.startDateTime = booking.getStartDateTime();
        this.endDateTime = booking.getEndDateTime();
        this.totalCost = booking.getTotalCost();
        this.paid = booking.isPaid();
    }
}
