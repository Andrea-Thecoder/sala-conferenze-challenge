package com.sala.challenge.dto.conference;

import java.math.BigDecimal;

import com.sala.challenge.model.ConferenceHall;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Payload used to update a conference hall")
public class UpdateConferenceHallDTO {

    @NotBlank(message = "Name is required")
    @Schema(description = "Conference hall name", example = "Sala Aurora")
    private String name;

    @Schema(description = "Optional free-text note")
    private String note;

    @NotNull(message = "Size is required")
    @Min(value = 2, message = "Size must be at least 2")
    @Schema(description = "Seating capacity", example = "10")
    private Integer size;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price per hour must be greater than 0")
    @Schema(description = "Price per hour", example = "50.00")
    private BigDecimal pricePerHour;

    @NotNull(message = "Floor is required")
    @Schema(description = "Floor number", example = "1")
    private Integer floor;

    @NotBlank(message = "Room number is required")
    @Size(max = 20, message = "Room number must be at most 20 characters")
    @Schema(description = "Room number", example = "101")
    private String roomNumber;

    public void toUpdate(ConferenceHall conferenceHall) {
        conferenceHall.setName(name);
        conferenceHall.setNote(note);
        conferenceHall.setSize(size);
        conferenceHall.setPricePerHour(pricePerHour);
        conferenceHall.setFloor(floor);
        conferenceHall.setRoomNumber(roomNumber);
    }
}
