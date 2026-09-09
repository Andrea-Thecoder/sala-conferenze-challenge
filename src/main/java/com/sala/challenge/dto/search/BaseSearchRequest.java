package com.sala.challenge.dto.search;

import io.ebean.ExpressionList;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Getter
@Setter
@Schema(description = "Base search request with pagination and sorting parameters")
public class BaseSearchRequest {

    private static final Pattern SAFE_SORT_FIELD = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    public Map<String, String> SORT_FIELDS = new HashMap<>();

    @QueryParam("page")
    @DefaultValue("1")
    @Min(1)
    @Schema(description = "Page number (1-based)", example = "1")
    protected int page;

    @QueryParam("size")
    @DefaultValue("20")
    @Min(1)
    @Max(100)
    @Schema(description = "Number of results per page", example = "20")
    protected int size;

    @QueryParam("sort")
    @Schema(description = "Field name to sort by (see endpoint docs for allowed values)", example = "name")
    protected String sort;

    @QueryParam("descending")
    @Schema(description = "Set to true to sort in descending order", example = "false")
    protected boolean descending;

    public void applySortAndPagination(ExpressionList<?> exl, String defaultSort) {
        applySort(exl, defaultSort);
        applyPagination(exl);
    }

    private void applySort(ExpressionList<?> exl, String defaultSort) {
        String direction = descending ? " desc" : " asc";
        String resolved = resolveSortField(this.getSort());
        if (StringUtils.isNotBlank(resolved)) {
            exl.orderBy(resolved + direction);
        } else if (StringUtils.isNotBlank(defaultSort)) {
            exl.orderBy(defaultSort);
        } else {
            exl.orderById(true);
        }
    }

    /**
     * Resolves the client-supplied sort key to a safe Ebean property path.
     * If {@link #SORT_FIELDS} is populated, only whitelisted keys are accepted — the mapped value is
     * server-controlled and trusted as-is (may be a multi-segment path like "ship.name"), and an
     * unmapped key falls back to the default sort instead of leaking the raw client value through.
     * If no whitelist is defined for this request, the raw value is validated against
     * {@link #SAFE_SORT_FIELD} to reject anything that isn't a plain identifier.
     */
    private String resolveSortField(String order) {
        if (StringUtils.isBlank(order)) {
            return null;
        }
        String key = order.trim();
        if (ObjectUtils.isNotEmpty(SORT_FIELDS)) {
            return SORT_FIELDS.get(key);
        }
        if (!SAFE_SORT_FIELD.matcher(key).matches()) {
            return null;
        }
        return key;
    }

    public void applyPagination(ExpressionList<?> query) {
        int rows = this.getSize();
        int offset = (this.getPage() - 1) * rows;
        query.setFirstRow(offset).setMaxRows(rows);
    }
}
