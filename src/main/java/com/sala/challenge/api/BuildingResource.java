package com.sala.challenge.api;

import java.util.UUID;

import com.sala.challenge.dto.PagedResultDTO;
import com.sala.challenge.dto.SimpleResultDTO;
import com.sala.challenge.dto.building.CreateBuildingDTO;
import com.sala.challenge.dto.building.BaseDetailBuildingDTO;
import com.sala.challenge.dto.building.DetailBuildingDTO;
import com.sala.challenge.dto.building.UpdateBuildingDTO;
import com.sala.challenge.dto.search.BuildingSearchRequest;
import com.sala.challenge.exception.ExceptionResponse;
import com.sala.challenge.services.BuildingService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/buildings")
@RolesAllowed({"ADMIN","ORGANIZER"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Building", description = "Building management")
@Slf4j
public class BuildingResource {

    @Inject
    BuildingService buildingService;

    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Create a new building", description = "Creates a new building. Requires the ADMIN role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Building created",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<UUID> createBuilding(
            @RequestBody(description = "Building data", required = true)
            @Valid CreateBuildingDTO dto) {
        log.info("BuildingResource - createBuilding : Creating new building");
        UUID buildingId = buildingService.createBuilding(dto);
        return SimpleResultDTO.<UUID>builder()
                .payload(buildingId)
                .message("Building successfully created")
                .build();
    }

    @GET
    @Path("/{buildingId}")
    @Operation(summary = "Get a building's detail", description = "Returns the given building's detail.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Building detail retrieved",
                    content = @Content(schema = @Schema(implementation = DetailBuildingDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "400", description = "Building not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public DetailBuildingDTO getBuildingById(
            @Schema(description = "ID of the building to fetch", type = SchemaType.STRING, format = "uuid")
            @PathParam("buildingId") UUID buildingId) {
        log.info("BuildingResource - getBuildingById ");
        return buildingService.getBuildingById(buildingId);
    }

    @GET
    @Operation(summary = "List buildings", description = "Paginated listing of all buildings.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Buildings retrieved",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public PagedResultDTO<BaseDetailBuildingDTO> findAllBuildings(
            @BeanParam BuildingSearchRequest request) {
        log.info("BuildingResource - findAllBuildings : page {} size {}", request.getPage(), request.getSize());
        return buildingService.findAllBuildings(request);
    }

    @PATCH
    @Path("/{buildingId}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Update a building", description = "Updates the address of the given building. Requires the ADMIN role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Building updated",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload, or building not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> updateBuilding(
            @Schema(description = "ID of the building to update", type = SchemaType.STRING, format = "uuid")
            @PathParam("buildingId") UUID buildingId,
            @RequestBody(description = "Updated address", required = true)
            @Valid UpdateBuildingDTO dto) {
        log.info("BuildingResource - updateBuilding : Updating building {}", buildingId);
        buildingService.updateBuilding(buildingId, dto);
        return SimpleResultDTO.<Void>builder().message("Building updated").build();
    }

    @DELETE
    @Path("/{buildingId}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Delete a building", description = "Permanently deletes a building. Requires the ADMIN role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Building deleted",
                    content = @Content(schema = @Schema(implementation = SimpleResultDTO.class))),
            @APIResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @APIResponse(responseCode = "400", description = "Building not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public SimpleResultDTO<Void> deleteBuilding(
            @Schema(description = "ID of the building to delete", type = SchemaType.STRING, format = "uuid")
            @PathParam("buildingId") UUID buildingId) {
        log.info("BuildingResource - deleteBuilding : Deleting building {}", buildingId);
        buildingService.deleteBuilding(buildingId);
        return SimpleResultDTO.<Void>builder().message("Building deleted").build();
    }
}
