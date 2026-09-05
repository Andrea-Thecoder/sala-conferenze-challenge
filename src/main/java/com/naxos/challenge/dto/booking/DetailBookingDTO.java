package com.naxos.challenge.dto.booking;

import com.naxos.challenge.dto.conference.DetailConferenceHallDTO;
import com.naxos.challenge.dto.user.BaseDetailUserDTO;
import com.naxos.challenge.dto.user.DetailUserDTO;
import com.naxos.challenge.model.Booking;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetailBookingDTO extends BaseDetailBookingDTO {

    private DetailConferenceHallDTO conferenceHall;
    private BaseDetailUserDTO user;


    public static DetailBookingDTO of(Booking booking) {
        DetailBookingDTO dto = new DetailBookingDTO();
        dto.setId(booking.getId());
        dto.setUser(BaseDetailUserDTO.of(booking.getUser()));
        dto.setConferenceHallId(booking.getConferenceHall().getId());
        dto.setStartDateTime(booking.getStartDateTime());
        dto.setEndDateTime(booking.getEndDateTime());
        dto.setTotalCost(booking.getTotalCost());
        dto.setPaid(booking.isPaid());
        dto.setConferenceHall(DetailConferenceHallDTO.of(booking.getConferenceHall()));
        return dto;
    }
}
