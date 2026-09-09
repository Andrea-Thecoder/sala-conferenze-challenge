package com.sala.challenge.dto.search;

import java.util.Map;
import java.util.UUID;

import com.sala.challenge.model.ConferenceHall;
import io.ebean.ExpressionList;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
public class ConferenceHallSearchRequest extends BaseSearchRequest {

    {
        SORT_FIELDS = Map.of(
                "name", "name",
                "size", "size",
                "priceperhour", "pricePerHour",
                "floor", "floor"
        );
    }

    @QueryParam("name")
    @Schema(description = "Filter by name (partial, case-insensitive match)", example = "Aurora")
    private String name;

    @QueryParam("buildingId")
    @Schema(description = "Filter by building")
    private UUID buildingId;

    @QueryParam("minSize")
    @Schema(description = "Filter by minimum seating capacity", example = "5")
    private Integer minSize;

    @QueryParam("enabled")
    @Schema(description = "Filter by availability. Ignored for a CUSTOMER, who always sees enabled halls only.", example = "true")
    private Boolean enabled;

    public void applyFilters(ExpressionList<ConferenceHall> exl) {
        if (StringUtils.isNotBlank(name)) {
            exl.ilike("name", name + "%");
        }
        if (buildingId != null) {
            exl.eq("building.id", buildingId);
        }
        if (minSize != null) {
            exl.ge("size", minSize);
        }
        if (enabled != null) {
            exl.eq("enabled", enabled);
        }
    }
}
