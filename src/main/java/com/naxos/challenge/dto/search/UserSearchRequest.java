package com.naxos.challenge.dto.search;

import com.naxos.challenge.model.User;
import com.naxos.challenge.model.enumerator.Role;
import io.ebean.ExpressionList;
import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchRequest extends BaseSearchRequest {


    {
        // I valori sono i property path Ebean reali sull'entity (case-sensitive,
        // es. "lastName"), non il nome del query param client-side ("lastname").
        SORT_FIELDS = Map.of(
                "lastname", "lastName",
                "active", "active",
                "role", "role"
        );
    }

    @QueryParam("name")
    @Schema(description = "Filter by first or last name (partial, case-insensitive match)", example = "Rossi")
    private String name;

    @QueryParam("email")
    @Schema(description = "Filter by email (partial, case-insensitive match)", example = "mario")
    private String email;

    @QueryParam("phoneNumber")
    @Schema(description = "Filter by phone number (partial match)", example = "3331234567")
    private String phoneNumber;

    @QueryParam("active")
    @Schema(description = "Filter by account status: true for active, false for inactive/not yet approved")
    private Boolean active;

    @QueryParam("role")
    @Schema(description = "Filter by role", example = "CUSTOMER")
    private Role role;

    public void applyFilters(ExpressionList<User> exl) {
        if (StringUtils.isNotBlank(name)) {
            String pattern = name + "%";
            exl.or()
                    .ilike("lastName", pattern)
                    .ilike("firstName", pattern)
                    .endOr();
        }

        if (StringUtils.isNotBlank(email)) {
            exl.ilike("email", email + "%");
        }

        if (StringUtils.isNotBlank(phoneNumber)) {
            exl.eq("phoneNumber", phoneNumber);
        }
        if (active != null) {
            exl.eq("active", active);
        }
        if (role != null) {
            exl.eq("role", role);
        }
    }

}
