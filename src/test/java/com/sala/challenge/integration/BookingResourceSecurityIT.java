package com.sala.challenge.integration;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sala.challenge.model.Booking;
import com.sala.challenge.model.Building;
import com.sala.challenge.model.ConferenceHall;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * BookingResource è @Authenticated (qualunque ruolo), ma l'ownership self-vs-admin
 * è verificata dentro BookingService contro il subject reale del JWT — e gli
 * endpoint /admin/... sono @RolesAllowed({"ADMIN","ORGANIZER"}). Nessuno dei due
 * meccanismi è verificabile con un Resource unit test a service mockato.
 */
@QuarkusTest
class BookingResourceSecurityIT extends AbstractIntegrationTest {

    private User customer;
    private User otherCustomer;
    private User organizer;
    private User bookingOwner;
    private Building building;
    private ConferenceHall hall;
    private Booking booking;

    @AfterEach
    void cleanup() {
        if (booking != null) deleteBooking(booking.getId());
        if (hall != null) deleteConferenceHall(hall.getId());
        if (building != null) deleteBuilding(building.getId());
        if (customer != null) deleteUser(customer.getId());
        if (otherCustomer != null) deleteUser(otherCustomer.getId());
        if (organizer != null) deleteUser(organizer.getId());
        if (bookingOwner != null) deleteUser(bookingOwner.getId());
    }

    @Test
    void createBookings_noToken_returnsUnauthorized() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"conferenceHallReservationDTOs\":[]}")
                .when().post("/bookings")
                .then()
                .statusCode(401);
    }

    @Test
    void createBookingsForUser_asCustomer_returnsForbidden() {
        String email = "bkr-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        otherCustomer = seedUser("bkr-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"conferenceHallReservationDTOs\":[]}")
                .when().post("/bookings/admin/" + otherCustomer.getId())
                .then()
                .statusCode(403);
    }

    @Test
    void getBookingById_customerTargetingAnotherUsersBooking_returnsForbidden() {
        String email = "bkr-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        otherCustomer = seedUser("bkr-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        building = seedBuilding();
        hall = seedConferenceHall(building, true);
        booking = seedBooking(hall, otherCustomer, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().get("/bookings/" + booking.getId())
                .then()
                .statusCode(403);
    }

    @Test
    void getBookingById_ownerCustomer_returnsSuccess() {
        String email = "bkr-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        building = seedBuilding();
        hall = seedConferenceHall(building, true);
        booking = seedBooking(hall, customer, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().get("/bookings/" + booking.getId())
                .then()
                .statusCode(200);
    }

    @Test
    void deleteBookingForUser_asCustomer_returnsForbidden() {
        String email = "bkr-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        otherCustomer = seedUser("bkr-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        building = seedBuilding();
        hall = seedConferenceHall(building, true);
        booking = seedBooking(hall, otherCustomer, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/bookings/admin/" + otherCustomer.getId() + "/" + booking.getId())
                .then()
                .statusCode(403);
    }

    @Test
    void updateBookingForUser_asOrganizerBookingNotOwnedByGivenUser_returnsBadRequest() {
        organizer = seedUser("bkr-" + UUID.randomUUID() + "@example.com", Role.ORGANIZER, true);
        otherCustomer = seedUser("bkr-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        bookingOwner = seedUser("bkr-" + UUID.randomUUID() + "@example.com", Role.CUSTOMER, true);
        building = seedBuilding();
        hall = seedConferenceHall(building, true);
        booking = seedBooking(hall, bookingOwner, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        String token = loginAndGetAccessToken(organizer.getEmail());
        String newStart = LocalDateTime.now().plusDays(3).withNano(0).toString();
        String newEnd = LocalDateTime.now().plusDays(3).plusHours(1).withNano(0).toString();

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"startDateTime\":\"" + newStart + "\",\"endDateTime\":\"" + newEnd + "\"}")
                .when().patch("/bookings/admin/" + otherCustomer.getId() + "/" + booking.getId())
                .then()
                .statusCode(400);
    }
}
