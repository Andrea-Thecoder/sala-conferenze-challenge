package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sala.challenge.dto.building.CreateBuildingDTO;
import com.sala.challenge.dto.building.DetailBuildingDTO;
import com.sala.challenge.dto.building.UpdateBuildingDTO;
import com.sala.challenge.dto.search.BuildingSearchRequest;
import com.sala.challenge.exception.ServiceException;
import com.sala.challenge.model.Building;
import com.sala.challenge.repository.BuildingRepository;
import com.sala.challenge.services.BuildingService;

import io.ebean.Database;
import io.ebean.Transaction;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    BuildingRepository buildingRepository;

    @Mock
    Database database;

    @Mock
    Transaction transaction;

    @InjectMocks
    BuildingService buildingService;

    private CreateBuildingDTO createDto() {
        CreateBuildingDTO dto = new CreateBuildingDTO();
        dto.setStreet("Via Roma 1");
        dto.setCity("Milano");
        dto.setPostalCode("20121");
        dto.setCountry("Italia");
        return dto;
    }

    private UpdateBuildingDTO updateDto() {
        UpdateBuildingDTO dto = new UpdateBuildingDTO();
        dto.setStreet("Corso Buenos Aires 10");
        dto.setCity("Milano");
        dto.setPostalCode("20124");
        dto.setCountry("Italia");
        return dto;
    }

    private Building building() {
        Building building = new Building();
        building.setId(UUID.randomUUID());
        building.setStreet("Via Roma 1");
        building.setCity("Milano");
        building.setPostalCode("20121");
        building.setCountry("Italia");
        return building;
    }

    // ---- createBuilding ----

    @Test
    void createBuilding_validDto_returnsGeneratedBuildingId() {
        UUID generatedId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);
        doAnswer(invocation -> {
            Building saved = invocation.getArgument(0);
            saved.setId(generatedId);
            return null;
        }).when(buildingRepository).save(any(), eq(transaction));

        UUID result = buildingService.createBuilding(createDto());

        assertThat(result).isEqualTo(generatedId);
    }

    @Test
    void createBuilding_transactionFails_throwsServiceException() {
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(buildingRepository).save(any(), eq(transaction));
        CreateBuildingDTO dto = createDto();

        Throwable thrown = catchThrowable(() -> buildingService.createBuilding(dto));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- getBuildingById ----

    @Test
    void getBuildingById_validId_returnsMappedDetail() {
        Building building = building();
        when(buildingRepository.getBuildingById(building.getId())).thenReturn(building);

        DetailBuildingDTO result = buildingService.getBuildingById(building.getId());

        assertThat(result.getId()).isEqualTo(building.getId());
    }

    // ---- findAllBuildings ----

    @Test
    void findAllBuildings_validRequest_delegatesToRepositorySearch() {
        BuildingSearchRequest request = new BuildingSearchRequest();
        var pagedList = TestPagedLists.<Building>empty();
        when(buildingRepository.search(request)).thenReturn(pagedList);

        buildingService.findAllBuildings(request);

        verify(buildingRepository).search(request);
    }

    // ---- updateBuilding ----

    @Test
    void updateBuilding_validRequest_updatesBuildingStreet() {
        Building building = building();
        when(buildingRepository.getBuildingById(building.getId())).thenReturn(building);
        when(database.beginTransaction()).thenReturn(transaction);

        buildingService.updateBuilding(building.getId(), updateDto());

        assertThat(building.getStreet()).isEqualTo("Corso Buenos Aires 10");
    }

    @Test
    void updateBuilding_transactionFails_throwsServiceException() {
        Building building = building();
        when(buildingRepository.getBuildingById(building.getId())).thenReturn(building);
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(buildingRepository).update(any(), eq(transaction));
        UUID buildingId = building.getId();
        UpdateBuildingDTO dto = updateDto();

        Throwable thrown = catchThrowable(() -> buildingService.updateBuilding(buildingId, dto));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }

    // ---- deleteBuilding ----

    @Test
    void deleteBuilding_validId_deletesBuilding() {
        UUID buildingId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);

        buildingService.deleteBuilding(buildingId);

        verify(buildingRepository).delete(buildingId, transaction);
    }

    @Test
    void deleteBuilding_transactionFails_throwsServiceException() {
        UUID buildingId = UUID.randomUUID();
        when(database.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("boom")).when(buildingRepository).delete(buildingId, transaction);

        Throwable thrown = catchThrowable(() -> buildingService.deleteBuilding(buildingId));

        assertThat(thrown).isInstanceOf(ServiceException.class);
    }
}
