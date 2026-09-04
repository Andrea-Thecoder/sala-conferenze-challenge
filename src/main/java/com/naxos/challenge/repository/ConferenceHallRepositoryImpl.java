package com.naxos.challenge.repository;

import com.naxos.challenge.model.ConferenceHall;
import io.ebean.Database;
import io.ebean.Transaction;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConferenceHallRepositoryImpl implements ConferenceHallRepository {

    @Inject
    Database db;

    @Override
    public Optional<ConferenceHall> findById(UUID uuid) {
        return Optional.empty();
    }

    @Override
    public List<ConferenceHall> findAll(int page, int size) {
        return db.find(ConferenceHall.class).findList();

    }

    @Override
    public void save(ConferenceHall entity, Transaction tx) {
        entity.save(tx);
    }

    @Override
    public void update(ConferenceHall entity, Transaction tx) {
        entity.update(tx);
    }

    @Override
    public void delete(UUID uuid, Transaction tx) {

    }

    @Override
    public long count() {
        return db.find(ConferenceHall.class).findCount();
    }
}
