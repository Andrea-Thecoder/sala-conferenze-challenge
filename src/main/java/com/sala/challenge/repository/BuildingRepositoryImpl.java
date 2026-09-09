package com.sala.challenge.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sala.challenge.dto.search.BuildingSearchRequest;
import com.sala.challenge.exception.ServiceException;
import com.sala.challenge.model.Building;
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.PagedList;
import io.ebean.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class BuildingRepositoryImpl implements BuildingRepository {

    @Inject
    Database database;

    @Override
    public Optional<Building> findById(UUID id) {
        return Optional.ofNullable(database.find(Building.class, id));
    }

    @Override
    public Building getBuildingById(UUID id) {
        return findById(id).orElseThrow(() -> {
            log.error("BuildingRepository - getBuildingById: Building {} not found", id);
            return new ServiceException("Building not found");
        });
    }

    @Override
    public List<Building> findAll(int page, int size) {
        return database.find(Building.class)
                .setFirstRow(page * size)
                .setMaxRows(size)
                .findList();
    }

    @Override
    public void save(Building entity, Transaction tx) {
        entity.save(tx);
    }

    @Override
    public void update(Building entity, Transaction tx) {
        log.info("BuildingRepository - update: Update building with id {}", entity.getId());
        entity.update(tx);
    }

    @Override
    public void delete(UUID id, Transaction tx) {
        log.info("BuildingRepository - delete: Delete building with id {}", id);
        Building building = getBuildingById(id);
        building.delete(tx);
    }

    @Override
    public long count() {
        return database.find(Building.class).findCount();
    }

    @Override
    public PagedList<Building> search(BuildingSearchRequest request) {
        ExpressionList<Building> exl = database.find(Building.class).where();
        request.applyFilters(exl);
        request.applySortAndPagination(exl, "city");
        return exl.findPagedList();
    }
}
