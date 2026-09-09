package com.sala.challenge.integration;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;

import java.math.BigDecimal;

import com.sala.challenge.dto.auth.AuthTokenDTO;
import com.sala.challenge.dto.auth.LoginCredentialsDTO;
import com.sala.challenge.model.AppRefreshToken;
import com.sala.challenge.model.Booking;
import com.sala.challenge.model.Building;
import com.sala.challenge.model.ConferenceHall;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;
import com.sala.challenge.security.PasswordEncoder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/**
 * Base per i test di integrazione: componenti reali (Postgres, Redis), nessun
 * mock — richiede `docker-compose up -d` attivo. Ogni sottoclasse crea i propri
 * utenti in {@code @BeforeEach}/dentro il test e li ripulisce in
 * {@code @AfterEach}, per restare indipendente dalle altre.
 */
@QuarkusTest
public abstract class AbstractIntegrationTest {

    private static final String RAW_PASSWORD = "Password1!";
    private static final int TEST_BCRYPT_ROUNDS = 4;

    @Inject
    protected io.ebean.Database database;

    @BeforeAll
    static void setBasePath() {
        RestAssured.basePath = "/api/v1/sala-backend-challenge";
    }

    protected User seedUser(String email, Role role, boolean active) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPhoneNumber("+3900000" + System.nanoTime() % 10000);
        user.setPassword(PasswordEncoder.hash(RAW_PASSWORD, TEST_BCRYPT_ROUNDS));
        user.setRole(role);
        user.setActive(active);
        user.save();
        return user;
    }

    /**
     * app_refresh_token e a_booking referenziano l'utente con FK "on delete
     * restrict" (stessa scelta GDPR-safe di UserService.anonymizeUser): vanno
     * ripulite per prime, altrimenti la delete dell'utente fallisce.
     */
    protected void deleteUser(UUID userId) {
        database.find(AppRefreshToken.class).where().eq("user.id", userId).delete();
        database.find(Booking.class).where().eq("user.id", userId).delete();
        database.find(User.class, userId).delete();
    }

    protected Building seedBuilding() {
        Building building = new Building();
        building.setStreet("Via Roma 1");
        building.setCity("Milano");
        building.setPostalCode("20121");
        building.setCountry("Italia");
        building.save();
        return building;
    }

    protected void deleteBuilding(UUID buildingId) {
        database.find(Building.class, buildingId).delete();
    }

    protected ConferenceHall seedConferenceHall(Building building, boolean enabled) {
        ConferenceHall conferenceHall = new ConferenceHall();
        conferenceHall.setName("Sala Test");
        conferenceHall.setSize(10);
        conferenceHall.setPricePerHour(new BigDecimal("50.00"));
        conferenceHall.setBuilding(building);
        conferenceHall.setFloor(1);
        conferenceHall.setRoomNumber("101");
        conferenceHall.setEnabled(enabled);
        conferenceHall.save();
        return conferenceHall;
    }

    protected void deleteConferenceHall(UUID conferenceHallId) {
        database.find(Booking.class).where().eq("conferenceHall.id", conferenceHallId).delete();
        database.find(ConferenceHall.class, conferenceHallId).delete();
    }

    protected Booking seedBooking(ConferenceHall conferenceHall, User user, LocalDateTime start, LocalDateTime end) {
        Booking booking = new Booking();
        booking.setConferenceHall(conferenceHall);
        booking.setUser(user);
        booking.setStartDateTime(start);
        booking.setEndDateTime(end);
        booking.setTotalCost(new BigDecimal("50.00"));
        booking.save();
        return booking;
    }

    protected void deleteBooking(UUID bookingId) {
        database.find(Booking.class, bookingId).delete();
    }

    protected String loginAndGetAccessToken(String email) {
        LoginCredentialsDTO credentials = new LoginCredentialsDTO();
        credentials.setEmail(email);
        credentials.setPassword(RAW_PASSWORD);

        AuthTokenDTO tokens = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().as(AuthTokenDTO.class);

        return tokens.getAccessToken();
    }
}
