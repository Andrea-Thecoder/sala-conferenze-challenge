package com.sala.challenge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.annotation.Index;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "building")
@Getter
@Setter
@NoArgsConstructor

public class Building extends AbstractAudit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    @Index
    private String street;

    @Column(nullable = false, length = 100)
    @Index
    private String city;

    @Column(nullable = false, length = 5)
    @Index
    private String postalCode;

    @Column(nullable = false, length = 100)
    @Index
    private String country;

    @OneToMany(mappedBy = "building", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ConferenceHall> conferenceHalls = new ArrayList<>();
}
