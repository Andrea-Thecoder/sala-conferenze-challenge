package com.sala.challenge.repository;

import java.util.UUID;

import com.sala.challenge.dto.search.BuildingSearchRequest;
import com.sala.challenge.model.Building;
import io.ebean.PagedList;

public interface BuildingRepository extends GenericRepository<Building, UUID> {

    Building getBuildingById(UUID id);

    PagedList<Building> search(BuildingSearchRequest request);
}
