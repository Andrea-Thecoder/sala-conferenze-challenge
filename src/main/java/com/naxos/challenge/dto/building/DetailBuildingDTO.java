package com.naxos.challenge.dto.building;

import com.naxos.challenge.dto.conference.DetailConferenceHallDTO;
import com.naxos.challenge.model.Building;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetailBuildingDTO extends  BaseDetailBuildingDTO {

    private List<DetailConferenceHallDTO> conferenceHallList;

    public static DetailBuildingDTO of (Building building) {
        DetailBuildingDTO detailBuildingDTO = new DetailBuildingDTO();
        detailBuildingDTO.setId(building.getId());
        detailBuildingDTO.setStreet(building.getStreet());
        detailBuildingDTO.setCity(building.getCity());
        detailBuildingDTO.setPostalCode(building.getPostalCode());
        detailBuildingDTO.setCountry(building.getCountry());
        detailBuildingDTO.setConferenceHallList(building.getConferenceHalls().stream().map(DetailConferenceHallDTO::of).toList());
        return detailBuildingDTO;

    }
}
