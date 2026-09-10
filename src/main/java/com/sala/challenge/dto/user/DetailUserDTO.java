package com.sala.challenge.dto.user;

import com.sala.challenge.dto.booking.BaseDetailBookingDTO;
import com.sala.challenge.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetailUserDTO extends BaseDetailUserDTO {

    private List<BaseDetailBookingDTO> bookings;

    public static DetailUserDTO of(User user, List<BaseDetailBookingDTO> bookings) {
        DetailUserDTO dto = new DetailUserDTO();
        dto.populate(user);
        dto.setBookings(bookings);
        return dto;
    }
}
