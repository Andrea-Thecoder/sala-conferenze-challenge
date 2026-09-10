package com.sala.challenge.dto.booking;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Payload used to book a conference hall")
public class CreateBookingDTO {

   @Valid
   @NotEmpty(message = "At least one conference hall reservation is required")
   @Size(min = 1, max = 5, message = "Between 1 and 5 conference hall reservations are allowed per request")
   private List<ConferenceHallReservationDTO> conferenceHallReservationDTOs;

}
