package com.sala.challenge.dto.conference;

import java.math.BigDecimal;
import java.util.UUID;

import com.sala.challenge.model.ConferenceHall;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Conference hall summary")
public class BaseDetailConferenceHallDTO {

    protected UUID id;
    protected String name;
    protected String note;
    protected Integer size;
    protected BigDecimal pricePerHour;
    protected UUID buildingId;
    protected Integer floor;
    protected String roomNumber;
    protected boolean enabled;

    public static BaseDetailConferenceHallDTO of(ConferenceHall conferenceHall) {
        BaseDetailConferenceHallDTO dto = new BaseDetailConferenceHallDTO();
        dto.setId(conferenceHall.getId());
        dto.setName(conferenceHall.getName());
        dto.setNote(conferenceHall.getNote());
        dto.setSize(conferenceHall.getSize());
        dto.setPricePerHour(conferenceHall.getPricePerHour());
        dto.setBuildingId(conferenceHall.getBuilding().getId());
        dto.setFloor(conferenceHall.getFloor());
        dto.setRoomNumber(conferenceHall.getRoomNumber());
        dto.setEnabled(conferenceHall.isEnabled());
        return dto;
    }
}
