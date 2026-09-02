package com.naxos.challenge.model;

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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Lob
    private String note;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 2")
    private Integer size = 2;

    @Column(nullable = false,columnDefinition = "NUMERIC(12,2)")
    private BigDecimal pricePerHour;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(nullable = false)
    private Integer floor;

    @Column(nullable = false, length = 20)
    private String roomNumber;

    @Column(nullable = false)
    private boolean enabled = true;

}
