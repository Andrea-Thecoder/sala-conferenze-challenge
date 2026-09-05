package com.naxos.challenge.dto.search;

import java.util.Map;

import com.naxos.challenge.model.Building;
import io.ebean.ExpressionList;
import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BuildingSearchRequest extends BaseSearchRequest {

    {
        SORT_FIELDS = Map.of(
                "city", "city",
                "country", "country",
                "postalcode", "postalCode"
        );
    }

    @QueryParam("street")
    @Schema(description = "Filter by street (partial, case-insensitive match)", example = "Via Roma")
    private String street;

    @QueryParam("city")
    @Schema(description = "Filter by city (partial, case-insensitive match)", example = "Milano")
    private String city;

    @QueryParam("postalCode")
    @Schema(description = "Filter by postal code (partial match)", example = "20100")
    private String postalCode;

    @QueryParam("country")
    @Schema(description = "Filter by country (partial, case-insensitive match)", example = "Italy")
    private String country;

    public void applyFilters(ExpressionList<Building> exl) {
        if (StringUtils.isNotBlank(street)) {
            exl.ilike("street",  street + "%");
        }
        if (StringUtils.isNotBlank(city)) {
            exl.ilike("city", city + "%");
        }
        if (StringUtils.isNotBlank(postalCode)) {
            exl.eq("postalCode", postalCode );
        }
        if (StringUtils.isNotBlank(country)) {
            exl.ilike("country", country + "%");
        }
    }
}
