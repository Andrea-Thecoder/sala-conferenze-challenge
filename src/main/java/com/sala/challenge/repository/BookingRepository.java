package com.sala.challenge.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.sala.challenge.dto.search.BookingSearchRequest;
import com.sala.challenge.model.Booking;
import io.ebean.PagedList;

public interface BookingRepository extends GenericRepository<Booking, UUID> {

    Booking getBookingById(UUID id);

    List<Booking> findByUserId(UUID userId);

    boolean existsOverlapping(UUID conferenceHallId, LocalDateTime start, LocalDateTime end);

    boolean existsOverlapping(UUID conferenceHallId, LocalDateTime start, LocalDateTime end, UUID excludeBookingId);

    PagedList<Booking> search(BookingSearchRequest request, UUID userConstraint);
}
