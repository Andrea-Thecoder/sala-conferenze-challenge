package com.sala.challenge.integration;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sala.challenge.model.Building;
import com.sala.challenge.model.User;
import com.sala.challenge.model.enumerator.Role;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * BuildingResource è @RolesAllowed({"ADMIN","ORGANIZER"}) a livello di classe,
 * ma create/update/delete restringono ulteriormente ad ADMIN only — solo un
 * test HTTP reale prova che entrambi i livelli di @RolesAllowed vengono
 * davvero applicati dal container, non solo scritti nell'annotazione.
 */
@QuarkusTest
class BuildingResourceSecurityIT extends AbstractIntegrationTest {

    private User customer;
    private User organizer;
    private User admin;
    private Building building;

    @AfterEach
    void cleanup() {
        if (building != null) deleteBuilding(building.getId());
        if (customer != null) deleteUser(customer.getId());
        if (organizer != null) deleteUser(organizer.getId());
        if (admin != null) deleteUser(admin.getId());
    }

    private String createBuildingBody() {
        return "{\"street\":\"Via Test 1\",\"city\":\"Milano\",\"postalCode\":\"20121\",\"country\":\"Italia\"}";
    }

    @Test
    void findAllBuildings_asCustomer_returnsForbidden() {
        String email = "bld-" + UUID.randomUUID() + "@example.com";
        customer = seedUser(email, Role.CUSTOMER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().get("/buildings")
                .then()
                .statusCode(403);
    }

    @Test
    void findAllBuildings_asOrganizer_returnsSuccess() {
        String email = "bld-" + UUID.randomUUID() + "@example.com";
        organizer = seedUser(email, Role.ORGANIZER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().get("/buildings")
                .then()
                .statusCode(200);
    }

    @Test
    void createBuilding_asOrganizer_returnsForbidden() {
        String email = "bld-" + UUID.randomUUID() + "@example.com";
        organizer = seedUser(email, Role.ORGANIZER, true);
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createBuildingBody())
                .when().post("/buildings")
                .then()
                .statusCode(403);
    }

    @Test
    void createBuilding_asAdmin_returnsSuccess() {
        String email = "bld-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(email, Role.ADMIN, true);
        String token = loginAndGetAccessToken(email);

        String createdId = RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createBuildingBody())
                .when().post("/buildings")
                .then()
                .statusCode(200)
                .extract().path("payload");

        building = new Building();
        building.setId(UUID.fromString(createdId));
    }

    @Test
    void updateBuilding_asOrganizer_returnsForbidden() {
        String email = "bld-" + UUID.randomUUID() + "@example.com";
        organizer = seedUser(email, Role.ORGANIZER, true);
        building = seedBuilding();
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createBuildingBody())
                .when().patch("/buildings/" + building.getId())
                .then()
                .statusCode(403);
    }

    @Test
    void updateBuilding_asAdmin_returnsSuccess() {
        String email = "bld-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(email, Role.ADMIN, true);
        building = seedBuilding();
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createBuildingBody())
                .when().patch("/buildings/" + building.getId())
                .then()
                .statusCode(200);
    }

    @Test
    void deleteBuilding_asOrganizer_returnsForbidden() {
        String email = "bld-" + UUID.randomUUID() + "@example.com";
        organizer = seedUser(email, Role.ORGANIZER, true);
        building = seedBuilding();
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/buildings/" + building.getId())
                .then()
                .statusCode(403);
    }

    @Test
    void deleteBuilding_asAdmin_returnsSuccess() {
        String email = "bld-" + UUID.randomUUID() + "@example.com";
        admin = seedUser(email, Role.ADMIN, true);
        Building toDelete = seedBuilding();
        String token = loginAndGetAccessToken(email);

        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/buildings/" + toDelete.getId())
                .then()
                .statusCode(200);
    }
}
