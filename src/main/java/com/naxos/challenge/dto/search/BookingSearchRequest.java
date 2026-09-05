package com.naxos.challenge.dto.search;

import java.util.Map;
import java.util.UUID;

import com.naxos.challenge.model.Booking;
import io.ebean.ExpressionList;
import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookingSearchRequest extends BaseSearchRequest {

    {
        SORT_FIELDS = Map.of(
                "startdatetime", "startDateTime",
                "totalcost", "totalCost",
                "paid", "paid"
        );
    }

    @QueryParam("userId")
    @Schema(description = "Filter by the user who made the booking (ADMIN/ORGANIZER only — ignored for a CUSTOMER, who always sees only their own bookings)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID userId;

    @QueryParam("conferenceHallId")
    @Schema(description = "Filter by conference hall", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID conferenceHallId;

    @QueryParam("paid")
    @Schema(description = "Filter by payment status")
    private Boolean paid;

    public void applyFilters(ExpressionList<Booking> exl, UUID userConstraint) {
        if (userConstraint != null) {
            exl.eq("user.id", userConstraint);
        }
        if (conferenceHallId != null) {
            exl.eq("conferenceHall.id", conferenceHallId);
        }
        if (paid != null) {
            exl.eq("paid", paid);
        }
    }
}
