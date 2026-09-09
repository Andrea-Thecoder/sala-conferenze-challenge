package com.sala.challenge.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sala.challenge.api.BuildingResource;
import com.sala.challenge.dto.PagedResultDTO;
import com.sala.challenge.dto.SimpleResultDTO;
import com.sala.challenge.dto.building.BaseDetailBuildingDTO;
import com.sala.challenge.dto.building.CreateBuildingDTO;
import com.sala.challenge.dto.building.DetailBuildingDTO;
import com.sala.challenge.dto.building.UpdateBuildingDTO;
import com.sala.challenge.dto.search.BuildingSearchRequest;
import com.sala.challenge.services.BuildingService;

@ExtendWith(MockitoExtension.class)
class BuildingResourceTest {

    @Mock
    BuildingService buildingService;

    @InjectMocks
    BuildingResource buildingResource;

    @Test
    void createBuilding_validDto_returnsCreatedIdInPayload() {
        CreateBuildingDTO dto = new CreateBuildingDTO();
        UUID createdId = UUID.randomUUID();
        when(buildingService.createBuilding(dto)).thenReturn(createdId);

        SimpleResultDTO<UUID> result = buildingResource.createBuilding(dto);

        assertThat(result.getPayload()).isEqualTo(createdId);
    }

    @Test
    void getBuildingById_validId_returnsServiceResult() {
        UUID buildingId = UUID.randomUUID();
        DetailBuildingDTO detail = new DetailBuildingDTO();
        when(buildingService.getBuildingById(buildingId)).thenReturn(detail);

        DetailBuildingDTO result = buildingResource.getBuildingById(buildingId);

        assertThat(result).isEqualTo(detail);
    }

    @Test
    void findAllBuildings_delegatesToServiceWithRequest() {
        BuildingSearchRequest request = new BuildingSearchRequest();
        PagedResultDTO<BaseDetailBuildingDTO> pagedResult = new PagedResultDTO<>();
        when(buildingService.findAllBuildings(request)).thenReturn(pagedResult);

        PagedResultDTO<BaseDetailBuildingDTO> result = buildingResource.findAllBuildings(request);

        assertThat(result).isEqualTo(pagedResult);
    }

    @Test
    void updateBuilding_validRequest_delegatesToService() {
        UUID buildingId = UUID.randomUUID();
        UpdateBuildingDTO dto = new UpdateBuildingDTO();

        buildingResource.updateBuilding(buildingId, dto);

        verify(buildingService).updateBuilding(buildingId, dto);
    }

    @Test
    void deleteBuilding_validId_delegatesToService() {
        UUID buildingId = UUID.randomUUID();

        buildingResource.deleteBuilding(buildingId);

        verify(buildingService).deleteBuilding(buildingId);
    }
}
