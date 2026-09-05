package com.naxos.challenge.dto.building;

import com.naxos.challenge.model.Building;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor

public class BaseDetailBuildingDTO {

    protected UUID id;
    protected String street;
    protected String city;
    protected String postalCode;
    protected String country;

    public static BaseDetailBuildingDTO of (Building building) {
        BaseDetailBuildingDTO baseDetailBuildingDTO = new BaseDetailBuildingDTO();
        baseDetailBuildingDTO.setId(building.getId());
        baseDetailBuildingDTO.setStreet(building.getStreet());
        baseDetailBuildingDTO.setCity(building.getCity());
        baseDetailBuildingDTO.setPostalCode(building.getPostalCode());
        baseDetailBuildingDTO.setCountry(building.getCountry());
        return baseDetailBuildingDTO;

    }
}
