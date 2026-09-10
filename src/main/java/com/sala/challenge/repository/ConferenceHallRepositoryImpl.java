package com.sala.challenge.repository;

import java.util.Optional;
import java.util.UUID;

import com.sala.challenge.dto.search.ConferenceHallSearchRequest;
import com.sala.challenge.exception.ServiceException;
import com.sala.challenge.model.ConferenceHall;
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.PagedList;
import io.ebean.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ConferenceHallRepositoryImpl implements ConferenceHallRepository {

    @Inject
    Database db;

    @Override
    public Optional<ConferenceHall> findById(UUID id) {
        return Optional.ofNullable(db.find(ConferenceHall.class, id));
    }

    @Override
    public ConferenceHall getConferenceHallById(UUID id) {
        return findById(id).orElseThrow(() -> {
            log.error("ConferenceHallRepository - getConferenceHallById: Conference hall {} not found", id);
            return new ServiceException("Conference hall not found");
        });
    }

    @Override
    public boolean conferenceHallAvailable(UUID conferenceHallId) {
        return db.find(ConferenceHall.class).where()
                .idEq(conferenceHallId)
                .eq("enabled", true)
                .exists();
    }

    @Override
    public void save(ConferenceHall entity, Transaction tx) {
        entity.save(tx);
    }

    @Override
    public void update(ConferenceHall entity, Transaction tx) {
        log.info("ConferenceHallRepository - update: Update conference hall with id {}", entity.getId());
        entity.update(tx);
    }

    @Override
    public void delete(UUID id, Transaction tx) {
        log.info("ConferenceHallRepository - delete: Delete conference hall with id {}", id);
        ConferenceHall conferenceHall = getConferenceHallById(id);
        conferenceHall.delete(tx);
    }

    @Override
    public PagedList<ConferenceHall> search(ConferenceHallSearchRequest request) {
        ExpressionList<ConferenceHall> exl = db.find(ConferenceHall.class).where();
        request.applyFilters(exl);
        request.applySortAndPagination(exl, "name");
        return exl.findPagedList();
    }
}
