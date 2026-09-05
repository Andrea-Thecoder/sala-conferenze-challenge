package com.naxos.challenge.dto.building;

import com.naxos.challenge.model.Building;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Payload used to create a new building")
public class CreateBuildingDTO {

    @NotBlank(message = "Street is required")
    @Schema(description = "Street address", example = "Via Roma 1")
    private String street;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be at most 100 characters")
    @Schema(description = "City", example = "Milano")
    private String city;

    @NotBlank(message = "Postal code is required")
    @Size(max = 5, message = "Postal code must be at most 5 characters")
    @Schema(description = "Postal code", example = "20121")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must be at most 100 characters")
    @Schema(description = "Country", example = "Italia")
    private String country;

    public Building toEntity() {
        Building building = new Building();
        building.setStreet(street);
        building.setCity(city);
        building.setPostalCode(postalCode);
        building.setCountry(country);
        return building;
    }
}
