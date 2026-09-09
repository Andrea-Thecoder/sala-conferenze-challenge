package com.sala.challenge.dto.conference;

import com.sala.challenge.dto.building.BaseDetailBuildingDTO;
import com.sala.challenge.model.ConferenceHall;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetailConferenceHallDTO extends BaseDetailConferenceHallDTO {

    private BaseDetailBuildingDTO building;

    public static DetailConferenceHallDTO of(ConferenceHall conferenceHall) {
        DetailConferenceHallDTO dto = new DetailConferenceHallDTO();
        dto.setId(conferenceHall.getId());
        dto.setName(conferenceHall.getName());
        dto.setNote(conferenceHall.getNote());
        dto.setSize(conferenceHall.getSize());
        dto.setPricePerHour(conferenceHall.getPricePerHour());
        dto.setBuildingId(conferenceHall.getBuilding().getId());
        dto.setFloor(conferenceHall.getFloor());
        dto.setRoomNumber(conferenceHall.getRoomNumber());
        dto.setEnabled(conferenceHall.isEnabled());
        dto.setBuilding(BaseDetailBuildingDTO.of(conferenceHall.getBuilding()));
        return dto;
    }
}
