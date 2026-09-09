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
        dto.setId(booking.getId());
        dto.setConferenceHallId(booking.getConferenceHall().getId());
        dto.setStartDateTime(booking.getStartDateTime());
        dto.setEndDateTime(booking.getEndDateTime());
        dto.setTotalCost(booking.getTotalCost());
        dto.setPaid(booking.isPaid());
        return dto;
    }
}
