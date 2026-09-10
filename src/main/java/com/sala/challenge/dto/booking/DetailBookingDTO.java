package com.sala.challenge.dto.booking;

import com.sala.challenge.dto.conference.DetailConferenceHallDTO;
import com.sala.challenge.dto.user.BaseDetailUserDTO;
import com.sala.challenge.model.Booking;
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
        dto.populate(booking);
        dto.setUser(BaseDetailUserDTO.of(booking.getUser()));
        dto.setConferenceHall(DetailConferenceHallDTO.of(booking.getConferenceHall()));
        return dto;
    }
}
