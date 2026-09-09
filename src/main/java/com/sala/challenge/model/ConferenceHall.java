package com.sala.challenge.model;

import io.ebean.annotation.Index;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "conference_hall")
@Getter
@Setter
@NoArgsConstructor
public class ConferenceHall extends AbstractAudit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    @Index
    private String name;

    @Lob
    private String note;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 2")
    @Index
    private Integer size = 2;

    @Column(nullable = false,columnDefinition = "NUMERIC(12,2)")
    @Index
    private BigDecimal pricePerHour;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(nullable = false)
    private Integer floor;

    @Column(nullable = false, length = 20)
    private String roomNumber;

    @Column(nullable = false)
    @Index
    private boolean enabled = true;

}
