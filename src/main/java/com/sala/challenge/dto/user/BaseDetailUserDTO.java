package com.sala.challenge.dto.user;

import java.util.UUID;

import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "User's public profile — never includes the password hash")
public class BaseDetailUserDTO {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Role role;
    private boolean active;

    public static BaseDetailUserDTO of(User user) {
        BaseDetailUserDTO dto = new BaseDetailUserDTO();
        dto.populate(user);
        return dto;
    }

    protected void populate(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.role = user.getRole();
        this.active = user.isActive();
    }
}
