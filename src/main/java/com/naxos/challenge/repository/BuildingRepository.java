package com.naxos.challenge.repository;

import java.util.UUID;

import com.naxos.challenge.dto.search.BuildingSearchRequest;
import com.naxos.challenge.model.Building;
import io.ebean.PagedList;

public interface BuildingRepository extends GenericRepository<Building, UUID> {

    Building getBuildingById(UUID id);

    PagedList<Building> search(BuildingSearchRequest request);
}
