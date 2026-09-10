package com.sala.challenge.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sala.challenge.model.Booking;
import com.sala.challenge.model.Building;
import com.sala.challenge.model.ConferenceHall;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;

import io.ebean.SqlRow;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Verifica end-to-end (@History, Ebean) che cancellare una prenotazione non ne
 * distrugga i dati contabili: la riga pre-cancellazione deve sopravvivere in
 * a_booking_history. Impossibile da testare con repository mockati — la scrittura
 * su "*_history" avviene via trigger Postgres, invisibile a qualunque unit test.
 */
@QuarkusTest
class BookingHistoryIT extends AbstractIntegrationTest {

    private User customer;
    private Building building;
    private ConferenceHall conferenceHall;
    private Booking booking;

    @AfterEach
    void cleanup() {
        // La prenotazione viene già cancellata dalla chiamata HTTP nel test stesso
        // (DELETE /bookings/{id}) — qui si ripulisce solo la riga di storico rimasta
        // e le altre entity seedate.
        if (booking != null) database.sqlUpdate("delete from a_booking_history where id = ?")
                .setParameter(1, booking.getId()).execute();
        if (conferenceHall != null) deleteConferenceHall(conferenceHall.getId());
        if (building != null) deleteBuilding(building.getId());
        if (customer != null) deleteUser(customer.getId());
    }

    @Test
    void deleteBooking_paidBooking_preservesAccountingDataInHistory() {
        customer = seedUser("history-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        building = seedBuilding();
        conferenceHall = seedConferenceHall(building, true);
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        booking = new Booking();
        booking.setConferenceHall(conferenceHall);
        booking.setUser(customer);
        booking.setStartDateTime(start);
        booking.setEndDateTime(start.plusHours(2));
        booking.setTotalCost(new BigDecimal("100.00"));
        booking.setPaid(true);
        booking.save();
        String token = loginAndGetAccessToken(customer.getEmail());

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/bookings/" + booking.getId())
                .then().statusCode(200);

        SqlRow historyRow = database.sqlQuery("select paid, total_cost, upper(sys_period) is not null as closed "
                        + "from a_booking_history where id = :id")
                .setParameter("id", booking.getId())
                .findOne();
        assertThat(historyRow).isNotNull();
        assertThat(historyRow.getBoolean("paid")).isTrue();
        assertThat(historyRow.getBigDecimal("total_cost")).isEqualByComparingTo("100.00");
        assertThat(historyRow.getBoolean("closed")).isTrue();
    }
}
