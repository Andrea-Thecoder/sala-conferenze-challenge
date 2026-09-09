package com.sala.challenge.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sala.challenge.dto.search.BookingSearchRequest;
import com.sala.challenge.exception.ServiceException;
import com.sala.challenge.model.Booking;
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.PagedList;
import io.ebean.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class BookingRepositoryImpl implements BookingRepository {

    @Inject
    Database database;

    @Override
    public Optional<Booking> findById(UUID id) {
        return Optional.ofNullable(database.find(Booking.class, id));
    }

    @Override
    public Booking getBookingById(UUID id) {
        return findById(id).orElseThrow(() -> {
            log.error("BookingRepository - getBookingById: Booking {} not found", id);
            return new ServiceException("Booking not found");
        });
    }

    @Override
    public List<Booking> findAll(int page, int size) {
        return database.find(Booking.class)
                .setFirstRow(page * size)
                .setMaxRows(size)
                .findList();
    }

    @Override
    public void save(Booking entity, Transaction tx) {
        entity.save(tx);
    }

    @Override
    public void update(Booking entity, Transaction tx) {
        log.info("BookingRepository - update: Update booking with id {}", entity.getId());
        entity.update(tx);
    }

    @Override
    public void delete(UUID id, Transaction tx) {
        log.info("BookingRepository - delete: Delete booking with id {}", id);
        Booking booking = getBookingById(id);
        booking.delete(tx);
    }

    @Override
    public long count() {
        return database.find(Booking.class).findCount();
    }

    @Override
    public List<Booking> findByUserId(UUID userId) {
        return database.find(Booking.class)
                .where().eq("user.id", userId)
                .orderBy("startDateTime desc")
                .findList();
    }

    @Override
    public boolean existsOverlapping(UUID conferenceHallId, LocalDateTime start, LocalDateTime end){
        return existsOverlapping(conferenceHallId, start, end, null);
    }

    @Override
    public boolean existsOverlapping(UUID conferenceHallId, LocalDateTime start, LocalDateTime end, UUID excludeBookingId) {
        ExpressionList<Booking> exl = database.find(Booking.class)
                .where()
                .eq("conferenceHall.id", conferenceHallId)
                .lt("startDateTime", end)
                .gt("endDateTime", start);
        if (excludeBookingId != null) {
            exl.ne("id", excludeBookingId);
        }
        return exl.exists();
    }

    @Override
    public PagedList<Booking> search(BookingSearchRequest request, UUID userConstraint) {
        ExpressionList<Booking> exl = database.find(Booking.class).where();
        request.applyFilters(exl, userConstraint);
        request.applySortAndPagination(exl, "startDateTime desc");
        return exl.findPagedList();
    }
}
