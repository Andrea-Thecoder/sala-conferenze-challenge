package com.naxos.challenge.services;

import java.util.UUID;

import com.naxos.challenge.dto.PagedResultDTO;
import com.naxos.challenge.dto.building.CreateBuildingDTO;
import com.naxos.challenge.dto.building.BaseDetailBuildingDTO;
import com.naxos.challenge.dto.building.DetailBuildingDTO;
import com.naxos.challenge.dto.building.UpdateBuildingDTO;
import com.naxos.challenge.dto.search.BuildingSearchRequest;
import com.naxos.challenge.exception.ServiceException;
import com.naxos.challenge.model.Building;
import com.naxos.challenge.repository.BuildingRepository;
import io.ebean.Database;
import io.ebean.PagedList;
import io.ebean.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class BuildingService {

    @Inject
    BuildingRepository buildingRepository;

    @Inject
    Database database;

    public UUID createBuilding(CreateBuildingDTO dto) {
        log.info("BuildingService - createBuilding : Creating new building");
        Building building = dto.toEntity();
        try (Transaction tx = database.beginTransaction()) {
            buildingRepository.save(building, tx);
            tx.commit();
            log.info("BuildingService - createBuilding : Created building {}", building.getId());
            return building.getId();
        } catch (Exception e) {
            log.error("BuildingService - createBuilding : Error creating building", e);
            throw new ServiceException("Error while creating building. Try again later.");
        }
    }

    public DetailBuildingDTO getBuildingById(UUID id) {
        return DetailBuildingDTO.of(buildingRepository.getBuildingById(id));
    }

    public PagedResultDTO<BaseDetailBuildingDTO> findAllBuildings(BuildingSearchRequest request) {
        log.info("BuildingService - findAllBuildings : Searching buildings, page {} size {}", request.getPage(), request.getSize());
        PagedList<Building> pagedList = buildingRepository.search(request);
        return PagedResultDTO.of(pagedList, BaseDetailBuildingDTO::of);
    }

    public void updateBuilding(UUID id, UpdateBuildingDTO dto) {
        log.info("BuildingService - updateBuilding : Updating building {}", id);
        try (Transaction tx = database.beginTransaction()) {
            Building building = buildingRepository.getBuildingById(id);
            dto.toUpdate(building);
            buildingRepository.update(building, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("BuildingService - updateBuilding : Error updating building {}", id, e);
            throw new ServiceException("Error while updating building. Try again later.");
        }
        log.info("BuildingService - updateBuilding : Updated building {}", id);
    }

    public void deleteBuilding(UUID id) {
        log.info("BuildingService - deleteBuilding : Deleting building {}", id);
        try (Transaction tx = database.beginTransaction()) {
            buildingRepository.delete(id, tx);
            tx.commit();
        } catch (Exception e) {
            log.error("BuildingService - deleteBuilding : Error deleting building {}", id, e);
            throw new ServiceException("Error while deleting building. Try again later.");
        }
    }
}
