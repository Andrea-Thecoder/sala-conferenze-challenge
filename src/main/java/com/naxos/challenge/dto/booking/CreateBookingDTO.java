package com.naxos.challenge.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.naxos.challenge.model.Booking;
import com.naxos.challenge.model.ConferenceHall;
import com.naxos.challenge.model.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Payload used to book a conference hall")
public class CreateBookingDTO {

   private List<ConferenceHallReservationDTO> conferenceHallReservationDTOs;


}
