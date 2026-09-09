package com.sala.challenge.model;

import io.ebean.annotation.Index;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "a_booking")
@Getter
@Setter
@NoArgsConstructor
@Index(name = "idx_booking_hall_time_range", columnNames = {"conference_hall_id, start_date_time, end_date_time"},unique = true)
public class Booking extends AbstractAudit {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_hall_id", nullable = false)
    private ConferenceHall conferenceHall;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_app_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @io.ebean.annotation.Index
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    @io.ebean.annotation.Index
    private LocalDateTime endDateTime;

    @Column(nullable = false, columnDefinition = "NUMERIC(12,2)")
    private BigDecimal totalCost;

    @Column(nullable = false)
    private boolean paid = false;
}
