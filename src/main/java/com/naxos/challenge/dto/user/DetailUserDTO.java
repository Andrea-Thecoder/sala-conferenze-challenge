package com.naxos.challenge.dto.user;

import com.naxos.challenge.dto.booking.BaseDetailBookingDTO;
import com.naxos.challenge.model.Booking;
import com.naxos.challenge.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class DetailUserDTO extends  BaseDetailUserDTO{

    private List<BaseDetailBookingDTO>  bookings;

    public static DetailUserDTO of (User user, List<BaseDetailBookingDTO> bookings) {
        DetailUserDTO dto = new DetailUserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());
        dto.setBookings(bookings);
        return dto;
    }



}
