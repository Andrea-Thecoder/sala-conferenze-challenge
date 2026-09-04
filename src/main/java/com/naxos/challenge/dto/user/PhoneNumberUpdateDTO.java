package com.naxos.challenge.dto.user;

import com.naxos.challenge.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Payload to update a user's phone number")
public class PhoneNumberUpdateDTO {

    @NotBlank(message = "Phone number is required")
    @Size(max = 16, message = "Phone number must be at most 16 characters, including the optional leading '+'")
    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "Phone number must contain only digits, with at most one optional leading '+'"
    )
    @Schema(description = "Mobile phone number, optional leading '+' followed by 7 to 15 digits (16 characters max in total)", example = "+393331234567")
    private String phoneNumber;

    public void toUpdate(User user) {
        user.setPhoneNumber(phoneNumber);
    }
}
