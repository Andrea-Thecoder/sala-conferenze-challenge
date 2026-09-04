package com.naxos.challenge.dto.user;

import java.util.UUID;

import com.naxos.challenge.model.User;
import com.naxos.challenge.model.enumerator.Role;
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
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());
        return dto;
    }
}
