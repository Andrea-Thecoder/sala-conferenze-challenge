package com.sala.challenge.integration;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sala.challenge.model.Building;
import com.sala.challenge.model.ConferenceHall;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Copre esattamente ciò che gli unit test (Resource con service mockato) non
 * possono verificare: l'enforcement reale di {@code @Authenticated}/{@code @RolesAllowed}
 * da parte del container JAX-RS, e il fix del finding #2 di CODE_REVIEW.md
 * (enabled-filter su GET /conference-halls/{id}) end-to-end su HTTP vero.
 */
@QuarkusTest
class ConferenceHallSecurityIT extends AbstractIntegrationTest {

    private User customer;
    private User admin;
    private User organizer;
    private Building building;
    private ConferenceHall enabledHall;
    private ConferenceHall disabledHall;

    @AfterEach
    void cleanup() {
        if (enabledHall != null) deleteConferenceHall(enabledHall.getId());
        if (disabledHall != null) deleteConferenceHall(disabledHall.getId());
        if (building != null) deleteBuilding(building.getId());
        if (customer != null) deleteUser(customer.getId());
        if (admin != null) deleteUser(admin.getId());
        if (organizer != null) deleteUser(organizer.getId());
    }

    @Test
    void findAllConferenceHalls_noToken_returnsUnauthorized() {
        RestAssured.given()
                .when().get("/conference-halls")
                .then()
                .statusCode(401);
    }

    @Test
    void getConferenceHallById_disabledHallAsCustomer_returnsBadRequestAsIfNotFound() {
        String email = "chs-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        building = seedBuilding();
        disabledHall = seedConferenceHall(building, false);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().get("/conference-halls/" + disabledHall.getId())
                .then()
                .statusCode(400)
                .body("violations[0].message", org.hamcrest.Matchers.equalTo("Conference hall not found"));
    }

    @Test
    void getConferenceHallById_disabledHallAsAdmin_returnsDetail() {
        String email = "chs-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(email, Role.ADMIN, true);
        building = seedBuilding();
        disabledHall = seedConferenceHall(building, false);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().get("/conference-halls/" + disabledHall.getId())
                .then()
                .statusCode(200)
                .body("id", org.hamcrest.Matchers.equalTo(disabledHall.getId().toString()));
    }

    @Test
    void createConferenceHall_asCustomer_returnsForbidden() {
        String email = "chs-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);
        String body = "{\"name\":\"Nope\",\"size\":10,\"pricePerHour\":50.00,\"buildingId\":\"" + UUID.randomUUID() + "\",\"floor\":1,\"roomNumber\":\"101\"}";

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(body)
                .when().post("/conference-halls")
                .then()
                .statusCode(403);
    }

    @Test
    void createConferenceHall_asOrganizer_returnsCreated() {
        String email = "chs-" + UUID.randomUUID() + "@example.com";
        organizer = seedUser(email, Role.ORGANIZER, true);
        building = seedBuilding();
        String token = loginAndGetAccessToken(email);
        String body = "{\"name\":\"Sala Creata\",\"size\":10,\"pricePerHour\":50.00,\"buildingId\":\"" + building.getId() + "\",\"floor\":1,\"roomNumber\":\"101\"}";

        String createdId = RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(body)
                .when().post("/conference-halls")
                .then()
                .statusCode(200)
                .extract().path("payload");

        enabledHall = new ConferenceHall();
        enabledHall.setId(UUID.fromString(createdId));
    }

    @Test
    void deleteConferenceHall_asOrganizer_returnsForbidden() {
        String email = "chs-" + UUID.randomUUID() + "@example.com";
        organizer = seedUser(email, Role.ORGANIZER, true);
        building = seedBuilding();
        enabledHall = seedConferenceHall(building, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/conference-halls/" + enabledHall.getId())
                .then()
                .statusCode(403);
    }

    @Test
    void deleteConferenceHall_asAdmin_returnsSuccess() {
        String email = "chs-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(email, Role.ADMIN, true);
        building = seedBuilding();
        ConferenceHall toDelete = seedConferenceHall(building, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/conference-halls/" + toDelete.getId())
                .then()
                .statusCode(200);
    }

    @Test
    void findAllConferenceHalls_asCustomer_neverReturnsDisabledHalls() {
        String email = "chs-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        building = seedBuilding();
        enabledHall = seedConferenceHall(building, true);
        disabledHall = seedConferenceHall(building, false);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .queryParam("buildingId", building.getId())
                .when().get("/conference-halls")
                .then()
                .statusCode(200)
                .body("list.id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(disabledHall.getId().toString())));
    }

    @Test
    void findAllConferenceHalls_asAdmin_canSeeDisabledHallsWhenRequested() {
        String email = "chs-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(email, Role.ADMIN, true);
        building = seedBuilding();
        disabledHall = seedConferenceHall(building, false);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .queryParam("buildingId", building.getId())
                .queryParam("enabled", false)
                .when().get("/conference-halls")
                .then()
                .statusCode(200)
                .body("list.id", org.hamcrest.Matchers.hasItem(disabledHall.getId().toString()));
    }

    @Test
    void updateConferenceHall_asCustomer_returnsForbidden() {
        String email = "chs-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        building = seedBuilding();
        enabledHall = seedConferenceHall(building, true);
        String token = loginAndGetAccessToken(email);
        String body = "{\"name\":\"Nuovo Nome\",\"size\":10,\"pricePerHour\":50.00,\"floor\":1,\"roomNumber\":\"101\"}";

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(body)
                .when().patch("/conference-halls/" + enabledHall.getId())
                .then()
                .statusCode(403);
    }

    @Test
    void disableConferenceHall_asOrganizer_returnsSuccess() {
        String email = "chs-" + UUID.randomUUID() + "@example.com";
        organizer = seedUser(email, Role.ORGANIZER, true);
        building = seedBuilding();
        enabledHall = seedConferenceHall(building, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().patch("/conference-halls/" + enabledHall.getId() + "/disable")
                .then()
                .statusCode(200);
    }

    @Test
    void disableConferenceHall_asCustomer_returnsForbidden() {
        String email = "chs-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        building = seedBuilding();
        enabledHall = seedConferenceHall(building, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().patch("/conference-halls/" + enabledHall.getId() + "/disable")
                .then()
                .statusCode(403);
    }
}
