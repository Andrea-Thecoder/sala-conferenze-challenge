package com.sala.challenge.model;

import com.sala.challenge.model.enumerator.Role;
import io.ebean.annotation.Index;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@Index(name = "idx_app_user_firstname_lastname", columnNames = {"first_name, last_name"})
public class User extends AbstractAudit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    @Index
    private String lastName;

    @Column(nullable = false)
    @Index
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 20)
    @Index
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 9)
    @Index
    private Role role;

    @Column(nullable = false)
    @Index
    private boolean active = false;


}
