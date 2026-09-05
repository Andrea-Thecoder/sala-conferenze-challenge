package com.naxos.challenge.repository;

import java.util.Optional;
import java.util.UUID;

import com.naxos.challenge.exception.ServiceException;
import com.naxos.challenge.model.ConferenceHall;
import io.ebean.Database;
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
}
