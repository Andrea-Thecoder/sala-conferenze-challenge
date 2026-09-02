package com.naxos.challenge.model;

import com.naxos.challenge.model.enumerator.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "app_user",
        indexes = {
                @Index(name = "idx_app_user_phone_number", columnList = "phone_number"),
                @Index(name = "idx_app_user_email",columnList = "email"),
                @Index(name = "idx_app_user_lastname", columnList = "last_name"),
                @Index(name = "idx_app_user_firstname_lastname", columnList = "first_name, last_name")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class User extends AbstractAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 9)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;


}
