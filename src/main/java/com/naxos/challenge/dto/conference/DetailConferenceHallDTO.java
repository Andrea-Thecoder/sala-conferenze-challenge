package com.naxos.challenge.dto.conference;

import com.naxos.challenge.dto.building.BaseDetailBuildingDTO;
import com.naxos.challenge.model.ConferenceHall;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetailConferenceHallDTO {

    private UUID id;
    private String name;
    private String note;
    private Integer size = 2;
    private BigDecimal pricePerHour;
    private BaseDetailBuildingDTO building;
    private Integer floor;
    private String roomNumber;
    private boolean enabled = true;

    public static DetailConferenceHallDTO of (ConferenceHall conferenceHall) {
        DetailConferenceHallDTO  detailConferenceHallDTO = new DetailConferenceHallDTO();
        detailConferenceHallDTO.setId(conferenceHall.getId());
        detailConferenceHallDTO.setName(conferenceHall.getName());
        detailConferenceHallDTO.setNote(conferenceHall.getNote());
        detailConferenceHallDTO.setSize(conferenceHall.getSize());
        detailConferenceHallDTO.setPricePerHour(conferenceHall.getPricePerHour());
        detailConferenceHallDTO.setFloor(conferenceHall.getFloor());
        detailConferenceHallDTO.setRoomNumber(conferenceHall.getRoomNumber());
        detailConferenceHallDTO.setEnabled(conferenceHall.isEnabled());
        detailConferenceHallDTO.setBuilding(BaseDetailBuildingDTO.of(conferenceHall.getBuilding()));
        return detailConferenceHallDTO;
    }

}
